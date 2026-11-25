package store

import (
	"bytes"
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
	"loopy/backend/internal/domain"
)

const checkColumns = `check_id::text, user_id::text, photo_key, clothing_type,
	ai_result, ai_description, action_recommendation, created_at`

// ClaimClothingCheck reserves an idempotency key before invoking Groq Vision.
func (s *PGStore) ClaimClothingCheck(ctx context.Context, userID, key string, hash []byte) (domain.CheckClaim, string, error) {
	id, err := domain.NewID()
	if err != nil {
		return domain.CheckClaim{}, "", err
	}
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return domain.CheckClaim{}, "", err
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
		return domain.CheckClaim{UserID: userID, Key: key, ID: reservedID}, "", tx.Commit(ctx)
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return domain.CheckClaim{}, "", err
	}

	var storedHash []byte
	var previousID *string
	if err := tx.QueryRow(ctx, `SELECT request_hash, check_id::text FROM clothing_check_requests
		WHERE user_id = $1 AND request_key = $2`, userID, key).Scan(&storedHash, &previousID); err != nil {
		return domain.CheckClaim{}, "", err
	}
	if !bytes.Equal(storedHash, hash) {
		return domain.CheckClaim{}, "", domain.ErrCheckKeyConflict
	}
	if previousID == nil {
		return domain.CheckClaim{}, "", domain.ErrCheckInProgress
	}
	return domain.CheckClaim{}, *previousID, nil
}

// CompleteClothingCheck persists the AI check, normalized JPEG photo, and idempotency reference atomically.
func (s *PGStore) CompleteClothingCheck(
	ctx context.Context,
	claim domain.CheckClaim,
	kind string,
	result domain.VisionResult,
	jpeg []byte,
) (domain.ClothingCheck, error) {
	id, err := domain.NewID()
	if err != nil {
		return domain.ClothingCheck{}, err
	}
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return domain.ClothingCheck{}, err
	}
	defer tx.Rollback(context.Background())

	check, err := scanClothingCheck(tx.QueryRow(ctx, `
		INSERT INTO clothing_checks (check_id, user_id, photo_key, clothing_type, ai_result, ai_description, action_recommendation)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
		RETURNING `+checkColumns, id, claim.UserID, "/v1/checks/"+id+"/photo",
		kind, result.Category, result.Description, result.Recommendation))
	if err != nil {
		return domain.ClothingCheck{}, err
	}
	if _, err = tx.Exec(ctx, "INSERT INTO clothing_check_photos (check_id, jpeg) VALUES ($1, $2)", id, jpeg); err != nil {
		return domain.ClothingCheck{}, err
	}

	tag, err := tx.Exec(ctx, `UPDATE clothing_check_requests
		SET check_id = $1, claim_id = NULL, claimed_until = NULL
		WHERE user_id = $2 AND request_key = $3 AND claim_id = $4
			AND check_id IS NULL AND claimed_until > clock_timestamp()`, id, claim.UserID, claim.Key, claim.ID)
	if err != nil {
		return domain.ClothingCheck{}, err
	}
	if tag.RowsAffected() != 1 {
		return domain.ClothingCheck{}, domain.ErrCheckClaimLost
	}
	if err = tx.Commit(ctx); err != nil {
		return domain.ClothingCheck{}, err
	}
	return check, nil
}

// ReleaseClothingCheck frees an active reservation after an upstream failure.
func (s *PGStore) ReleaseClothingCheck(ctx context.Context, claim domain.CheckClaim) error {
	_, err := s.pool.Exec(ctx, `UPDATE clothing_check_requests SET claim_id = NULL, claimed_until = NULL
		WHERE user_id = $1 AND request_key = $2 AND claim_id = $3 AND check_id IS NULL`, claim.UserID, claim.Key, claim.ID)
	return err
}

// FindClothingCheck returns a clothing check visible to the owner or an admin.
func (s *PGStore) FindClothingCheck(ctx context.Context, actor domain.User, id string) (domain.ClothingCheck, error) {
	return scanClothingCheck(s.pool.QueryRow(ctx, `SELECT `+checkColumns+` FROM clothing_checks
		WHERE check_id = $1 AND ($2 OR user_id = $3)`, id, actor.Role == "ADMIN", actor.ID))
}

// FindClothingPhoto returns the private JPEG bytes visible to the owner or an admin.
func (s *PGStore) FindClothingPhoto(ctx context.Context, actor domain.User, id string) ([]byte, error) {
	var jpeg []byte
	err := s.pool.QueryRow(ctx, `SELECT p.jpeg FROM clothing_check_photos p
		JOIN clothing_checks c USING (check_id)
		WHERE c.check_id = $1 AND ($2 OR c.user_id = $3)`, id, actor.Role == "ADMIN", actor.ID).Scan(&jpeg)
	return jpeg, storeError(err)
}

func scanClothingCheck(row pgx.Row) (domain.ClothingCheck, error) {
	var check domain.ClothingCheck
	err := row.Scan(&check.ID, &check.UserID, &check.ClothingPhoto, &check.ClothingType,
		&check.Category, &check.Description, &check.Recommendation, &check.CreatedAt)
	check.CreatedAt = check.CreatedAt.UTC()
	return check, storeError(err)
}
