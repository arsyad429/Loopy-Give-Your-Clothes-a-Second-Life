package httpapi

import (
	"context"
	"errors"
	"net/http"

	"loopy/backend/internal/domain"
	"loopy/backend/internal/security"
)

// AuthService coordinates registration, login, and token refresh flows.
type AuthService struct {
	store     domain.AuthRepository
	dummyHash string
}

// NewAuthService initializes AuthService with a dummy Argon2id hash for timing equalization.
func NewAuthService(store domain.AuthRepository) (*AuthService, error) {
	dummy, err := security.HashPassword("unused dummy password for timing")
	if err != nil {
		return nil, err
	}
	return &AuthService{store: store, dummyHash: dummy}, nil
}

// MakeAccount hashes a validated registration payload into a persisted Account.
func MakeAccount(req domain.RegisterRequest) (domain.Account, error) {
	id, err := domain.NewID()
	if err != nil {
		return domain.Account{}, err
	}
	hash, err := security.HashPassword(req.Password)
	return domain.Account{
		User:         domain.User{ID: id, Name: req.Name, Email: req.Email, Username: req.Username},
		PasswordHash: hash,
	}, err
}

// Register creates a new USER account and returns a fresh session pair.
func (a *AuthService) Register(ctx context.Context, req domain.RegisterRequest) (domain.SessionResponse, error) {
	req, err := domain.ValidateRegistration(req)
	if err != nil {
		return domain.SessionResponse{}, err
	}
	u, err := MakeAccount(req)
	if err != nil {
		return domain.SessionResponse{}, err
	}
	s, response, err := domain.NewSession(u.ID)
	if err != nil {
		return domain.SessionResponse{}, err
	}
	response.User, err = a.store.Register(ctx, u, s)
	return response, domain.AccountError(err)
}

// Login authenticates an account and returns a fresh session pair.
func (a *AuthService) Login(ctx context.Context, req domain.LoginRequest) (domain.SessionResponse, error) {
	invalid := &domain.APIError{Status: 401, Code: "INVALID_CREDENTIALS", Message: "Email or password is incorrect."}
	email := domain.NormalizeEmail(req.Email)
	if !domain.ValidEmail(email) || !domain.ValidPassword(req.Password) {
		return domain.SessionResponse{}, invalid
	}
	u, err := a.store.FindByEmail(ctx, email)
	if errors.Is(err, domain.ErrNotFound) {
		security.VerifyPassword(req.Password, a.dummyHash)
		return domain.SessionResponse{}, invalid
	}
	if err != nil {
		return domain.SessionResponse{}, err
	}
	if !security.VerifyPassword(req.Password, u.PasswordHash) {
		return domain.SessionResponse{}, invalid
	}
	s, response, err := domain.NewSession(u.ID)
	if err != nil {
		return domain.SessionResponse{}, err
	}
	if err = a.store.CreateSession(ctx, s, u.PasswordHash); err != nil {
		if errors.Is(err, domain.ErrNotFound) {
			err = invalid
		}
		return domain.SessionResponse{}, err
	}
	response.User = u.User
	return response, nil
}

// Refresh rotates a refresh token and returns a new session pair.
func (a *AuthService) Refresh(ctx context.Context, token string) (domain.SessionResponse, error) {
	if !domain.ValidToken(token) {
		return domain.SessionResponse{}, domain.Unauthorized()
	}
	s, response, err := domain.NewSession("")
	if err != nil {
		return domain.SessionResponse{}, err
	}
	response.User, response.ExpiresAt, response.RefreshExpiresAt, err = a.store.Rotate(ctx, domain.TokenHash(token), s)
	if errors.Is(err, domain.ErrNotFound) {
		return domain.SessionResponse{}, domain.Unauthorized()
	}
	return response, err
}

func (s *Server) register(w http.ResponseWriter, r *http.Request) {
	var req domain.RegisterRequest
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	if s.emailRegistration != nil {
		s.beginEmailRegistration(w, r, req)
		return
	}
	response, err := s.auth.Register(r.Context(), req)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, 201, response)
}

func (s *Server) login(w http.ResponseWriter, r *http.Request) {
	var req domain.LoginRequest
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	response, err := s.auth.Login(r.Context(), req)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, 200, response)
}

func (s *Server) refresh(w http.ResponseWriter, r *http.Request) {
	var req struct {
		RefreshToken string `json:"refreshToken"`
	}
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	response, err := s.auth.Refresh(r.Context(), req.RefreshToken)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, 200, response)
}

func (s *Server) updateProfile(w http.ResponseWriter, r *http.Request) {
	var req domain.ProfileUpdateRequest
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	req, err := domain.ValidateProfileUpdate(req)
	if err != nil {
		writeError(w, err)
		return
	}
	principal := currentPrincipal(r)
	change := domain.ProfileUpdate{Principal: principal, Name: req.Name, Username: req.Username}
	if req.NewPassword != "" {
		account, err := s.auth.store.FindByEmail(r.Context(), principal.User.Email)
		if err != nil {
			writeError(w, err)
			return
		}
		if !security.VerifyPassword(req.CurrentPassword, account.PasswordHash) {
			writeError(w, &domain.APIError{
				Status: 422, Code: "VALIDATION_ERROR", Message: "Current password is incorrect.",
				Fields: map[string]string{"currentPassword": "Current password is incorrect."},
			})
			return
		}
		change.OldPasswordHash = account.PasswordHash
		change.NewPasswordHash, err = security.HashPassword(req.NewPassword)
		if err != nil {
			writeError(w, err)
			return
		}
	}
	profile, err := s.auth.store.UpdateProfile(r.Context(), change)
	if errors.Is(err, domain.ErrProfileChanged) {
		err = &domain.APIError{Status: 409, Code: "PROFILE_CHANGED", Message: "Reload the profile and try again."}
	}
	if err != nil {
		writeError(w, domain.AccountError(err))
		return
	}
	writeJSON(w, 200, profile)
}

func (s *Server) logout(w http.ResponseWriter, r *http.Request) {
	err := s.auth.store.Revoke(r.Context(), domain.TokenHash(bearerToken(r)))
	if errors.Is(err, domain.ErrNotFound) {
		err = domain.Unauthorized()
	}
	if err != nil {
		writeError(w, err)
		return
	}
	w.WriteHeader(204)
}
