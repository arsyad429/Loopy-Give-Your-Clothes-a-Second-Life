package main

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
)

type api struct {
	auth      *authService
	donations donationRepository
	rewards   rewardsRepository
	limiter   *authLimiter
	hashSlots chan struct{}
}

func newAPI(auth *authService, donations donationRepository) *api {
	rewards, _ := donations.(rewardsRepository)
	return &api{
		auth: auth, donations: donations, rewards: rewards,
		limiter: &authLimiter{peers: make(map[string]limitWindow)}, hashSlots: make(chan struct{}, 4),
	}
}

func (a *api) routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]string{"status": "ok", "mode": "go-auth"})
	})
	mux.HandleFunc("GET /ready", func(w http.ResponseWriter, r *http.Request) {
		ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
		defer cancel()
		if err := a.auth.store.Ping(ctx); err != nil {
			writeError(w, &apiError{Status: 503, Code: "NOT_READY", Message: "Database unavailable."})
			return
		}
		writeJSON(w, 200, map[string]string{"status": "ready"})
	})
	mux.Handle("POST /v1/auth/register", a.limit(true, http.HandlerFunc(a.register)))
	mux.Handle("POST /v1/auth/login", a.limit(true, http.HandlerFunc(a.login)))
	mux.Handle("POST /v1/auth/refresh", a.limit(false, http.HandlerFunc(a.refresh)))
	mux.Handle("GET /v1/auth/me", a.authenticate(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, currentPrincipal(r).User)
	})))
	mux.Handle("POST /v1/auth/logout", a.authenticate(http.HandlerFunc(a.logout)))
	if a.donations != nil {
		a.mountDonationRoutes(mux)
	}
	if a.rewards != nil {
		a.mountRewardRoutes(mux)
	}
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		w.Header().Set("X-Content-Type-Options", "nosniff")
		ctx, cancel := context.WithTimeout(r.Context(), 10*time.Second)
		defer cancel()
		mux.ServeHTTP(w, r.WithContext(ctx))
	})
}

func decodeJSON(w http.ResponseWriter, r *http.Request, dst any) error {
	mediaType, _, err := mime.ParseMediaType(r.Header.Get("Content-Type"))
	if err != nil || mediaType != "application/json" {
		return &apiError{Status: 415, Code: "UNSUPPORTED_MEDIA_TYPE", Message: "Use Content-Type: application/json."}
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
		return &apiError{Status: 413, Code: "BODY_TOO_LARGE", Message: "Request body must be at most 16 KiB."}
	}
	if err == nil && len(raw) > 0 && raw[0] == '{' {
		strict := json.NewDecoder(strings.NewReader(string(raw)))
		strict.DisallowUnknownFields()
		err = strict.Decode(dst)
		if err == nil {
			return nil
		}
	}
	return &apiError{Status: 400, Code: "INVALID_JSON", Message: "Send one JSON object with only the documented fields."}
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(body)
}

func writeError(w http.ResponseWriter, err error) {
	var apiErr *apiError
	if !errors.As(err, &apiErr) {
		// Avoid logging SQL errors, which can include email addresses and hashes.
		slog.Error("request failed", "errorType", fmt.Sprintf("%T", err))
		apiErr = &apiError{Status: 500, Code: "INTERNAL_ERROR", Message: "The request could not be completed."}
	}
	writeJSON(w, apiErr.Status, map[string]any{"error": apiErr})
}

func (a *api) register(w http.ResponseWriter, r *http.Request) {
	var req registerRequest
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	response, err := a.auth.register(r.Context(), req)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, 201, response)
}

func (a *api) login(w http.ResponseWriter, r *http.Request) {
	var req loginRequest
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	response, err := a.auth.login(r.Context(), req)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, 200, response)
}

func (a *api) refresh(w http.ResponseWriter, r *http.Request) {
	var req struct {
		RefreshToken string `json:"refreshToken"`
	}
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	response, err := a.auth.refresh(r.Context(), req.RefreshToken)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, 200, response)
}

func (a *api) logout(w http.ResponseWriter, r *http.Request) {
	err := a.auth.store.Revoke(r.Context(), tokenHash(bearerToken(r)))
	if errors.Is(err, errNotFound) {
		err = unauthorized()
	}
	if err != nil {
		writeError(w, err)
		return
	}
	w.WriteHeader(204)
}

type principalKey struct{}

func currentPrincipal(r *http.Request) principal {
	p, _ := r.Context().Value(principalKey{}).(principal)
	return p
}

func bearerToken(r *http.Request) string {
	if len(r.Header.Values("Authorization")) != 1 {
		return ""
	}
	parts := strings.Fields(r.Header.Get("Authorization"))
	if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") || !validToken(parts[1]) {
		return ""
	}
	return parts[1]
}

func (a *api) authenticate(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		token := bearerToken(r)
		if token == "" {
			w.Header().Set("WWW-Authenticate", "Bearer")
			writeError(w, unauthorized())
			return
		}
		p, err := a.auth.store.Authenticate(r.Context(), tokenHash(token))
		if errors.Is(err, errNotFound) {
			w.Header().Set("WWW-Authenticate", "Bearer")
			err = unauthorized()
		}
		if err != nil {
			writeError(w, err)
			return
		}
		next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), principalKey{}, p)))
	})
}

// Mount role-sensitive routes inside authenticate; never use a client role.
func requireRole(role string, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		p := currentPrincipal(r)
		if p.User.ID == "" {
			writeError(w, unauthorized())
			return
		}
		if p.User.Role != role {
			writeError(w, &apiError{Status: 403, Code: "FORBIDDEN", Message: "You do not have access to this action."})
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

func (a *api) limit(hashesPassword bool, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		peer, _, err := net.SplitHostPort(r.RemoteAddr)
		if err != nil {
			peer = r.RemoteAddr
		}
		if !a.limiter.allow(peer, time.Now()) {
			w.Header().Set("Retry-After", "60")
			writeError(w, &apiError{Status: 429, Code: "RATE_LIMITED", Message: "Too many authentication attempts. Try again later."})
			return
		}
		if hashesPassword {
			select {
			case a.hashSlots <- struct{}{}:
				defer func() { <-a.hashSlots }()
			default:
				w.Header().Set("Retry-After", "1")
				writeError(w, &apiError{Status: 429, Code: "AUTH_BUSY", Message: "Authentication is busy. Try again shortly."})
				return
			}
		}
		next.ServeHTTP(w, r)
	})
}
