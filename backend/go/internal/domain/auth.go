package domain

import (
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	"net/mail"
	"strings"
	"time"
	"unicode"
	"unicode/utf8"
)

const (
	MinPasswordLength = 8
	MaxPasswordLength = 128
	maxPasswordBytes  = 512
)

// User represents a public Loopy account profile.
type User struct {
	ID          string `json:"userId"`
	Name        string `json:"name"`
	Email       string `json:"email"`
	Role        string `json:"role"`
	TotalPoints int    `json:"totalPoints"`
}

// Account extends User with the stored password hash.
type Account struct {
	User
	PasswordHash string
}

// Session represents a revocable, per-device authentication session.
type Session struct {
	ID               string
	UserID           string
	AccessHash       []byte
	RefreshHash      []byte
	AccessExpiresAt  time.Time
	RefreshExpiresAt time.Time
}

// Principal identifies the authenticated caller for a request.
type Principal struct {
	User      User
	SessionID string
}

// SessionResponse is the token pair and profile returned after register, login, or refresh.
type SessionResponse struct {
	AccessToken      string    `json:"accessToken"`
	RefreshToken     string    `json:"refreshToken"`
	TokenType        string    `json:"tokenType"`
	ExpiresAt        time.Time `json:"expiresAt"`
	RefreshExpiresAt time.Time `json:"refreshExpiresAt"`
	User             User      `json:"user"`
}

// RegisterRequest holds user registration payload fields.
type RegisterRequest struct {
	Name     string `json:"name"`
	Email    string `json:"email"`
	Password string `json:"password"`
}

// LoginRequest holds credential payload fields for sign-in.
type LoginRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

// ValidateRegistration normalizes and checks registration input boundaries.
func ValidateRegistration(req RegisterRequest) (RegisterRequest, error) {
	req.Name = strings.TrimSpace(req.Name)
	req.Email = NormalizeEmail(req.Email)
	fields := map[string]string{}
	if !utf8.ValidString(req.Name) || utf8.RuneCountInString(req.Name) < 1 || utf8.RuneCountInString(req.Name) > 100 || strings.ContainsFunc(req.Name, unicode.IsControl) {
		fields["name"] = "Use 1–100 characters without control characters."
	}
	if !ValidEmail(req.Email) {
		fields["email"] = "Enter a valid email address (at most 254 bytes)."
	}
	if !ValidPassword(req.Password) {
		fields["password"] = fmt.Sprintf("Use %d–%d characters (at most %d bytes).", MinPasswordLength, MaxPasswordLength, maxPasswordBytes)
	}
	if len(fields) != 0 {
		return req, &APIError{Status: 422, Code: "VALIDATION_ERROR", Message: "Check the submitted fields.", Fields: fields}
	}
	return req, nil
}

// NormalizeEmail trims whitespace and lowercases an email address.
func NormalizeEmail(email string) string { return strings.ToLower(strings.TrimSpace(email)) }

// ValidEmail enforces RFC address parsing without display names or line breaks.
func ValidEmail(email string) bool {
	if len(email) > 254 || !utf8.ValidString(email) || strings.ContainsAny(email, "\r\n") {
		return false
	}
	address, err := mail.ParseAddress(email)
	return err == nil && address.Address == email && address.Name == "" && strings.Contains(email, "@")
}

// ValidPassword checks passphrase rune and byte length policy.
func ValidPassword(password string) bool {
	n := utf8.RuneCountInString(password)
	return utf8.ValidString(password) && n >= MinPasswordLength && n <= MaxPasswordLength && len(password) <= maxPasswordBytes
}

// NewID generates a random UUIDv4 string.
func NewID() (string, error) {
	b := make([]byte, 16)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	b[6] = (b[6] & 0x0f) | 0x40
	b[8] = (b[8] & 0x3f) | 0x80
	return fmt.Sprintf("%x-%x-%x-%x-%x", b[:4], b[4:6], b[6:8], b[8:10], b[10:]), nil
}

// NewToken generates an opaque 256-bit base64url token.
func NewToken() (string, error) {
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return base64.RawURLEncoding.EncodeToString(b), nil
}

// ValidToken verifies the expected length and encoding of an opaque token.
func ValidToken(token string) bool {
	if len(token) != 43 {
		return false
	}
	b, err := base64.RawURLEncoding.Strict().DecodeString(token)
	return err == nil && len(b) == 32
}

// TokenHash computes the SHA-256 digest stored in PostgreSQL for a raw session token.
func TokenHash(token string) []byte {
	h := sha256.Sum256([]byte(token))
	return h[:]
}

// NewSession issues a new Session record alongside its raw SessionResponse pair.
func NewSession(userID string) (Session, SessionResponse, error) {
	id, err := NewID()
	if err != nil {
		return Session{}, SessionResponse{}, err
	}
	access, err := NewToken()
	if err != nil {
		return Session{}, SessionResponse{}, err
	}
	refresh, err := NewToken()
	if err != nil {
		return Session{}, SessionResponse{}, err
	}
	now := time.Now().UTC().Truncate(time.Microsecond) // PostgreSQL timestamp precision.
	s := Session{
		ID:               id,
		UserID:           userID,
		AccessHash:       TokenHash(access),
		RefreshHash:      TokenHash(refresh),
		AccessExpiresAt:  now.Add(15 * time.Minute),
		RefreshExpiresAt: now.Add(30 * 24 * time.Hour),
	}
	r := SessionResponse{
		AccessToken:      access,
		RefreshToken:     refresh,
		TokenType:        "Bearer",
		ExpiresAt:        s.AccessExpiresAt,
		RefreshExpiresAt: s.RefreshExpiresAt,
	}
	return s, r, nil
}
