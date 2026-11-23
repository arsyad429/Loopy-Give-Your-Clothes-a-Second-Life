package main

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"
	"net/mail"
	"strings"
	"time"
	"unicode"
	"unicode/utf8"
)

var (
	errNotFound       = errors.New("record not found")
	errDuplicateEmail = errors.New("duplicate email")
)

type user struct {
	ID          string `json:"userId"`
	Name        string `json:"name"`
	Email       string `json:"email"`
	Role        string `json:"role"`
	TotalPoints int    `json:"totalPoints"`
}

type account struct {
	user
	PasswordHash string
}

type session struct {
	ID, UserID                        string
	AccessHash, RefreshHash           []byte
	AccessExpiresAt, RefreshExpiresAt time.Time
}

type principal struct {
	User      user
	SessionID string
}

type sessionResponse struct {
	AccessToken      string    `json:"accessToken"`
	RefreshToken     string    `json:"refreshToken"`
	TokenType        string    `json:"tokenType"`
	ExpiresAt        time.Time `json:"expiresAt"`
	RefreshExpiresAt time.Time `json:"refreshExpiresAt"`
	User             user      `json:"user"`
}

type registerRequest struct {
	Name     string `json:"name"`
	Email    string `json:"email"`
	Password string `json:"password"`
}

type loginRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

type repository interface {
	Register(context.Context, account, session) (user, error)
	CreateAdmin(context.Context, account) (user, error)
	FindByEmail(context.Context, string) (account, error)
	CreateSession(context.Context, session) error
	Authenticate(context.Context, []byte) (principal, error)
	Rotate(context.Context, []byte, session) (user, time.Time, time.Time, error)
	Revoke(context.Context, []byte) error
	Ping(context.Context) error
}

type apiError struct {
	Status  int               `json:"-"`
	Code    string            `json:"code"`
	Message string            `json:"message"`
	Fields  map[string]string `json:"fieldErrors,omitempty"`
}

func (e *apiError) Error() string { return e.Code }

func unauthorized() *apiError {
	return &apiError{Status: 401, Code: "INVALID_SESSION", Message: "Sign in again to continue."}
}

func validateRegistration(req registerRequest) (registerRequest, error) {
	req.Name = strings.TrimSpace(req.Name)
	req.Email = normalizeEmail(req.Email)
	fields := map[string]string{}
	if !utf8.ValidString(req.Name) || utf8.RuneCountInString(req.Name) < 1 || utf8.RuneCountInString(req.Name) > 100 || strings.ContainsFunc(req.Name, unicode.IsControl) {
		fields["name"] = "Use 1–100 characters without control characters."
	}
	if !validEmail(req.Email) {
		fields["email"] = "Enter a valid email address (at most 254 bytes)."
	}
	if !validPassword(req.Password) {
		fields["password"] = "Use 15–128 characters (at most 512 bytes)."
	}
	if len(fields) != 0 {
		return req, &apiError{Status: 422, Code: "VALIDATION_ERROR", Message: "Check the submitted fields.", Fields: fields}
	}
	return req, nil
}

func normalizeEmail(email string) string { return strings.ToLower(strings.TrimSpace(email)) }

func validEmail(email string) bool {
	if len(email) > 254 || !utf8.ValidString(email) || strings.ContainsAny(email, "\r\n") {
		return false
	}
	address, err := mail.ParseAddress(email)
	return err == nil && address.Address == email && address.Name == "" && strings.Contains(email, "@")
}

func validPassword(password string) bool {
	n := utf8.RuneCountInString(password)
	return utf8.ValidString(password) && n >= 15 && n <= 128 && len(password) <= 512
}

type authService struct {
	store     repository
	dummyHash string
}

func newAuthService(store repository) (*authService, error) {
	dummy, err := hashPassword("unused dummy password for timing")
	if err != nil {
		return nil, err
	}
	return &authService{store: store, dummyHash: dummy}, nil
}

func (a *authService) register(ctx context.Context, req registerRequest) (sessionResponse, error) {
	req, err := validateRegistration(req)
	if err != nil {
		return sessionResponse{}, err
	}
	u, err := makeAccount(req)
	if err != nil {
		return sessionResponse{}, err
	}
	s, response, err := newSession(u.ID)
	if err != nil {
		return sessionResponse{}, err
	}
	response.User, err = a.store.Register(ctx, u, s)
	return response, accountError(err)
}

func makeAccount(req registerRequest) (account, error) {
	id, err := newID()
	if err != nil {
		return account{}, err
	}
	hash, err := hashPassword(req.Password)
	return account{user: user{ID: id, Name: req.Name, Email: req.Email}, PasswordHash: hash}, err
}

func accountError(err error) error {
	if errors.Is(err, errDuplicateEmail) {
		return &apiError{Status: 409, Code: "EMAIL_ALREADY_REGISTERED", Message: "An account already uses this email."}
	}
	return err
}

func (a *authService) login(ctx context.Context, req loginRequest) (sessionResponse, error) {
	invalid := &apiError{Status: 401, Code: "INVALID_CREDENTIALS", Message: "Email or password is incorrect."}
	email := normalizeEmail(req.Email)
	if !validEmail(email) || !validPassword(req.Password) {
		return sessionResponse{}, invalid
	}
	u, err := a.store.FindByEmail(ctx, email)
	if errors.Is(err, errNotFound) {
		verifyPassword(req.Password, a.dummyHash)
		return sessionResponse{}, invalid
	}
	if err != nil {
		return sessionResponse{}, err
	}
	if !verifyPassword(req.Password, u.PasswordHash) {
		return sessionResponse{}, invalid
	}
	s, response, err := newSession(u.ID)
	if err != nil {
		return sessionResponse{}, err
	}
	if err = a.store.CreateSession(ctx, s); err != nil {
		return sessionResponse{}, err
	}
	response.User = u.user
	return response, nil
}

func (a *authService) refresh(ctx context.Context, token string) (sessionResponse, error) {
	if !validToken(token) {
		return sessionResponse{}, unauthorized()
	}
	s, response, err := newSession("")
	if err != nil {
		return sessionResponse{}, err
	}
	response.User, response.ExpiresAt, response.RefreshExpiresAt, err = a.store.Rotate(ctx, tokenHash(token), s)
	if errors.Is(err, errNotFound) {
		return sessionResponse{}, unauthorized()
	}
	return response, err
}

func newID() (string, error) {
	b := make([]byte, 16)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	b[6] = (b[6] & 0x0f) | 0x40
	b[8] = (b[8] & 0x3f) | 0x80
	return fmt.Sprintf("%x-%x-%x-%x-%x", b[:4], b[4:6], b[6:8], b[8:10], b[10:]), nil
}

func newToken() (string, error) {
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return base64.RawURLEncoding.EncodeToString(b), nil
}

func validToken(token string) bool {
	if len(token) != 43 {
		return false
	}
	b, err := base64.RawURLEncoding.Strict().DecodeString(token)
	return err == nil && len(b) == 32
}

func tokenHash(token string) []byte {
	h := sha256.Sum256([]byte(token))
	return h[:]
}

func newSession(userID string) (session, sessionResponse, error) {
	id, err := newID()
	if err != nil {
		return session{}, sessionResponse{}, err
	}
	access, err := newToken()
	if err != nil {
		return session{}, sessionResponse{}, err
	}
	refresh, err := newToken()
	if err != nil {
		return session{}, sessionResponse{}, err
	}
	now := time.Now().UTC().Truncate(time.Microsecond) // PostgreSQL timestamp precision.
	s := session{ID: id, UserID: userID, AccessHash: tokenHash(access), RefreshHash: tokenHash(refresh), AccessExpiresAt: now.Add(15 * time.Minute), RefreshExpiresAt: now.Add(30 * 24 * time.Hour)}
	r := sessionResponse{AccessToken: access, RefreshToken: refresh, TokenType: "Bearer", ExpiresAt: s.AccessExpiresAt, RefreshExpiresAt: s.RefreshExpiresAt}
	return s, r, nil
}
