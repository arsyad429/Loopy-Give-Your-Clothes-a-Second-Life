package store

import (
	"context"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"loopy/backend/internal/domain"
	"loopy/backend/internal/security"
)

func openTestStore(t *testing.T) *PGStore {
	t.Helper()
	connection := os.Getenv("TEST_DATABASE_URL")
	if connection == "" {
		t.Skip("set TEST_DATABASE_URL to run real PostgreSQL tests")
	}
	ctx := context.Background()
	admin, err := pgxpool.New(ctx, connection)
	if err != nil {
		t.Fatal(err)
	}
	id, err := domain.NewID()
	if err != nil {
		admin.Close()
		t.Fatal(err)
	}
	schema := "store_test_" + strings.ReplaceAll(id, "-", "")
	identifier := pgx.Identifier{schema}.Sanitize()
	if _, err = admin.Exec(ctx, "CREATE SCHEMA "+identifier); err != nil {
		admin.Close()
		t.Fatal(err)
	}
	t.Cleanup(func() {
		_, err := admin.Exec(context.Background(), "DROP SCHEMA "+identifier+" CASCADE")
		admin.Close()
		if err != nil {
			t.Error(err)
		}
	})
	cfg, err := pgxpool.ParseConfig(connection)
	if err != nil {
		t.Fatal(err)
	}
	cfg.ConnConfig.RuntimeParams["search_path"] = schema
	pool, err := pgxpool.NewWithConfig(ctx, cfg)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(pool.Close)
	pg := NewPGStore(pool)
	if err = pg.Migrate(ctx); err != nil {
		t.Fatal(err)
	}
	if err = pg.Migrate(ctx); err != nil {
		t.Fatalf("second startup migration: %v", err)
	}
	return pg
}

func insertTestUser(t *testing.T, pg *PGStore, email string) domain.User {
	t.Helper()
	id, err := domain.NewID()
	if err != nil {
		t.Fatal(err)
	}
	hash, err := security.HashPassword("a test password for store")
	if err != nil {
		t.Fatal(err)
	}
	sess, _, err := domain.NewSession(id)
	if err != nil {
		t.Fatal(err)
	}
	user, err := pg.Register(context.Background(), domain.Account{
		User:         domain.User{ID: id, Name: "Test Donor", Email: email},
		PasswordHash: hash,
	}, sess)
	if err != nil {
		t.Fatal(err)
	}
	return user
}

func TestMergedMigrationPreservesBothVersionThreeHistories(t *testing.T) {
	for _, original := range []string{"vision", "rewards"} {
		t.Run(original, func(t *testing.T) {
			ctx := context.Background()
			pg := openTestStore(t)
			user := insertTestUser(t, pg, "merge@example.test")
			sess, _, err := domain.NewSession(user.ID)
			if err != nil {
				t.Fatal(err)
			}
			if err = pg.CreateSession(ctx, sess); err != nil {
				t.Fatal(err)
			}

			var redemptionID string
			if original == "vision" {
				_, err = pg.Pool().Exec(ctx, `DROP TABLE donation_submit_requests, point_transactions, redemptions, rewards;
					DELETE FROM schema_migrations WHERE version>=4`, pgx.QueryExecModeSimpleProtocol)
			} else {
				if _, err = pg.Pool().Exec(ctx, `UPDATE users SET total_points=10 WHERE user_id=$1`, user.ID); err != nil {
					t.Fatal(err)
				}
				result, redeemErr := pg.Redeem(ctx, user.ID, "eco-supporter-badge", "existing-reward-key")
				if redeemErr != nil {
					t.Fatal(redeemErr)
				}
				redemptionID = result.Redemption.ID
				_, err = pg.Pool().Exec(ctx, `DROP TABLE donation_submit_requests, clothing_check_requests, clothing_check_photos;
					ALTER TABLE clothing_checks DROP COLUMN action_recommendation;
					DELETE FROM schema_migrations WHERE version IN (3, 5);
					UPDATE schema_migrations SET version=3 WHERE version=4`, pgx.QueryExecModeSimpleProtocol)
			}
			if err != nil {
				t.Fatal(err)
			}
			for range 2 {
				if err = pg.Migrate(ctx); err != nil {
					t.Fatal(err)
				}
			}
			principal, err := pg.Authenticate(ctx, sess.AccessHash)
			if err != nil || principal.User.ID != user.ID {
				t.Fatalf("existing session = %+v, %v", principal, err)
			}
			var migrations int
			var hasPhotos, hasLedger, hasSubmit bool
			err = pg.Pool().QueryRow(ctx, `SELECT (SELECT count(*) FROM schema_migrations),
				to_regclass('clothing_check_photos') IS NOT NULL,
				to_regclass('point_transactions') IS NOT NULL,
				to_regclass('donation_submit_requests') IS NOT NULL`).Scan(&migrations, &hasPhotos, &hasLedger, &hasSubmit)
			if err != nil || migrations != 5 || !hasPhotos || !hasLedger || !hasSubmit {
				t.Fatalf("merged schema = %d, %v, %v, %v, %v", migrations, hasPhotos, hasLedger, hasSubmit, err)
			}
			if original == "rewards" {
				history, err := pg.Redemptions(ctx, user.ID)
				if err != nil || len(history) != 1 || history[0].ID != redemptionID {
					t.Fatalf("existing redemption = %+v, %v", history, err)
				}
				balance, err := pg.PointsBalance(ctx, user.ID)
				if err != nil || balance.Balance != 0 {
					t.Fatalf("existing balance = %+v, %v", balance, err)
				}
			}
		})
	}
}

func TestPickupMigrationPreservesAuth(t *testing.T) {
	ctx := context.Background()
	pg := openTestStore(t)
	user := insertTestUser(t, pg, "existing@example.test")
	sess, _, err := domain.NewSession(user.ID)
	if err != nil {
		t.Fatal(err)
	}
	if err = pg.CreateSession(ctx, sess); err != nil {
		t.Fatal(err)
	}
	_, err = pg.Pool().Exec(ctx, `DROP TABLE donation_submit_requests, point_transactions, redemptions, rewards, clothing_check_requests, clothing_check_photos,
		donation_status_history, donation_requests, clothing_checks;
		DELETE FROM schema_migrations WHERE version>=2`, pgx.QueryExecModeSimpleProtocol)
	if err != nil {
		t.Fatal(err)
	}
	for range 2 {
		if err = pg.Migrate(ctx); err != nil {
			t.Fatal(err)
		}
	}
	p, err := pg.Authenticate(ctx, sess.AccessHash)
	if err != nil || p.User.ID != user.ID {
		t.Fatalf("existing session = %+v, %v", p, err)
	}
	if summary, err := pg.DonationSummary(ctx); err != nil || summary.Total != 0 {
		t.Fatalf("new schema summary = %+v, %v", summary, err)
	}
}

func TestVisionMigrationPreservesPickupRecords(t *testing.T) {
	ctx := context.Background()
	pg := openTestStore(t)
	user := insertTestUser(t, pg, "vision-preserve@example.test")
	sess, _, err := domain.NewSession(user.ID)
	if err != nil {
		t.Fatal(err)
	}
	if err = pg.CreateSession(ctx, sess); err != nil {
		t.Fatal(err)
	}

	if _, err := pg.Pool().Exec(ctx, `DROP TABLE clothing_check_requests, clothing_check_photos;
		ALTER TABLE clothing_checks DROP COLUMN action_recommendation;
		DELETE FROM schema_migrations WHERE version=3`, pgx.QueryExecModeSimpleProtocol); err != nil {
		t.Fatal(err)
	}
	checkID, _ := domain.NewID()
	donationID, _ := domain.NewID()
	now := time.Now().UTC()
	if _, err := pg.Pool().Exec(ctx, `INSERT INTO clothing_checks(check_id,user_id,photo_key,clothing_type,ai_result,ai_description)
		VALUES($1,$2,'private/test-shirt.jpg','Kaus','POTENTIALLY_ELIGIBLE','Tidak tampak sobekan besar.')`, checkID, user.ID); err != nil {
		t.Fatal(err)
	}
	if _, err := pg.Pool().Exec(ctx, `INSERT INTO donation_requests(donation_id,user_id,check_id,phone_number,pickup_address,city,quantity,
		preferred_date,preferred_time,notes,created_at,updated_at)
		VALUES($1,$2,$3,'081200000000','Jl. Contoh No. 12','Malang',3,'2026-12-01','Pagi','Sudah dicuci',$4,$4)`, donationID, user.ID, checkID, now); err != nil {
		t.Fatal(err)
	}
	if _, err := pg.Pool().Exec(ctx, `INSERT INTO donation_status_history(donation_id,version,new_status,updated_by,updated_at)
		VALUES($1,1,'PENDING_REVIEW',$2,$3)`, donationID, user.ID, now); err != nil {
		t.Fatal(err)
	}
	if err := pg.Migrate(ctx); err != nil {
		t.Fatal(err)
	}
	p, err := pg.Authenticate(ctx, sess.AccessHash)
	if err != nil || p.User.ID != user.ID {
		t.Fatalf("existing session not preserved: %v", err)
	}
	detail, err := pg.DonationDetail(ctx, donationID)
	if err != nil || detail.Status != domain.DonationPending || detail.ClothingCheck.PhotoKey != "private/test-shirt.jpg" {
		t.Fatalf("existing pickup not preserved: %+v, %v", detail, err)
	}
}
