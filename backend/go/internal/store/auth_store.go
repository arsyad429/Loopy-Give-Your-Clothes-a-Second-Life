package store

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"loopy/backend/internal/domain"
)

type executor interface {
	Exec(context.Context, string, ...any) (pgconn.CommandTag, error)
}

const userColumns = `u.user_id::text,u.name,u.email,u.role,u.total_points,COALESCE(u.username,''),u.email_verified_at IS NOT NULL`

func userFields(u *domain.User) []any {
	return []any{&u.ID, &u.Name, &u.Email, &u.Role, &u.TotalPoints, &u.Username, &u.EmailVerified}
}

func scanUser(row pgx.Row) (domain.User, error) {
	var u domain.User
	err := row.Scan(userFields(&u)...)
	return u, storeError(err)
}

// Register creates a new USER account and initial session in a single transaction.
func (s *PGStore) Register(ctx context.Context, a domain.Account, session domain.Session) (domain.User, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return domain.User{}, err
	}
	defer tx.Rollback(context.Background())

	u, err := scanUser(tx.QueryRow(ctx, `INSERT INTO users AS u (user_id,name,email,password_hash,username) VALUES($1,$2,$3,$4,NULLIF($5,''))
		RETURNING `+userColumns, a.ID, a.Name, a.Email, a.PasswordHash, a.Username))
	if err != nil {
		return domain.User{}, err
	}
	if err = insertSession(ctx, tx, session); err != nil {
		return domain.User{}, err
	}
	if err = tx.Commit(ctx); err != nil {
		return domain.User{}, err
	}
	return u, nil
}

// CreateAdmin creates a new ADMIN account.
func (s *PGStore) CreateAdmin(ctx context.Context, a domain.Account) (domain.User, error) {
	return scanUser(s.pool.QueryRow(ctx, `INSERT INTO users AS u (user_id,name,email,password_hash,role,username) VALUES($1,$2,$3,$4,'ADMIN',NULLIF($5,''))
		RETURNING `+userColumns, a.ID, a.Name, a.Email, a.PasswordHash, a.Username))
}

// FindByEmail retrieves an account by normalized email address.
func (s *PGStore) FindByEmail(ctx context.Context, email string) (domain.Account, error) {
	var a domain.Account
	err := s.pool.QueryRow(ctx, `SELECT `+userColumns+`,u.password_hash FROM users u WHERE u.email=$1`, email).
		Scan(append(userFields(&a.User), &a.PasswordHash)...)
	return a, storeError(err)
}

// UpdateProfile applies edits and revokes other sessions when a password changes.
func (s *PGStore) UpdateProfile(ctx context.Context, change domain.ProfileUpdate) (domain.User, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return domain.User{}, err
	}
	defer tx.Rollback(context.Background())

	id := change.Principal.User.ID
	u, err := scanUser(tx.QueryRow(ctx, `UPDATE users AS u SET name=$2,username=NULLIF($3,''),
		password_hash=COALESCE(NULLIF($5,''),u.password_hash)
		WHERE u.user_id=$1 AND ($4='' OR u.password_hash=$4)
		RETURNING `+userColumns, id, change.Name, change.Username, change.OldPasswordHash, change.NewPasswordHash))
	if err == domain.ErrNotFound {
		return domain.User{}, domain.ErrProfileChanged
	}
	if err != nil {
		return domain.User{}, err
	}
	if change.NewPasswordHash != "" {
		_, err = tx.Exec(ctx, `DELETE FROM auth_sessions WHERE user_id=$1 AND session_id<>$2`, id, change.Principal.SessionID)
		if err != nil {
			return domain.User{}, err
		}
	}
	return u, tx.Commit(ctx)
}

func insertSession(ctx context.Context, db executor, s domain.Session) error {
	_, err := db.Exec(ctx, `INSERT INTO auth_sessions(session_id,user_id,access_hash,refresh_hash,access_expires_at,refresh_expires_at)
		VALUES($1,$2,$3,$4,$5,$6)`, s.ID, s.UserID, s.AccessHash, s.RefreshHash, s.AccessExpiresAt, s.RefreshExpiresAt)
	return err
}

// CreateSession persists a new authentication session.
func (s *PGStore) CreateSession(ctx context.Context, sess domain.Session, passwordHash string) error {
	tag, err := s.pool.Exec(ctx, `WITH account AS (
		SELECT user_id FROM users WHERE user_id=$2 AND password_hash=$7 FOR UPDATE
	)
	INSERT INTO auth_sessions(session_id,user_id,access_hash,refresh_hash,access_expires_at,refresh_expires_at)
	SELECT $1,account.user_id,$3,$4,$5,$6 FROM account`,
		sess.ID, sess.UserID, sess.AccessHash, sess.RefreshHash, sess.AccessExpiresAt, sess.RefreshExpiresAt, passwordHash)
	if err == nil && tag.RowsAffected() == 0 {
		return domain.ErrNotFound
	}
	return err
}

// Authenticate resolves an active principal from a hashed Bearer access token.
func (s *PGStore) Authenticate(ctx context.Context, hash []byte) (domain.Principal, error) {
	var p domain.Principal
	err := s.pool.QueryRow(ctx, `SELECT `+userColumns+`,s.session_id::text
		FROM auth_sessions s JOIN users u USING(user_id)
		WHERE s.access_hash=$1 AND s.access_expires_at>now() AND s.refresh_expires_at>now()`, hash).
		Scan(append(userFields(&p.User), &p.SessionID)...)
	return p, storeError(err)
}

// Rotate atomically replaces a refresh token pair and returns updated account details.
func (s *PGStore) Rotate(ctx context.Context, oldHash []byte, next domain.Session) (domain.User, time.Time, time.Time, error) {
	var u domain.User
	var accessExpiry, refreshExpiry time.Time
	// PostgreSQL rechecks the WHERE condition after waiting for a concurrent
	// updater: at most one request can consume a given refresh token.
	err := s.pool.QueryRow(ctx, `WITH rotated AS (
		UPDATE auth_sessions SET access_hash=$2,refresh_hash=$3,
		access_expires_at=LEAST($4,refresh_expires_at)
		WHERE refresh_hash=$1 AND refresh_expires_at>now()
		RETURNING user_id,access_expires_at,refresh_expires_at)
		SELECT `+userColumns+`,r.access_expires_at,r.refresh_expires_at
		FROM rotated r JOIN users u USING(user_id)`, oldHash, next.AccessHash, next.RefreshHash, next.AccessExpiresAt).
		Scan(append(userFields(&u), &accessExpiry, &refreshExpiry)...)
	return u, accessExpiry.UTC(), refreshExpiry.UTC(), storeError(err)
}

// Revoke deletes the session matching the hashed access token.
func (s *PGStore) Revoke(ctx context.Context, hash []byte) error {
	tag, err := s.pool.Exec(ctx, "DELETE FROM auth_sessions WHERE access_hash=$1", hash)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return domain.ErrNotFound
	}
	return nil
}
