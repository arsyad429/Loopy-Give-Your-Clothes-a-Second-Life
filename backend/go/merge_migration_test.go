package main

import (
	"context"
	"testing"

	"github.com/jackc/pgx/v5"
)

func TestMergedMigrationPreservesBothVersionThreeHistories(t *testing.T) {
	for _, original := range []string{"vision", "rewards"} {
		t.Run(original, func(t *testing.T) {
			ctx := context.Background()
			store := integrationStore(t)
			auth := testAuth(t, store)
			session, err := auth.register(ctx, registerRequest{"Existing Donor", "merge@example.test", "existing merge passphrase"})
			if err != nil {
				t.Fatal(err)
			}
			var redemptionID string
			if original == "vision" {
				_, err = store.pool.Exec(ctx, `DROP TABLE point_transactions, redemptions, rewards;
					DELETE FROM schema_migrations WHERE version=4`, pgx.QueryExecModeSimpleProtocol)
			} else {
				if _, err = store.pool.Exec(ctx, `UPDATE users SET total_points=10 WHERE user_id=$1`, session.User.ID); err != nil {
					t.Fatal(err)
				}
				result, redeemErr := store.Redeem(ctx, session.User.ID, "eco-supporter-badge", "existing-reward-key")
				if redeemErr != nil {
					t.Fatal(redeemErr)
				}
				redemptionID = result.Redemption.ID
				_, err = store.pool.Exec(ctx, `DROP TABLE clothing_check_requests, clothing_check_photos;
					ALTER TABLE clothing_checks DROP COLUMN action_recommendation;
					DELETE FROM schema_migrations WHERE version=3;
					UPDATE schema_migrations SET version=3 WHERE version=4`, pgx.QueryExecModeSimpleProtocol)
			}
			if err != nil {
				t.Fatal(err)
			}
			for range 2 {
				if err = store.migrate(ctx); err != nil {
					t.Fatal(err)
				}
			}
			principal, err := store.Authenticate(ctx, tokenHash(session.AccessToken))
			if err != nil || principal.User.ID != session.User.ID {
				t.Fatalf("existing session = %+v, %v", principal, err)
			}
			var migrations int
			var hasPhotos, hasLedger bool
			err = store.pool.QueryRow(ctx, `SELECT (SELECT count(*) FROM schema_migrations),
				to_regclass('clothing_check_photos') IS NOT NULL, to_regclass('point_transactions') IS NOT NULL`).Scan(&migrations, &hasPhotos, &hasLedger)
			if err != nil || migrations != 4 || !hasPhotos || !hasLedger {
				t.Fatalf("merged schema = %d, %v, %v, %v", migrations, hasPhotos, hasLedger, err)
			}
			if original == "rewards" {
				history, err := store.Redemptions(ctx, session.User.ID)
				if err != nil || len(history) != 1 || history[0].ID != redemptionID {
					t.Fatalf("existing redemption = %+v, %v", history, err)
				}
				balance, err := store.PointsBalance(ctx, session.User.ID)
				if err != nil || balance.Balance != 0 {
					t.Fatalf("existing balance = %+v, %v", balance, err)
				}
			}
		})
	}
}
