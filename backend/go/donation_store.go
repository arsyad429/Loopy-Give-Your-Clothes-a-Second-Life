package main

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
)

const donationItemColumns = `d.donation_id::text, u.name, d.city, d.quantity, d.status, d.version, d.created_at, d.updated_at`

func donationItemFields(item *donationItem) []any {
	return []any{&item.ID, &item.DonorName, &item.City, &item.Quantity, &item.Status, &item.Version, &item.CreatedAt, &item.UpdatedAt}
}

func (d *donationItem) useUTC() {
	d.CreatedAt, d.UpdatedAt = d.CreatedAt.UTC(), d.UpdatedAt.UTC()
}

func (s *pgStore) DonationSummary(ctx context.Context) (donationSummary, error) {
	var summary donationSummary
	err := s.pool.QueryRow(ctx, `SELECT count(*),
		count(*) FILTER (WHERE status = $1),
		count(*) FILTER (WHERE status = $2),
		count(*) FILTER (WHERE status = $3),
		count(*) FILTER (WHERE status = $4),
		count(*) FILTER (WHERE status = $5)
		FROM donation_requests`, donationPending, donationScheduled, donationPickedUp, donationCompleted, donationRejected).
		Scan(&summary.Total, &summary.Pending, &summary.Scheduled, &summary.PickedUp, &summary.Completed, &summary.Rejected)
	return summary, err
}

func (s *pgStore) ListDonations(ctx context.Context, filter donationFilter) ([]donationItem, error) {
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
		ORDER BY d.created_at, d.donation_id
		LIMIT $5`, filter.Status, filter.Query, afterTime, afterID, filter.Limit+1)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	items := make([]donationItem, 0, filter.Limit+1)
	for rows.Next() {
		var item donationItem
		if err = rows.Scan(donationItemFields(&item)...); err != nil {
			return nil, err
		}
		item.useUTC()
		items = append(items, item)
	}
	return items, rows.Err()
}

func (s *pgStore) DonationDetail(ctx context.Context, id string) (donationDetail, error) {
	tx, err := s.pool.BeginTx(ctx, pgx.TxOptions{IsoLevel: pgx.RepeatableRead, AccessMode: pgx.ReadOnly})
	if err != nil {
		return donationDetail{}, err
	}
	defer tx.Rollback(context.Background())

	detail, err := readDonationDetail(ctx, tx, id)
	if err != nil {
		return donationDetail{}, err
	}
	return detail, tx.Commit(ctx)
}

func readDonationDetail(ctx context.Context, tx pgx.Tx, id string) (donationDetail, error) {
	var detail donationDetail
	check := &detail.ClothingCheck
	fields := append(donationItemFields(&detail.donationItem),
		&detail.PhoneNumber, &detail.PickupAddress, &detail.PreferredDate, &detail.PreferredTime, &detail.Notes,
		&detail.ScheduledAt, &detail.RejectionReason, &check.CheckID, &check.PhotoKey, &check.ClothingType,
		&check.AIResult, &check.AIDescription,
	)
	err := tx.QueryRow(ctx, `SELECT `+donationItemColumns+`,
		d.phone_number, d.pickup_address, to_char(d.preferred_date, 'YYYY-MM-DD'), d.preferred_time, d.notes,
		d.scheduled_at, d.rejection_reason, c.check_id::text, c.photo_key, c.clothing_type, c.ai_result, c.ai_description
		FROM donation_requests d
		JOIN users u ON u.user_id = d.user_id
		JOIN clothing_checks c ON c.check_id = d.check_id AND c.user_id = d.user_id
		WHERE d.donation_id = $1`, id).Scan(fields...)
	if err != nil {
		return donationDetail{}, storeError(err)
	}

	rows, err := tx.Query(ctx, `SELECT version, previous_status, new_status, updated_by::text, updated_at
		FROM donation_status_history WHERE donation_id = $1 ORDER BY version`, id)
	if err != nil {
		return donationDetail{}, err
	}
	defer rows.Close()

	detail.History = make([]donationHistory, 0)
	for rows.Next() {
		var entry donationHistory
		if err = rows.Scan(&entry.Version, &entry.PreviousStatus, &entry.NewStatus, &entry.UpdatedBy, &entry.UpdatedAt); err != nil {
			return donationDetail{}, err
		}
		entry.UpdatedAt = entry.UpdatedAt.UTC()
		detail.History = append(detail.History, entry)
	}
	detail.useUTC()
	detail.setSchedule()
	return detail, rows.Err()
}

func (s *pgStore) ReviewDonation(ctx context.Context, decision donationDecision) (donationDetail, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return donationDetail{}, err
	}
	defer tx.Rollback(context.Background())

	var status string
	var version int64
	err = tx.QueryRow(ctx, `SELECT status, version FROM donation_requests WHERE donation_id = $1 FOR UPDATE`, decision.ID).
		Scan(&status, &version)
	if err != nil {
		return donationDetail{}, storeError(err)
	}
	if version != decision.ExpectedVersion {
		return donationDetail{}, errDonationChanged
	}
	if status != donationPending {
		return donationDetail{}, errDonationFinal
	}

	var decidedAt time.Time
	err = tx.QueryRow(ctx, `UPDATE donation_requests SET status = $2, scheduled_at = $3, rejection_reason = $4,
		version = version + 1, updated_at = clock_timestamp()
		WHERE donation_id = $1 AND ($3::timestamptz IS NULL OR $3::timestamptz > clock_timestamp())
		RETURNING version, updated_at`, decision.ID, decision.Status, decision.ScheduledAt, decision.RejectionReason).
		Scan(&version, &decidedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return donationDetail{}, errPickupInPast
	}
	if err != nil {
		return donationDetail{}, err
	}

	_, err = tx.Exec(ctx, `INSERT INTO donation_status_history
		(donation_id, version, previous_status, new_status, updated_by, updated_at) VALUES ($1, $2, $3, $4, $5, $6)`,
		decision.ID, version, status, decision.Status, decision.AdminID, decidedAt)
	if err != nil {
		return donationDetail{}, err
	}
	detail, err := readDonationDetail(ctx, tx, decision.ID)
	if err != nil {
		return donationDetail{}, err
	}
	return detail, tx.Commit(ctx)
}
