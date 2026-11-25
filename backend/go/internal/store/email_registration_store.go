package store

import (
	"context"
	"crypto/subtle"
	"errors"
	"time"

	"loopy/backend/internal/domain"
)

// BeginEmailRegistration reserves an email until its current code expires.
func (s *PGStore) BeginEmailRegistration(ctx context.Context, id string, account domain.Account, digest []byte, expiry, resend time.Time) error {
	var emailTaken, usernameTaken bool
	if err := s.pool.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM users WHERE email=$1),
		EXISTS(SELECT 1 FROM users WHERE username=NULLIF($2,''))`, account.Email, account.Username).Scan(&emailTaken, &usernameTaken); err != nil {
		return err
	}
	if emailTaken {
		return domain.ErrDuplicateEmail
	}
	if usernameTaken {
		return domain.ErrDuplicateUsername
	}
	if _, err := s.pool.Exec(ctx, `DELETE FROM email_registrations WHERE started_at <= now()-interval '30 minutes' AND expires_at<=now()`); err != nil {
		return err
	}
	tag, err := s.pool.Exec(ctx, `INSERT INTO email_registrations (challenge_id,email,name,username,password_hash,code_hash,expires_at,resend_after)
		VALUES($1,$2,$3,NULLIF($4,''),$5,$6,$7,$8)
		ON CONFLICT(email) DO UPDATE SET challenge_id=EXCLUDED.challenge_id,name=EXCLUDED.name,
		username=EXCLUDED.username,password_hash=EXCLUDED.password_hash,code_hash=EXCLUDED.code_hash,
		expires_at=EXCLUDED.expires_at,resend_after=EXCLUDED.resend_after,attempts=0,started_at=now()
		WHERE email_registrations.expires_at <= now()`,
		id, account.Email, account.Name, account.Username, account.PasswordHash, digest, expiry, resend)
	if err == nil && tag.RowsAffected() == 0 {
		return domain.ErrRegistrationCoolingDown
	}
	return err
}

// CancelEmailRegistration discards an undelivered initial challenge without deleting a replacement.
func (s *PGStore) CancelEmailRegistration(ctx context.Context, id string, digest []byte) error {
	_, err := s.pool.Exec(ctx, `DELETE FROM email_registrations WHERE challenge_id=$1 AND code_hash=$2`, id, digest)
	return err
}

// ResendEmailRegistration rotates the code for the same pending registration.
func (s *PGStore) ResendEmailRegistration(ctx context.Context, id string, digest []byte, expiry, resend time.Time) (string, error) {
	var email string
	err := s.pool.QueryRow(ctx, `UPDATE email_registrations SET code_hash=$2,expires_at=$3,resend_after=$4,attempts=0
		WHERE challenge_id=$1 AND resend_after<=now() AND started_at>now()-interval '30 minutes' RETURNING email`,
		id, digest, expiry, resend).Scan(&email)
	if !errors.Is(storeError(err), domain.ErrNotFound) {
		return email, err
	}
	var active bool
	err = s.pool.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM email_registrations
		WHERE challenge_id=$1 AND started_at>now()-interval '30 minutes')`, id).Scan(&active)
	if err != nil {
		return "", err
	}
	if !active {
		return "", domain.ErrRegistrationCodeExpired
	}
	return "", domain.ErrRegistrationCoolingDown
}

// CompleteEmailRegistration consumes the code, creates the verified account and starts its session in one transaction.
func (s *PGStore) CompleteEmailRegistration(ctx context.Context, id string, digest []byte, session domain.Session) (domain.User, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return domain.User{}, err
	}
	defer tx.Rollback(context.Background())

	var account domain.Account
	var storedDigest []byte
	var attempts int
	var expired bool
	err = storeError(tx.QueryRow(ctx, `SELECT email,name,COALESCE(username,''),password_hash,code_hash,attempts,
		expires_at<=clock_timestamp() FROM email_registrations WHERE challenge_id=$1 FOR UPDATE`, id).
		Scan(&account.Email, &account.Name, &account.Username, &account.PasswordHash, &storedDigest, &attempts, &expired))
	if errors.Is(err, domain.ErrNotFound) || expired {
		return domain.User{}, domain.ErrRegistrationCodeExpired
	}
	if err != nil {
		return domain.User{}, err
	}
	if attempts >= 5 {
		return domain.User{}, domain.ErrRegistrationCodeLocked
	}
	if subtle.ConstantTimeCompare(storedDigest, digest) != 1 {
		if _, err = tx.Exec(ctx, `UPDATE email_registrations SET attempts=attempts+1 WHERE challenge_id=$1`, id); err != nil {
			return domain.User{}, err
		}
		if err = tx.Commit(ctx); err != nil {
			return domain.User{}, err
		}
		if attempts == 4 {
			return domain.User{}, domain.ErrRegistrationCodeLocked
		}
		return domain.User{}, domain.ErrRegistrationCodeInvalid
	}

	user, err := scanUser(tx.QueryRow(ctx, `INSERT INTO users AS u(user_id,name,email,password_hash,username,email_verified_at)
		VALUES($1,$2,$3,$4,NULLIF($5,''),now()) RETURNING `+userColumns,
		session.UserID, account.Name, account.Email, account.PasswordHash, account.Username))
	if err != nil {
		return domain.User{}, err
	}
	if err = insertSession(ctx, tx, session); err != nil {
		return domain.User{}, err
	}
	if _, err = tx.Exec(ctx, `DELETE FROM email_registrations WHERE challenge_id=$1`, id); err != nil {
		return domain.User{}, err
	}
	return user, tx.Commit(ctx)
}
