package domain

import "errors"

// Sentinel errors returned by stores and services and mapped to HTTP status codes by httpapi.
var (
	ErrNotFound            = errors.New("record not found")
	ErrDuplicateEmail      = errors.New("duplicate email")
	ErrDonationChanged     = errors.New("donation version changed")
	ErrDonationFinal       = errors.New("donation is no longer pending review")
	ErrPickupInPast        = errors.New("pickup time is not in the future")
	ErrCheckNotEligible    = errors.New("clothing check is not potentially eligible")
	ErrDonationKeyConflict = errors.New("donation idempotency key conflict")
	ErrCheckInProgress     = errors.New("clothing check in progress")
	ErrCheckKeyConflict    = errors.New("clothing check payload does not match idempotency key")
	ErrCheckClaimLost      = errors.New("clothing check reservation expired")
)

// APIError is a safe, structured error formatted for HTTP responses without leaking internals.
type APIError struct {
	Status  int               `json:"-"`
	Code    string            `json:"code"`
	Message string            `json:"message"`
	Fields  map[string]string `json:"fieldErrors,omitempty"`
}

func (e *APIError) Error() string { return e.Code }

// Unauthorized constructs the standard 401 session error.
func Unauthorized() *APIError {
	return &APIError{Status: 401, Code: "INVALID_SESSION", Message: "Sign in again to continue."}
}

// DonationValidation constructs a 422 validation error with field-level messages.
func DonationValidation(fields map[string]string) *APIError {
	return &APIError{Status: 422, Code: "VALIDATION_ERROR", Message: "Check the submitted fields.", Fields: fields}
}

// RewardError constructs a structured reward domain API error.
func RewardError(status int, code, message string) error {
	return &APIError{Status: status, Code: code, Message: message}
}

// AccountError maps storage sentinel errors into client-safe account errors.
func AccountError(err error) error {
	if errors.Is(err, ErrDuplicateEmail) {
		return &APIError{Status: 409, Code: "EMAIL_ALREADY_REGISTERED", Message: "An account already uses this email."}
	}
	return err
}
