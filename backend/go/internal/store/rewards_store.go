package store

import (
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
	"loopy/backend/internal/domain"
)

// AdvanceDonation transitions a scheduled donation through PICKED_UP to COMPLETED and credits +10 points once.
func (s *PGStore) AdvanceDonation(ctx context.Context, id, admin string, version int64, target string) (domain.DonationDetail, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return domain.DonationDetail{}, err
	}
	defer tx.Rollback(context.Background())

	var owner, status string
	var current int64
	err = tx.QueryRow(ctx, `SELECT user_id::text, status, version FROM donation_requests WHERE donation_id=$1 FOR UPDATE`, id).
		Scan(&owner, &status, &current)
	if err != nil {
		return domain.DonationDetail{}, storeError(err)
	}
	if current != version {
		return domain.DonationDetail{}, domain.ErrDonationChanged
	}
	if !(status == domain.DonationScheduled && target == domain.DonationPickedUp ||
		status == domain.DonationPickedUp && target == domain.DonationCompleted) {
		return domain.DonationDetail{}, domain.RewardError(409, "INVALID_TRANSITION", "Use PICKUP_SCHEDULED → PICKED_UP → COMPLETED.")
	}
	_, err = tx.Exec(ctx, `UPDATE donation_requests SET status=$2, version=version+1, updated_at=clock_timestamp() WHERE donation_id=$1`, id, target)
	if err != nil {
		return domain.DonationDetail{}, err
	}
	_, err = tx.Exec(ctx, `INSERT INTO donation_status_history(donation_id, version, previous_status, new_status, updated_by, updated_at)
		SELECT donation_id, version, $2, status, $3, updated_at FROM donation_requests WHERE donation_id=$1`, id, status, admin)
	if err != nil {
		return domain.DonationDetail{}, err
	}
	if target == domain.DonationCompleted {
		transactionID, idErr := domain.NewID()
		if idErr != nil {
			return domain.DonationDetail{}, idErr
		}
		// The donation row lock and UNIQUE(donation_id) guarantee exactly one credit.
		_, err = tx.Exec(ctx, `INSERT INTO point_transactions(transaction_id,user_id,donation_id,points) VALUES($1,$2,$3,10)`, transactionID, owner, id)
		if err != nil {
			return domain.DonationDetail{}, err
		}
		_, err = tx.Exec(ctx, `UPDATE users SET total_points=total_points+10 WHERE user_id=$1`, owner)
		if err != nil {
			return domain.DonationDetail{}, err
		}
	}
	detail, err := readDonationDetail(ctx, tx, id)
	if err != nil {
		return domain.DonationDetail{}, err
	}
	return detail, tx.Commit(ctx)
}

// PointsBalance returns the user's current points balance and latest transaction timestamp.
func (s *PGStore) PointsBalance(ctx context.Context, owner string) (domain.PointsBalance, error) {
	var result domain.PointsBalance
	err := s.pool.QueryRow(ctx, `SELECT total_points, COALESCE((SELECT max(created_at) FROM point_transactions WHERE user_id=$1), created_at)
		FROM users WHERE user_id=$1`, owner).Scan(&result.Balance, &result.UpdatedAt)
	result.UpdatedAt = result.UpdatedAt.UTC()
	return result, storeError(err)
}

// Rewards lists all active catalog rewards ordered by point cost.
func (s *PGStore) Rewards(ctx context.Context) ([]domain.Reward, error) {
	rows, err := s.pool.Query(ctx, `SELECT reward_id,name,description,icon,required_points,type,is_active FROM rewards WHERE is_active ORDER BY required_points,reward_id`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := make([]domain.Reward, 0)
	for rows.Next() {
		var item domain.Reward
		if err = rows.Scan(&item.ID, &item.Name, &item.Description, &item.Icon, &item.RequiredPoints, &item.Type, &item.IsActive); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

func redemptionFields(item *domain.Redemption) []any {
	return []any{&item.ID, &item.RewardID, &item.RewardName, &item.Description, &item.Icon, &item.Type, &item.PointsUsed, &item.RedeemedAt}
}

const redemptionColumns = `redemption_id::text,reward_id,reward_name,reward_description,reward_icon,reward_type,points_used,redeemed_at`

// Redeem atomically deducts points and records an idempotent reward snapshot.
func (s *PGStore) Redeem(ctx context.Context, owner, rewardID, key string) (domain.RedemptionResult, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return domain.RedemptionResult{}, err
	}
	defer tx.Rollback(context.Background())

	var balance int
	// Serializes all redemptions for this account, including different request keys.
	err = tx.QueryRow(ctx, `SELECT total_points FROM users WHERE user_id=$1 FOR UPDATE`, owner).Scan(&balance)
	if err != nil {
		return domain.RedemptionResult{}, storeError(err)
	}
	var result domain.RedemptionResult
	fields := append(redemptionFields(&result.Redemption), &result.RemainingBalance)
	err = tx.QueryRow(ctx, `SELECT `+redemptionColumns+`,remaining_balance FROM redemptions WHERE user_id=$1 AND idempotency_key=$2`, owner, key).Scan(fields...)
	if err == nil {
		if result.Redemption.RewardID != rewardID {
			return domain.RedemptionResult{}, domain.RewardError(409, "IDEMPOTENCY_CONFLICT", "This request key was used for a different reward.")
		}
		result.Replayed = true
		result.Redemption.RedeemedAt = result.Redemption.RedeemedAt.UTC()
		return result, tx.Commit(ctx)
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return domain.RedemptionResult{}, err
	}

	var item domain.Reward
	err = tx.QueryRow(ctx, `SELECT reward_id,name,description,icon,required_points,type,is_active FROM rewards WHERE reward_id=$1 AND is_active FOR SHARE`, rewardID).
		Scan(&item.ID, &item.Name, &item.Description, &item.Icon, &item.RequiredPoints, &item.Type, &item.IsActive)
	if errors.Is(err, pgx.ErrNoRows) {
		return domain.RedemptionResult{}, domain.RewardError(404, "REWARD_NOT_FOUND", "Reward is unavailable.")
	}
	if err != nil {
		return domain.RedemptionResult{}, err
	}
	if balance < item.RequiredPoints {
		return domain.RedemptionResult{}, domain.RewardError(409, "INSUFFICIENT_POINTS", "Poin belum cukup untuk reward ini.")
	}

	id, err := domain.NewID()
	if err != nil {
		return domain.RedemptionResult{}, err
	}
	result.RemainingBalance = balance - item.RequiredPoints
	err = tx.QueryRow(ctx, `INSERT INTO redemptions(redemption_id,user_id,reward_id,reward_name,reward_description,reward_icon,reward_type,points_used,remaining_balance,idempotency_key)
		VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10) RETURNING `+redemptionColumns,
		id, owner, item.ID, item.Name, item.Description, item.Icon, item.Type, item.RequiredPoints, result.RemainingBalance, key).Scan(redemptionFields(&result.Redemption)...)
	if err != nil {
		return domain.RedemptionResult{}, err
	}
	transactionID, err := domain.NewID()
	if err != nil {
		return domain.RedemptionResult{}, err
	}
	_, err = tx.Exec(ctx, `INSERT INTO point_transactions(transaction_id,user_id,redemption_id,points) VALUES($1,$2,$3,$4)`, transactionID, owner, id, -item.RequiredPoints)
	if err != nil {
		return domain.RedemptionResult{}, err
	}
	_, err = tx.Exec(ctx, `UPDATE users SET total_points=$2 WHERE user_id=$1`, owner, result.RemainingBalance)
	if err != nil {
		return domain.RedemptionResult{}, err
	}
	result.Redemption.RedeemedAt = result.Redemption.RedeemedAt.UTC()
	return result, tx.Commit(ctx)
}

// Redemptions returns the user's redemption history newest first.
func (s *PGStore) Redemptions(ctx context.Context, owner string) ([]domain.Redemption, error) {
	rows, err := s.pool.Query(ctx, `SELECT `+redemptionColumns+` FROM redemptions WHERE user_id=$1 ORDER BY redeemed_at DESC,redemption_id`, owner)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := make([]domain.Redemption, 0)
	for rows.Next() {
		var item domain.Redemption
		if err = rows.Scan(redemptionFields(&item)...); err != nil {
			return nil, err
		}
		item.RedeemedAt = item.RedeemedAt.UTC()
		items = append(items, item)
	}
	return items, rows.Err()
}
