package httpapi

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"mime"
	"net"
	"net/http"
	"strings"
	"sync"
	"time"

	"loopy/backend/internal/domain"
)

// Server wires HTTP handlers, authentication, rate limiting, and routing.
type Server struct {
	auth      *AuthService
	donations domain.DonationRepository
	rewards   domain.RewardsRepository
	checks    *ClothingCheckService
	limiter   *authLimiter
	hashSlots chan struct{}
}

// NewServer constructs an HTTP API server with its domain services.
func NewServer(
	auth *AuthService,
	donations domain.DonationRepository,
	checks *ClothingCheckService,
) *Server {
	rewards, _ := donations.(domain.RewardsRepository)
	return &Server{
		auth:      auth,
		donations: donations,
		rewards:   rewards,
		checks:    checks,
		limiter:   &authLimiter{peers: make(map[string]limitWindow)},
		hashSlots: make(chan struct{}, 4),
	}
}

// Routes builds the complete HTTP mux wrapped with security headers and per-request timeouts.
func (s *Server) Routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]string{"status": "ok", "mode": "go-auth"})
	})
	mux.HandleFunc("GET /ready", func(w http.ResponseWriter, r *http.Request) {
		ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
		defer cancel()
		if err := s.auth.store.Ping(ctx); err != nil {
			writeError(w, &domain.APIError{Status: 503, Code: "NOT_READY", Message: "Database unavailable."})
			return
		}
		writeJSON(w, 200, map[string]string{"status": "ready"})
	})

	mux.Handle("POST /v1/auth/register", s.limit(true, http.HandlerFunc(s.register)))
	mux.Handle("POST /v1/auth/login", s.limit(true, http.HandlerFunc(s.login)))
	mux.Handle("POST /v1/auth/refresh", s.limit(false, http.HandlerFunc(s.refresh)))
	mux.Handle("GET /v1/auth/me", s.Authenticate(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, currentPrincipal(r).User)
	})))
	mux.Handle("POST /v1/auth/logout", s.Authenticate(http.HandlerFunc(s.logout)))

	if s.donations != nil {
		s.mountDonationRoutes(mux)
	}
	if s.rewards != nil {
		s.mountRewardRoutes(mux)
	}
	if s.checks != nil {
		s.mountClothingCheckRoutes(mux)
	}

	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		w.Header().Set("X-Content-Type-Options", "nosniff")
		timeout := 10 * time.Second
		if r.Method == http.MethodPost && r.URL.Path == "/v1/checks" {
			timeout = checkTimeout
		}
		ctx, cancel := context.WithTimeout(r.Context(), timeout)
		defer cancel()
		mux.ServeHTTP(w, r.WithContext(ctx))
	})
}

func decodeJSON(w http.ResponseWriter, r *http.Request, dst any) error {
	mediaType, _, err := mime.ParseMediaType(r.Header.Get("Content-Type"))
	if err != nil || mediaType != "application/json" {
		return &domain.APIError{Status: 415, Code: "UNSUPPORTED_MEDIA_TYPE", Message: "Use Content-Type: application/json."}
	}
	r.Body = http.MaxBytesReader(w, r.Body, 16*1024)
	defer r.Body.Close()

	// Require an object; decoding JSON null into a struct otherwise succeeds.
	var raw json.RawMessage
	decoder := json.NewDecoder(r.Body)
	if err = decoder.Decode(&raw); err == nil {
		var extra any
		if end := decoder.Decode(&extra); end != io.EOF {
			if end == nil {
				end = errors.New("multiple JSON values")
			}
			err = end
		}
	}
	var tooLarge *http.MaxBytesError
	if errors.As(err, &tooLarge) {
		return &domain.APIError{Status: 413, Code: "BODY_TOO_LARGE", Message: "Request body must be at most 16 KiB."}
	}
	if err == nil && len(raw) > 0 && raw[0] == '{' {
		strict := json.NewDecoder(strings.NewReader(string(raw)))
		strict.DisallowUnknownFields()
		err = strict.Decode(dst)
		if err == nil {
			return nil
		}
	}
	return &domain.APIError{Status: 400, Code: "INVALID_JSON", Message: "Send one JSON object with only the documented fields."}
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(body)
}

func writeError(w http.ResponseWriter, err error) {
	var apiErr *domain.APIError
	if !errors.As(err, &apiErr) {
		// Avoid logging SQL errors, which can include email addresses and hashes.
		slog.Error("request failed", "errorType", fmt.Sprintf("%T", err))
		apiErr = &domain.APIError{Status: 500, Code: "INTERNAL_ERROR", Message: "The request could not be completed."}
	}
	writeJSON(w, apiErr.Status, map[string]any{"error": apiErr})
}

type principalKey struct{}

func currentPrincipal(r *http.Request) domain.Principal {
	p, _ := r.Context().Value(principalKey{}).(domain.Principal)
	return p
}

func bearerToken(r *http.Request) string {
	if len(r.Header.Values("Authorization")) != 1 {
		return ""
	}
	parts := strings.Fields(r.Header.Get("Authorization"))
	if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") || !domain.ValidToken(parts[1]) {
		return ""
	}
	return parts[1]
}

// Authenticate validates the Bearer access token against active database sessions.
func (s *Server) Authenticate(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		token := bearerToken(r)
		if token == "" {
			w.Header().Set("WWW-Authenticate", "Bearer")
			writeError(w, domain.Unauthorized())
			return
		}
		p, err := s.auth.store.Authenticate(r.Context(), domain.TokenHash(token))
		if errors.Is(err, domain.ErrNotFound) {
			w.Header().Set("WWW-Authenticate", "Bearer")
			err = domain.Unauthorized()
		}
		if err != nil {
			writeError(w, err)
			return
		}
		next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), principalKey{}, p)))
	})
}

// RequireRole verifies the authenticated user holds the required server-side role.
func RequireRole(role string, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		p := currentPrincipal(r)
		if p.User.ID == "" {
			writeError(w, domain.Unauthorized())
			return
		}
		if p.User.Role != role {
			writeError(w, &domain.APIError{Status: 403, Code: "FORBIDDEN", Message: "You do not have access to this action."})
			return
		}
		next.ServeHTTP(w, r)
	})
}

type limitWindow struct {
	count int
	reset time.Time
}

type authLimiter struct {
	mu    sync.Mutex
	peers map[string]limitWindow
}

func (l *authLimiter) allow(peer string, now time.Time) bool {
	l.mu.Lock()
	defer l.mu.Unlock()
	window, exists := l.peers[peer]
	if !exists || !now.Before(window.reset) {
		if len(l.peers) >= 10000 {
			for key, value := range l.peers {
				if !now.Before(value.reset) {
					delete(l.peers, key)
				}
			}
			if _, exists = l.peers[peer]; !exists && len(l.peers) >= 10000 {
				return false
			}
		}
		window = limitWindow{reset: now.Add(time.Minute)}
	}
	if window.count >= 20 {
		return false
	}
	window.count++
	l.peers[peer] = window
	return true
}

func (s *Server) limit(hashesPassword bool, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		peer, _, err := net.SplitHostPort(r.RemoteAddr)
		if err != nil {
			peer = r.RemoteAddr
		}
		if !s.limiter.allow(peer, time.Now()) {
			w.Header().Set("Retry-After", "60")
			writeError(w, &domain.APIError{Status: 429, Code: "RATE_LIMITED", Message: "Too many authentication attempts. Try again later."})
			return
		}
		if hashesPassword {
			select {
			case s.hashSlots <- struct{}{}:
				defer func() { <-s.hashSlots }()
			default:
				w.Header().Set("Retry-After", "1")
				writeError(w, &domain.APIError{Status: 429, Code: "AUTH_BUSY", Message: "Authentication is busy. Try again shortly."})
				return
			}
		}
		next.ServeHTTP(w, r)
	})
}
