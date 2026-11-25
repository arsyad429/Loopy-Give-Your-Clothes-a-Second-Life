package store

import (
	"context"
	"errors"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
	"loopy/backend/internal/domain"
	"loopy/backend/migrations"
)

// PGStore implements Loopy persistence repositories backed by PostgreSQL.
type PGStore struct {
	pool *pgxpool.Pool
}

// NewPGStore wraps a pgxpool.Pool as a PGStore.
func NewPGStore(pool *pgxpool.Pool) *PGStore {
	return &PGStore{pool: pool}
}

// Pool exposes the underlying connection pool for integration verification.
func (s *PGStore) Pool() *pgxpool.Pool {
	return s.pool
}

// Ping verifies PostgreSQL connectivity.
func (s *PGStore) Ping(ctx context.Context) error {
	return s.pool.Ping(ctx)
}

// Migrate applies pending SQL migrations inside an advisory-locked transaction.
func (s *PGStore) Migrate(ctx context.Context) error {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(context.Background())

	// Serializes startup across instances. The transaction also makes DDL atomic.
	if _, err = tx.Exec(ctx, "SELECT pg_advisory_xact_lock(710021001)"); err != nil {
		return err
	}
	if _, err = tx.Exec(ctx, "CREATE TABLE IF NOT EXISTS schema_migrations (version INTEGER PRIMARY KEY, applied_at TIMESTAMPTZ NOT NULL DEFAULT now())"); err != nil {
		return err
	}
	// Preserve main's Vision v3 and move an already-applied feature-branch rewards
	// migration to version 4 without altering existing balances or redemptions.
	if _, err = tx.Exec(ctx, `UPDATE schema_migrations SET version=4 WHERE version=3
		AND to_regclass('rewards') IS NOT NULL
		AND to_regclass('redemptions') IS NOT NULL
		AND to_regclass('point_transactions') IS NOT NULL
		AND to_regclass('clothing_check_photos') IS NULL
		AND NOT EXISTS (SELECT 1 FROM schema_migrations WHERE version=4)`); err != nil {
		return err
	}
	for index, migration := range migrations.Versions {
		version := index + 1
		var exists bool
		if err = tx.QueryRow(ctx, "SELECT EXISTS (SELECT 1 FROM schema_migrations WHERE version=$1)", version).Scan(&exists); err != nil {
			return err
		}
		if exists {
			continue
		}
		if _, err = tx.Exec(ctx, migration); err != nil {
			return err
		}
		if _, err = tx.Exec(ctx, "INSERT INTO schema_migrations(version) VALUES ($1)", version); err != nil {
			return err
		}
	}
	return tx.Commit(ctx)
}

func storeError(err error) error {
	if errors.Is(err, pgx.ErrNoRows) {
		return domain.ErrNotFound
	}
	var pgErr *pgconn.PgError
	if errors.As(err, &pgErr) && pgErr.Code == "23505" && pgErr.ConstraintName == "users_email_key" {
		return domain.ErrDuplicateEmail
	}
	return err
}
