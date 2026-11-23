package main

import (
	"bytes"
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
)

type clothingCheck struct {
	ID            string `json:"checkId"`
	UserID        string `json:"userId"`
	ClothingPhoto string `json:"clothingPhoto"`
	ClothingType  string `json:"clothingType"`
	visionResult
	CreatedAt time.Time `json:"createdAt"`
}

type checkClaim struct {
	userID string
	key    string
	id     string
}

var (
	errCheckInProgress  = errors.New("clothing check in progress")
	errCheckKeyConflict = errors.New("clothing check payload does not match idempotency key")
	errCheckClaimLost   = errors.New("clothing check reservation expired")
)

const checkColumns = `check_id::text, user_id::text, photo_key, clothing_type,
	ai_result, ai_description, action_recommendation, created_at`

// claimClothingCheck reserves a key before calling Groq. A completed key
// returns its existing check ID instead of creating another reservation.
func (s *pgStore) claimClothingCheck(ctx context.Context, userID, key string, hash []byte) (checkClaim, string, error) {
	id, err := newID()
	if err != nil {
		return checkClaim{}, "", err
	}
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return checkClaim{}, "", err
	}
	defer tx.Rollback(context.Background())

	var reservedID string
	err = tx.QueryRow(ctx, `
		INSERT INTO clothing_check_requests AS r (user_id, request_key, request_hash, claim_id, claimed_until)
		VALUES ($1, $2, $3, $4, clock_timestamp() + INTERVAL '2 minutes')
		ON CONFLICT (user_id, request_key) DO UPDATE
		SET claim_id = EXCLUDED.claim_id, claimed_until = clock_timestamp() + INTERVAL '2 minutes'
		WHERE r.request_hash = EXCLUDED.request_hash AND r.check_id IS NULL
			AND (r.claimed_until IS NULL OR r.claimed_until <= clock_timestamp())
		RETURNING claim_id::text`, userID, key, hash, id).Scan(&reservedID)
	if err == nil {
		return checkClaim{userID: userID, key: key, id: reservedID}, "", tx.Commit(ctx)
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return checkClaim{}, "", err
	}

	var storedHash []byte
	var previousID *string
	if err := tx.QueryRow(ctx, `SELECT request_hash, check_id::text FROM clothing_check_requests
		WHERE user_id = $1 AND request_key = $2`, userID, key).Scan(&storedHash, &previousID); err != nil {
		return checkClaim{}, "", err
	}
	if !bytes.Equal(storedHash, hash) {
		return checkClaim{}, "", errCheckKeyConflict
	}
	if previousID == nil {
		return checkClaim{}, "", errCheckInProgress
	}
	return checkClaim{}, *previousID, nil
}

func (s *pgStore) completeClothingCheck(ctx context.Context, claim checkClaim, kind string, result visionResult, jpeg []byte) (clothingCheck, error) {
	id, err := newID()
	if err != nil {
		return clothingCheck{}, err
	}
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return clothingCheck{}, err
	}
	defer tx.Rollback(context.Background())

	check, err := scanClothingCheck(tx.QueryRow(ctx, `
		INSERT INTO clothing_checks (check_id, user_id, photo_key, clothing_type, ai_result, ai_description, action_recommendation)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
		RETURNING `+checkColumns, id, claim.userID, "/v1/checks/"+id+"/photo",
		kind, result.Category, result.Description, result.Recommendation))
	if err != nil {
		return clothingCheck{}, err
	}
	if _, err = tx.Exec(ctx, "INSERT INTO clothing_check_photos (check_id, jpeg) VALUES ($1, $2)", id, jpeg); err != nil {
		return clothingCheck{}, err
	}

	tag, err := tx.Exec(ctx, `UPDATE clothing_check_requests
		SET check_id = $1, claim_id = NULL, claimed_until = NULL
		WHERE user_id = $2 AND request_key = $3 AND claim_id = $4
			AND check_id IS NULL AND claimed_until > clock_timestamp()`, id, claim.userID, claim.key, claim.id)
	if err != nil {
		return clothingCheck{}, err
	}
	if tag.RowsAffected() != 1 {
		return clothingCheck{}, errCheckClaimLost
	}
	if err = tx.Commit(ctx); err != nil {
		return clothingCheck{}, err
	}
	return check, nil
}

func (s *pgStore) releaseClothingCheck(ctx context.Context, claim checkClaim) error {
	_, err := s.pool.Exec(ctx, `UPDATE clothing_check_requests SET claim_id = NULL, claimed_until = NULL
		WHERE user_id = $1 AND request_key = $2 AND claim_id = $3 AND check_id IS NULL`, claim.userID, claim.key, claim.id)
	return err
}

func (s *pgStore) findClothingCheck(ctx context.Context, actor user, id string) (clothingCheck, error) {
	return scanClothingCheck(s.pool.QueryRow(ctx, `SELECT `+checkColumns+` FROM clothing_checks
		WHERE check_id = $1 AND ($2 OR user_id = $3)`, id, actor.Role == "ADMIN", actor.ID))
}

func (s *pgStore) findClothingPhoto(ctx context.Context, actor user, id string) ([]byte, error) {
	var jpeg []byte
	err := s.pool.QueryRow(ctx, `SELECT p.jpeg FROM clothing_check_photos p
		JOIN clothing_checks c USING (check_id)
		WHERE c.check_id = $1 AND ($2 OR c.user_id = $3)`, id, actor.Role == "ADMIN", actor.ID).Scan(&jpeg)
	return jpeg, storeError(err)
}

func scanClothingCheck(row pgx.Row) (clothingCheck, error) {
	var check clothingCheck
	err := row.Scan(&check.ID, &check.UserID, &check.ClothingPhoto, &check.ClothingType,
		&check.Category, &check.Description, &check.Recommendation, &check.CreatedAt)
	check.CreatedAt = check.CreatedAt.UTC()
	return check, storeError(err)
}
