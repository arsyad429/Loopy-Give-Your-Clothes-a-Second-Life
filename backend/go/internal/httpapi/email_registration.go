package httpapi

import (
	"context"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"math/big"
	"net/http"
	"regexp"
	"strings"
	"time"

	"loopy/backend/internal/domain"
)

type registrationMailer interface {
	SendVerificationCode(context.Context, string, string) error
}

// EmailRegistration handles the challenge required before creating a new account.
type EmailRegistration struct {
	store  domain.EmailRegistrationStore
	mailer registrationMailer
	key    []byte
}

// NewEmailRegistration prepares verification. A missing sender leaves registration unavailable.
func NewEmailRegistration(store domain.EmailRegistrationStore, mailer registrationMailer, hexKey string) (*EmailRegistration, error) {
	if mailer == nil {
		return &EmailRegistration{store: store}, nil
	}
	key, err := hex.DecodeString(hexKey)
	if err != nil || len(key) != 32 {
		return nil, errors.New("AUTH_OTP_HMAC_KEY must contain 32 random bytes encoded as hexadecimal")
	}
	return &EmailRegistration{store: store, mailer: mailer, key: key}, nil
}

type registrationChallenge struct {
	ID          string `json:"challengeId"`
	MaskedEmail string `json:"maskedEmail"`
	ExpiresIn   int    `json:"expiresInSeconds"`
	ResendAfter int    `json:"resendAfterSeconds"`
}

// RequireEmailRegistration enables email verification before public signup.
func (s *Server) RequireEmailRegistration(registration *EmailRegistration) *Server {
	s.emailRegistration = registration
	return s
}

func (e *EmailRegistration) digest(id, code string) []byte {
	mac := hmac.New(sha256.New, e.key)
	_, _ = mac.Write([]byte(id + ":" + code))
	return mac.Sum(nil)
}

func (s *Server) beginEmailRegistration(w http.ResponseWriter, r *http.Request, req domain.RegisterRequest) {
	e := s.emailRegistration
	if e.mailer == nil {
		writeError(w, mailUnavailable())
		return
	}
	req, err := domain.ValidateRegistration(req)
	if err != nil {
		writeError(w, err)
		return
	}
	account, err := MakeAccount(req)
	if err != nil {
		writeError(w, err)
		return
	}
	ctx := r.Context()
	s.issueRegistrationCode(w, ctx, account.ID, true, func(digest []byte, expiry, resend time.Time) (string, error) {
		return account.Email, e.store.BeginEmailRegistration(ctx, account.ID, account, digest, expiry, resend)
	})
}

func (s *Server) resendRegistration(w http.ResponseWriter, r *http.Request) {
	e := s.emailRegistration
	if e == nil || e.mailer == nil {
		writeError(w, mailUnavailable())
		return
	}
	var input struct {
		ID string `json:"challengeId"`
	}
	if err := decodeJSON(w, r, &input); err != nil {
		writeError(w, err)
		return
	}
	if !domain.UUIDPattern.MatchString(input.ID) {
		writeRegistrationError(w, domain.ErrRegistrationCodeExpired)
		return
	}
	ctx := r.Context()
	s.issueRegistrationCode(w, ctx, input.ID, false, func(digest []byte, expiry, resend time.Time) (string, error) {
		return e.store.ResendEmailRegistration(ctx, input.ID, digest, expiry, resend)
	})
}

func (s *Server) issueRegistrationCode(w http.ResponseWriter, ctx context.Context, id string, discardOnFailure bool,
	save func([]byte, time.Time, time.Time) (string, error),
) {
	e := s.emailRegistration
	number, err := rand.Int(rand.Reader, big.NewInt(1_000_000))
	if err != nil {
		writeError(w, err)
		return
	}
	code := fmt.Sprintf("%06d", number.Int64())
	now := time.Now()
	digest := e.digest(id, code)
	email, err := save(digest, now.Add(domain.RegistrationCodeTTL), now.Add(domain.RegistrationResendDelay))
	if err != nil {
		writeRegistrationError(w, err)
		return
	}
	if err := e.mailer.SendVerificationCode(ctx, email, code); err != nil {
		if discardOnFailure {
			cleanupCtx, cancel := context.WithTimeout(context.WithoutCancel(ctx), 2*time.Second)
			defer cancel()
			_ = e.store.CancelEmailRegistration(cleanupCtx, id, digest)
		}
		writeError(w, mailUnavailable())
		return
	}
	name, host, _ := strings.Cut(email, "@")
	initial := []rune(name)[0]
	writeJSON(w, 202, registrationChallenge{
		ID:          id,
		MaskedEmail: string(initial) + "***@" + host,
		ExpiresIn:   int(domain.RegistrationCodeTTL.Seconds()),
		ResendAfter: int(domain.RegistrationResendDelay.Seconds()),
	})
}

var registrationCodePattern = regexp.MustCompile(`^[0-9]{6}$`)

func (s *Server) verifyRegistration(w http.ResponseWriter, r *http.Request) {
	e := s.emailRegistration
	if e == nil || e.mailer == nil {
		writeError(w, mailUnavailable())
		return
	}
	var input struct {
		ID   string `json:"challengeId"`
		Code string `json:"code"`
	}
	if err := decodeJSON(w, r, &input); err != nil {
		writeError(w, err)
		return
	}
	if !domain.UUIDPattern.MatchString(input.ID) {
		writeRegistrationError(w, domain.ErrRegistrationCodeExpired)
		return
	}
	if !registrationCodePattern.MatchString(input.Code) {
		writeRegistrationError(w, domain.ErrRegistrationCodeInvalid)
		return
	}
	userID, err := domain.NewID()
	if err != nil {
		writeError(w, err)
		return
	}
	session, response, err := domain.NewSession(userID)
	if err != nil {
		writeError(w, err)
		return
	}
	response.User, err = e.store.CompleteEmailRegistration(r.Context(), input.ID, e.digest(input.ID, input.Code), session)
	if err != nil {
		writeRegistrationError(w, err)
		return
	}
	writeJSON(w, 201, response)
}

func mailUnavailable() *domain.APIError {
	return &domain.APIError{Status: 503, Code: "VERIFICATION_EMAIL_UNAVAILABLE", Message: "Verification email is temporarily unavailable."}
}

func writeRegistrationError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, domain.ErrRegistrationCoolingDown):
		w.Header().Set("Retry-After", "60")
		err = &domain.APIError{Status: 429, Code: "OTP_COOLDOWN", Message: "A recent verification request is still active."}
	case errors.Is(err, domain.ErrRegistrationCodeExpired):
		err = &domain.APIError{Status: 410, Code: "OTP_EXPIRED", Message: "Request a fresh verification code."}
	case errors.Is(err, domain.ErrRegistrationCodeInvalid):
		err = &domain.APIError{Status: 422, Code: "OTP_INVALID", Message: "The verification code is incorrect."}
	case errors.Is(err, domain.ErrRegistrationCodeLocked):
		err = &domain.APIError{Status: 429, Code: "OTP_LOCKED", Message: "Too many attempts. Request a fresh verification code."}
	}
	writeError(w, domain.AccountError(err))
}
