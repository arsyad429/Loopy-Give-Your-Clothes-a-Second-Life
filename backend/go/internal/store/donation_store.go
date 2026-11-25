package store

import (
	"bytes"
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"loopy/backend/internal/domain"
)

const donationItemColumns = `d.donation_id::text, u.name, d.city, d.quantity, d.status, d.version, d.created_at, d.updated_at`

func donationItemFields(item *domain.DonationItem) []any {
	return []any{&item.ID, &item.DonorName, &item.City, &item.Quantity, &item.Status, &item.Version, &item.CreatedAt, &item.UpdatedAt}
}

// CreateDonation atomically validates check ownership/eligibility, checks idempotency, and persists a PENDING_REVIEW request.
func (s *PGStore) CreateDonation(ctx context.Context, sub domain.CreateDonationSubmission) (domain.DonationDetail, bool, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return domain.DonationDetail{}, false, err
	}
	defer tx.Rollback(context.Background())

	var ownerExists int
	if err = tx.QueryRow(ctx, `SELECT 1 FROM users WHERE user_id = $1 FOR UPDATE`, sub.UserID).Scan(&ownerExists); err != nil {
		return domain.DonationDetail{}, false, storeError(err)
	}

	var storedHash []byte
	var existingID string
	err = tx.QueryRow(ctx, `SELECT request_hash, donation_id::text FROM donation_submit_requests
		WHERE user_id = $1 AND request_key = $2`, sub.UserID, sub.Key).Scan(&storedHash, &existingID)
	if err == nil {
		if !bytes.Equal(storedHash, sub.RequestHash) {
			return domain.DonationDetail{}, false, domain.ErrDonationKeyConflict
		}
		detail, readErr := readDonationDetail(ctx, tx, existingID)
		if readErr != nil {
			return domain.DonationDetail{}, false, readErr
		}
		return detail, false, tx.Commit(ctx)
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return domain.DonationDetail{}, false, err
	}

	var aiResult string
	err = tx.QueryRow(ctx, `SELECT ai_result FROM clothing_checks WHERE check_id = $1 AND user_id = $2 FOR SHARE`,
		sub.CheckID, sub.UserID).Scan(&aiResult)
	if err != nil {
		return domain.DonationDetail{}, false, storeError(err)
	}
	if aiResult != "POTENTIALLY_ELIGIBLE" {
		return domain.DonationDetail{}, false, domain.ErrCheckNotEligible
	}

	donationID, err := domain.NewID()
	if err != nil {
		return domain.DonationDetail{}, false, err
	}
	_, err = tx.Exec(ctx, `INSERT INTO donation_requests
		(donation_id, user_id, check_id, phone_number, pickup_address, city, quantity, preferred_date, preferred_time, notes)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8::date, $9, $10)`,
		donationID, sub.UserID, sub.CheckID, sub.PhoneNumber, sub.PickupAddress, sub.City,
		sub.Quantity, sub.PreferredDate, sub.PreferredTime, sub.Notes)
	if err != nil {
		return domain.DonationDetail{}, false, err
	}

	_, err = tx.Exec(ctx, `INSERT INTO donation_status_history
		(donation_id, version, previous_status, new_status, updated_by, updated_at)
		SELECT donation_id, version, NULL, status, user_id, created_at
		FROM donation_requests WHERE donation_id = $1`, donationID)
	if err != nil {
		return domain.DonationDetail{}, false, err
	}

	_, err = tx.Exec(ctx, `INSERT INTO donation_submit_requests
		(user_id, request_key, request_hash, donation_id) VALUES ($1, $2, $3, $4)`,
		sub.UserID, sub.Key, sub.RequestHash, donationID)
	if err != nil {
		return domain.DonationDetail{}, false, err
	}

	detail, err := readDonationDetail(ctx, tx, donationID)
	if err != nil {
		return domain.DonationDetail{}, false, err
	}
	return detail, true, tx.Commit(ctx)
}

// DonationSummary returns counts grouped by donation lifecycle status.
func (s *PGStore) DonationSummary(ctx context.Context) (domain.DonationSummary, error) {
	var summary domain.DonationSummary
	err := s.pool.QueryRow(ctx, `SELECT count(*),
		count(*) FILTER (WHERE status = $1),
		count(*) FILTER (WHERE status = $2),
		count(*) FILTER (WHERE status = $3),
		count(*) FILTER (WHERE status = $4),
		count(*) FILTER (WHERE status = $5)
		FROM donation_requests`,
		domain.DonationPending, domain.DonationScheduled, domain.DonationPickedUp, domain.DonationCompleted, domain.DonationRejected).
		Scan(&summary.Total, &summary.Pending, &summary.Scheduled, &summary.PickedUp, &summary.Completed, &summary.Rejected)
	return summary, err
}

// ListDonations returns filtered, cursor-paginated donation items.
func (s *PGStore) ListDonations(ctx context.Context, filter domain.DonationFilter) ([]domain.DonationItem, error) {
	var afterTime, afterID any
	if filter.After.ID != "" {
		afterTime, afterID = filter.After.CreatedAt, filter.After.ID
	}
	rows, err := s.pool.Query(ctx, `SELECT `+donationItemColumns+`
		FROM donation_requests d JOIN users u ON u.user_id = d.user_id
		WHERE ($1 = '' OR d.status = $1)
		AND ($2 = '' OR strpos(lower(d.donation_id::text), lower($2)) > 0
			OR strpos(lower(u.name), lower($2)) > 0 OR strpos(lower(d.city), lower($2)) > 0)
		AND ($3::timestamptz IS NULL OR (d.created_at, d.donation_id) > ($3::timestamptz, $4::uuid))
		AND ($6 = '' OR d.user_id = NULLIF($6, '')::uuid)
		ORDER BY d.created_at, d.donation_id
		LIMIT $5`, filter.Status, filter.Query, afterTime, afterID, filter.Limit+1, filter.OwnerID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	items := make([]domain.DonationItem, 0, filter.Limit+1)
	for rows.Next() {
		var item domain.DonationItem
		if err = rows.Scan(donationItemFields(&item)...); err != nil {
			return nil, err
		}
		item.UseUTC()
		items = append(items, item)
	}
	return items, rows.Err()
}

// LatestDonation returns the authenticated owner's most recently created donation.
func (s *PGStore) LatestDonation(ctx context.Context, ownerID string) (*domain.DonationItem, error) {
	var item domain.DonationItem
	err := s.pool.QueryRow(ctx, `SELECT `+donationItemColumns+`
		FROM donation_requests d JOIN users u ON u.user_id = d.user_id
		WHERE d.user_id = $1
		ORDER BY d.created_at DESC, d.donation_id DESC LIMIT 1`, ownerID).
		Scan(donationItemFields(&item)...)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	item.UseUTC()
	return &item, nil
}

// DonationDetail returns the complete donation detail and history inside a repeatable-read snapshot.
func (s *PGStore) DonationDetail(ctx context.Context, id string) (domain.DonationDetail, error) {
	tx, err := s.pool.BeginTx(ctx, pgx.TxOptions{IsoLevel: pgx.RepeatableRead, AccessMode: pgx.ReadOnly})
	if err != nil {
		return domain.DonationDetail{}, err
	}
	defer tx.Rollback(context.Background())

	detail, err := readDonationDetail(ctx, tx, id)
	if err != nil {
		return domain.DonationDetail{}, err
	}
	return detail, tx.Commit(ctx)
}

func readDonationDetail(ctx context.Context, tx pgx.Tx, id string) (domain.DonationDetail, error) {
	var detail domain.DonationDetail
	check := &detail.ClothingCheck
	fields := append(donationItemFields(&detail.DonationItem),
		&detail.UserID, &detail.PhoneNumber, &detail.PickupAddress, &detail.PreferredDate, &detail.PreferredTime, &detail.Notes,
		&detail.ScheduledAt, &detail.RejectionReason, &check.CheckID, &check.PhotoKey, &check.ClothingType,
		&check.AIResult, &check.AIDescription,
	)
	err := tx.QueryRow(ctx, `SELECT `+donationItemColumns+`,
		d.user_id::text, d.phone_number, d.pickup_address, to_char(d.preferred_date, 'YYYY-MM-DD'), d.preferred_time, d.notes,
		d.scheduled_at, d.rejection_reason, c.check_id::text, c.photo_key, c.clothing_type, c.ai_result, c.ai_description
		FROM donation_requests d
		JOIN users u ON u.user_id = d.user_id
		JOIN clothing_checks c ON c.check_id = d.check_id AND c.user_id = d.user_id
		WHERE d.donation_id = $1`, id).Scan(fields...)
	if err != nil {
		return domain.DonationDetail{}, storeError(err)
	}

	rows, err := tx.Query(ctx, `SELECT version, previous_status, new_status, updated_by::text, updated_at
		FROM donation_status_history WHERE donation_id = $1 ORDER BY version`, id)
	if err != nil {
		return domain.DonationDetail{}, err
	}
	defer rows.Close()

	detail.History = make([]domain.DonationHistory, 0)
	for rows.Next() {
		var entry domain.DonationHistory
		if err = rows.Scan(&entry.Version, &entry.PreviousStatus, &entry.NewStatus, &entry.UpdatedBy, &entry.UpdatedAt); err != nil {
			return domain.DonationDetail{}, err
		}
		entry.UpdatedAt = entry.UpdatedAt.UTC()
		detail.History = append(detail.History, entry)
	}
	detail.UseUTC()
	detail.SetSchedule()
	return detail, rows.Err()
}

// ReviewDonation applies an optimistic-locked admin schedule or rejection decision.
func (s *PGStore) ReviewDonation(ctx context.Context, decision domain.DonationDecision) (domain.DonationDetail, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return domain.DonationDetail{}, err
	}
	defer tx.Rollback(context.Background())

	var status string
	var version int64
	err = tx.QueryRow(ctx, `SELECT status, version FROM donation_requests WHERE donation_id = $1 FOR UPDATE`, decision.ID).
		Scan(&status, &version)
	if err != nil {
		return domain.DonationDetail{}, storeError(err)
	}
	if version != decision.ExpectedVersion {
		return domain.DonationDetail{}, domain.ErrDonationChanged
	}
	if status != domain.DonationPending {
		return domain.DonationDetail{}, domain.ErrDonationFinal
	}

	var decidedAt time.Time
	err = tx.QueryRow(ctx, `UPDATE donation_requests SET status = $2, scheduled_at = $3, rejection_reason = $4,
		version = version + 1, updated_at = clock_timestamp()
		WHERE donation_id = $1 AND ($3::timestamptz IS NULL OR $3::timestamptz > clock_timestamp())
		RETURNING version, updated_at`, decision.ID, decision.Status, decision.ScheduledAt, decision.RejectionReason).
		Scan(&version, &decidedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return domain.DonationDetail{}, domain.ErrPickupInPast
	}
	if err != nil {
		return domain.DonationDetail{}, err
	}

	_, err = tx.Exec(ctx, `INSERT INTO donation_status_history
		(donation_id, version, previous_status, new_status, updated_by, updated_at) VALUES ($1, $2, $3, $4, $5, $6)`,
		decision.ID, version, status, decision.Status, decision.AdminID, decidedAt)
	if err != nil {
		return domain.DonationDetail{}, err
	}
	detail, err := readDonationDetail(ctx, tx, decision.ID)
	if err != nil {
		return domain.DonationDetail{}, err
	}
	return detail, tx.Commit(ctx)
}
