package main

import (
	"context"
	_ "embed"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

//go:embed schema.sql
var schemaSQL string

type pgStore struct{ pool *pgxpool.Pool }

func (s *pgStore) migrate(ctx context.Context) error {
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
	var exists bool
	if err = tx.QueryRow(ctx, "SELECT EXISTS (SELECT 1 FROM schema_migrations WHERE version=1)").Scan(&exists); err != nil {
		return err
	}
	if !exists {
		if _, err = tx.Exec(ctx, schemaSQL); err != nil {
			return err
		}
		if _, err = tx.Exec(ctx, "INSERT INTO schema_migrations(version) VALUES (1)"); err != nil {
			return err
		}
	}
	return tx.Commit(ctx)
}

func scanUser(row pgx.Row) (user, error) {
	var u user
	err := row.Scan(&u.ID, &u.Name, &u.Email, &u.Role, &u.TotalPoints)
	return u, storeError(err)
}

func storeError(err error) error {
	if errors.Is(err, pgx.ErrNoRows) {
		return errNotFound
	}
	var pgErr *pgconn.PgError
	if errors.As(err, &pgErr) && pgErr.Code == "23505" && pgErr.ConstraintName == "users_email_key" {
		return errDuplicateEmail
	}
	return err
}

func (s *pgStore) Register(ctx context.Context, a account, session session) (user, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return user{}, err
	}
	defer tx.Rollback(context.Background())
	u, err := scanUser(tx.QueryRow(ctx, `INSERT INTO users(user_id,name,email,password_hash) VALUES($1,$2,$3,$4)
		RETURNING user_id::text,name,email,role,total_points`, a.ID, a.Name, a.Email, a.PasswordHash))
	if err != nil {
		return user{}, err
	}
	if err = insertSession(ctx, tx, session); err != nil {
		return user{}, err
	}
	if err = tx.Commit(ctx); err != nil {
		return user{}, err
	}
	return u, nil
}

func (s *pgStore) CreateAdmin(ctx context.Context, a account) (user, error) {
	return scanUser(s.pool.QueryRow(ctx, `INSERT INTO users(user_id,name,email,password_hash,role) VALUES($1,$2,$3,$4,'ADMIN')
		RETURNING user_id::text,name,email,role,total_points`, a.ID, a.Name, a.Email, a.PasswordHash))
}

func (s *pgStore) FindByEmail(ctx context.Context, email string) (account, error) {
	var a account
	err := s.pool.QueryRow(ctx, `SELECT user_id::text,name,email,role,total_points,password_hash FROM users WHERE email=$1`, email).
		Scan(&a.ID, &a.Name, &a.Email, &a.Role, &a.TotalPoints, &a.PasswordHash)
	return a, storeError(err)
}

type executor interface {
	Exec(context.Context, string, ...any) (pgconn.CommandTag, error)
}

func insertSession(ctx context.Context, db executor, s session) error {
	_, err := db.Exec(ctx, `INSERT INTO auth_sessions(session_id,user_id,access_hash,refresh_hash,access_expires_at,refresh_expires_at)
		VALUES($1,$2,$3,$4,$5,$6)`, s.ID, s.UserID, s.AccessHash, s.RefreshHash, s.AccessExpiresAt, s.RefreshExpiresAt)
	return err
}

func (s *pgStore) CreateSession(ctx context.Context, sess session) error {
	return insertSession(ctx, s.pool, sess)
}

func (s *pgStore) Authenticate(ctx context.Context, hash []byte) (principal, error) {
	var p principal
	err := s.pool.QueryRow(ctx, `SELECT u.user_id::text,u.name,u.email,u.role,u.total_points,s.session_id::text
		FROM auth_sessions s JOIN users u USING(user_id)
		WHERE s.access_hash=$1 AND s.access_expires_at>now() AND s.refresh_expires_at>now()`, hash).
		Scan(&p.User.ID, &p.User.Name, &p.User.Email, &p.User.Role, &p.User.TotalPoints, &p.SessionID)
	return p, storeError(err)
}

func (s *pgStore) Rotate(ctx context.Context, oldHash []byte, next session) (user, time.Time, time.Time, error) {
	var u user
	var accessExpiry, refreshExpiry time.Time
	// PostgreSQL rechecks the WHERE condition after waiting for a concurrent
	// updater: at most one request can consume a given refresh token.
	err := s.pool.QueryRow(ctx, `WITH rotated AS (
		UPDATE auth_sessions SET access_hash=$2,refresh_hash=$3,
		access_expires_at=LEAST($4,refresh_expires_at)
		WHERE refresh_hash=$1 AND refresh_expires_at>now()
		RETURNING user_id,access_expires_at,refresh_expires_at)
		SELECT u.user_id::text,u.name,u.email,u.role,u.total_points,r.access_expires_at,r.refresh_expires_at
		FROM rotated r JOIN users u USING(user_id)`, oldHash, next.AccessHash, next.RefreshHash, next.AccessExpiresAt).
		Scan(&u.ID, &u.Name, &u.Email, &u.Role, &u.TotalPoints, &accessExpiry, &refreshExpiry)
	return u, accessExpiry.UTC(), refreshExpiry.UTC(), storeError(err)
}

func (s *pgStore) Revoke(ctx context.Context, hash []byte) error {
	tag, err := s.pool.Exec(ctx, "DELETE FROM auth_sessions WHERE access_hash=$1", hash)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return errNotFound
	}
	return nil
}

func (s *pgStore) Ping(ctx context.Context) error { return s.pool.Ping(ctx) }
