package domain

import (
	"context"
	"errors"
	"time"
)

var (
	ErrRegistrationCoolingDown = errors.New("registration cooldown")
	ErrRegistrationCodeInvalid = errors.New("invalid registration code")
	ErrRegistrationCodeExpired = errors.New("expired registration code")
	ErrRegistrationCodeLocked  = errors.New("registration code attempt limit reached")
)

const (
	RegistrationCodeTTL     = 5 * time.Minute
	RegistrationResendDelay = time.Minute
)

// EmailRegistrationStore manages pending registrations and atomically activates an account.
type EmailRegistrationStore interface {
	BeginEmailRegistration(context.Context, string, Account, []byte, time.Time, time.Time) error
	CancelEmailRegistration(context.Context, string, []byte) error
	ResendEmailRegistration(context.Context, string, []byte, time.Time, time.Time) (string, error)
	CompleteEmailRegistration(context.Context, string, []byte, Session) (User, error)
}
