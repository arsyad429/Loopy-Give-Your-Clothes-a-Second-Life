package main

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

func TestRegistrationPolicy(t *testing.T) {
	base := registerRequest{Name: " Neva ", Email: " Neva@Example.test ", Password: " a long sample passphrase "}
	normalized, err := validateRegistration(base)
	if err != nil || normalized.Name != "Neva" || normalized.Email != "neva@example.test" || normalized.Password != base.Password {
		t.Fatalf("normalization failed: %v", err)
	}
	for _, test := range []struct {
		name   string
		change func(*registerRequest)
	}{
		{"empty name", func(r *registerRequest) { r.Name = " " }},
		{"long name", func(r *registerRequest) { r.Name = strings.Repeat("a", 101) }},
		{"control name", func(r *registerRequest) { r.Name = "a\nb" }},
		{"display email", func(r *registerRequest) { r.Email = "Neva <neva@example.test>" }},
		{"invalid email", func(r *registerRequest) { r.Email = "missing-at" }},
		{"email newline", func(r *registerRequest) { r.Email = "neva@exam\nple.test" }},
		{"short password", func(r *registerRequest) { r.Password = strings.Repeat("a", 14) }},
		{"long password", func(r *registerRequest) { r.Password = strings.Repeat("a", 129) }},
	} {
		t.Run(test.name, func(t *testing.T) {
			r := base
			test.change(&r)
			if _, err := validateRegistration(r); err == nil {
				t.Fatal("invalid input accepted")
			}
		})
	}
	base.Name = strings.Repeat("衣", 100)
	base.Password = strings.Repeat("🧥", 128)
	if _, err := validateRegistration(base); err != nil {
		t.Fatalf("unicode boundary rejected: %v", err)
	}
}

func TestPasswordHashing(t *testing.T) {
	password := "an example long password 🧥"
	first, err := hashPassword(password)
	if err != nil {
		t.Fatal(err)
	}
	second, err := hashPassword(password)
	if err != nil {
		t.Fatal(err)
	}
	if first == second || strings.Contains(first, password) {
		t.Fatal("password hashes need distinct salts")
	}
	if !verifyPassword(password, first) || verifyPassword("wrong long password", first) {
		t.Fatal("password verification failed")
	}
	for _, bad := range []string{"", "plaintext", strings.Replace(first, "m=19456", "m=999999999", 1), strings.Replace(first, "p=1", "p=0", 1), first + "$extra"} {
		if verifyPassword(password, bad) {
			t.Fatal("malformed hash accepted")
		}
	}
}

func TestHTTPInputBoundary(t *testing.T) {
	handler := newAPI(&authService{}, nil).routes()
	for _, test := range []struct {
		name, path, body, contentType string
		status                        int
	}{
		{"unknown role", "/v1/auth/register", `{"name":"Neva","email":"neva@example.test","password":"a long sample passphrase","role":"ADMIN"}`, "application/json", 400},
		{"injected points", "/v1/auth/register", `{"totalPoints":100}`, "application/json", 400},
		{"null", "/v1/auth/register", `null`, "application/json", 400},
		{"array", "/v1/auth/register", `[]`, "application/json", 400},
		{"truncated", "/v1/auth/register", `{"name":`, "application/json", 400},
		{"multiple objects", "/v1/auth/register", `{} {}`, "application/json", 400},
		{"wrong type", "/v1/auth/register", `{"name":12}`, "application/json", 400},
		{"missing fields", "/v1/auth/register", `{}`, "application/json; charset=utf-8", 422},
		{"wrong content type", "/v1/auth/login", `{}`, "text/plain", 415},
		{"oversize", "/v1/auth/register", `{"name":"` + strings.Repeat("a", 17000) + `"}`, "application/json", 413},
		{"trailing oversized whitespace", "/v1/auth/register", `{}` + strings.Repeat(" ", 17000), "application/json", 413},
		{"bad refresh", "/v1/auth/refresh", `{"refreshToken":"bad"}`, "application/json", 401},
	} {
		t.Run(test.name, func(t *testing.T) {
			r := httptest.NewRequest("POST", test.path, strings.NewReader(test.body))
			r.Header.Set("Content-Type", test.contentType)
			w := httptest.NewRecorder()
			handler.ServeHTTP(w, r)
			if w.Code != test.status {
				t.Fatalf("got %d: %s", w.Code, w.Body.String())
			}
			if w.Header().Get("Cache-Control") != "no-store" {
				t.Fatal("auth response can be cached")
			}
			var body struct {
				Error apiError `json:"error"`
			}
			if err := json.Unmarshal(w.Body.Bytes(), &body); err != nil || body.Error.Code == "" {
				t.Fatal("missing JSON error")
			}
		})
	}
}

type principalStore struct {
	repository
	p principal
}

func (s principalStore) Authenticate(context.Context, []byte) (principal, error) { return s.p, nil }

func TestBearerAndRoleGuard(t *testing.T) {
	token, err := newToken()
	if err != nil {
		t.Fatal(err)
	}
	if !validToken(token) || validToken("invalid") {
		t.Fatal("invalid token format")
	}
	for _, test := range []struct {
		name, header, role string
		status             int
	}{
		{"no token", "", "ADMIN", 401},
		{"wrong scheme", "Basic " + token, "ADMIN", 401},
		{"bad token", "Bearer garbage", "ADMIN", 401},
		{"user cannot access admin", "Bearer " + token, "USER", 403},
		{"admin", "bearer " + token, "ADMIN", 204},
	} {
		t.Run(test.name, func(t *testing.T) {
			a := newAPI(&authService{store: principalStore{p: principal{User: user{ID: "account", Role: test.role}}}}, nil)
			h := a.authenticate(requireRole("ADMIN", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { w.WriteHeader(204) })))
			r := httptest.NewRequest("GET", "/admin", nil)
			r.Header.Set("Authorization", test.header)
			w := httptest.NewRecorder()
			h.ServeHTTP(w, r)
			if w.Code != test.status {
				t.Fatalf("got %d", w.Code)
			}
		})
	}
	r := httptest.NewRequest("GET", "/", nil)
	r.Header.Add("Authorization", "Bearer "+token)
	r.Header.Add("Authorization", "Bearer "+token)
	if bearerToken(r) != "" {
		t.Fatal("ambiguous headers accepted")
	}
}

func TestAuthRateLimit(t *testing.T) {
	a := newAPI(&authService{}, nil)
	h := a.routes()
	for i := 0; i < 21; i++ {
		r := httptest.NewRequest("POST", "/v1/auth/login", strings.NewReader(`{}`))
		r.Header.Set("Content-Type", "application/json")
		r.Header.Set("X-Forwarded-For", "203.0.113."+string(rune('a'+i)))
		w := httptest.NewRecorder()
		h.ServeHTTP(w, r)
		if i < 20 && w.Code != 401 {
			t.Fatalf("unexpected response %d", w.Code)
		}
		if i == 20 && (w.Code != 429 || w.Header().Get("Retry-After") == "") {
			t.Fatal("forwarded headers bypassed limit")
		}
	}
	if !a.limiter.allow("192.0.2.1", time.Now().Add(2*time.Minute)) {
		t.Fatal("new window rejected")
	}
	for i := 0; i < cap(a.hashSlots); i++ {
		a.hashSlots <- struct{}{}
	}
	r := httptest.NewRequest("POST", "/v1/auth/register", strings.NewReader(`{}`))
	r.RemoteAddr = "192.0.2.9:1234"
	w := httptest.NewRecorder()
	h.ServeHTTP(w, r)
	if w.Code != 429 || !strings.Contains(w.Body.String(), "AUTH_BUSY") {
		t.Fatal("hash concurrency not bounded")
	}
}
