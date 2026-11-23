package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

func integrationStore(t *testing.T) *pgStore {
	t.Helper()
	connection := os.Getenv("TEST_DATABASE_URL")
	if connection == "" {
		t.Skip("set TEST_DATABASE_URL to run real PostgreSQL tests")
	}
	ctx := context.Background()
	admin, err := pgxpool.New(ctx, connection)
	if err != nil {
		t.Fatal(err)
	}
	id, err := newID()
	if err != nil {
		admin.Close()
		t.Fatal(err)
	}
	schema := "auth_test_" + strings.ReplaceAll(id, "-", "")
	identifier := pgx.Identifier{schema}.Sanitize()
	if _, err = admin.Exec(ctx, "CREATE SCHEMA "+identifier); err != nil {
		admin.Close()
		t.Fatal(err)
	}
	t.Cleanup(func() {
		_, err := admin.Exec(context.Background(), "DROP SCHEMA "+identifier+" CASCADE")
		admin.Close()
		if err != nil {
			t.Error(err)
		}
	})
	cfg, err := pgxpool.ParseConfig(connection)
	if err != nil {
		t.Fatal(err)
	}
	cfg.ConnConfig.RuntimeParams["search_path"] = schema
	pool, err := pgxpool.NewWithConfig(ctx, cfg)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(pool.Close)
	store := &pgStore{pool: pool}
	if err = store.migrate(ctx); err != nil {
		t.Fatal(err)
	}
	if err = store.migrate(ctx); err != nil {
		t.Fatalf("second startup migration: %v", err)
	}
	return store
}

func testAuth(t *testing.T, store repository) *authService {
	t.Helper()
	a, err := newAuthService(store)
	if err != nil {
		t.Fatal(err)
	}
	return a
}

func TestPostgresAuthFlow(t *testing.T) {
	store := integrationStore(t)
	a := testAuth(t, store)
	handler := newAPI(a, store).routes()
	var requestID atomic.Uint32
	request := func(method, path, body, token string) *httptest.ResponseRecorder {
		r := httptest.NewRequest(method, path, strings.NewReader(body))
		r.Header.Set("Content-Type", "application/json")
		if token != "" {
			r.Header.Set("Authorization", "Bearer "+token)
		}
		r.RemoteAddr = fmt.Sprintf("192.0.2.%d:1234", requestID.Add(1))
		w := httptest.NewRecorder()
		handler.ServeHTTP(w, r)
		return w
	}
	decodeSession := func(w *httptest.ResponseRecorder, status int) sessionResponse {
		t.Helper()
		if w.Code != status {
			t.Fatalf("expected %d, got %d: %s", status, w.Code, w.Body.String())
		}
		var response sessionResponse
		if err := json.Unmarshal(w.Body.Bytes(), &response); err != nil {
			t.Fatal(err)
		}
		return response
	}
	assertStatus := func(w *httptest.ResponseRecorder, status int) {
		t.Helper()
		if w.Code != status {
			t.Fatalf("expected %d, got %d: %s", status, w.Code, w.Body.String())
		}
	}
	registered := decodeSession(request("POST", "/v1/auth/register", `{"name":" Neva ","email":" Neva@Example.test ","password":"a long sample passphrase"}`, ""), 201)
	if registered.User.Role != "USER" || registered.User.TotalPoints != 0 || registered.User.Name != "Neva" || registered.User.Email != "neva@example.test" {
		t.Fatal("registration defaults/normalization failed")
	}
	if !validToken(registered.AccessToken) || !validToken(registered.RefreshToken) || registered.AccessToken == registered.RefreshToken {
		t.Fatal("invalid token pair")
	}
	var accessHash, refreshHash []byte
	if err := store.pool.QueryRow(context.Background(), "SELECT access_hash,refresh_hash FROM auth_sessions WHERE user_id=$1", registered.User.ID).Scan(&accessHash, &refreshHash); err != nil {
		t.Fatal(err)
	}
	if string(accessHash) != string(tokenHash(registered.AccessToken)) || string(refreshHash) != string(tokenHash(registered.RefreshToken)) {
		t.Fatal("raw tokens persisted or wrong hashes")
	}
	stored, err := store.FindByEmail(context.Background(), registered.User.Email)
	if err != nil || !verifyPassword("a long sample passphrase", stored.PasswordHash) {
		t.Fatal("password not hashed correctly")
	}
	assertStatus(request("POST", "/v1/auth/register", `{"name":"Duplicate","email":"NEVA@example.test","password":"a long sample passphrase"}`, ""), 409)
	wrong := request("POST", "/v1/auth/login", `{"email":"neva@example.test","password":"an incorrect long password"}`, "")
	missing := request("POST", "/v1/auth/login", `{"email":"absent@example.test","password":"an incorrect long password"}`, "")
	assertStatus(wrong, 401)
	assertStatus(missing, 401)
	if wrong.Body.String() != missing.Body.String() {
		t.Fatal("login reveals account existence")
	}
	me := request("GET", "/v1/auth/me", "", registered.AccessToken)
	assertStatus(me, 200)
	if strings.Contains(me.Body.String(), "password") || strings.Contains(me.Body.String(), "Token") {
		t.Fatal("me leaked credentials")
	}
	assertStatus(request("GET", "/v1/auth/me", "", registered.RefreshToken), 401)
	second := decodeSession(request("POST", "/v1/auth/login", `{"email":"NEVA@example.test","password":"a long sample passphrase"}`, ""), 200)
	refreshed := decodeSession(request("POST", "/v1/auth/refresh", fmt.Sprintf(`{"refreshToken":%q}`, registered.RefreshToken), ""), 200)
	if !refreshed.RefreshExpiresAt.Equal(registered.RefreshExpiresAt) {
		t.Fatal("refresh extended absolute lifetime")
	}
	assertStatus(request("GET", "/v1/auth/me", "", registered.AccessToken), 401)
	assertStatus(request("POST", "/v1/auth/refresh", fmt.Sprintf(`{"refreshToken":%q}`, registered.RefreshToken), ""), 401)
	assertStatus(request("GET", "/v1/auth/me", "", refreshed.AccessToken), 200)
	assertStatus(request("POST", "/v1/auth/logout", "", refreshed.AccessToken), 204)
	assertStatus(request("GET", "/v1/auth/me", "", refreshed.AccessToken), 401)
	assertStatus(request("POST", "/v1/auth/refresh", fmt.Sprintf(`{"refreshToken":%q}`, refreshed.RefreshToken), ""), 401)
	assertStatus(request("GET", "/v1/auth/me", "", second.AccessToken), 200)
	// A fresh server instance reads the same durable sessions and live role/points.
	handler = newAPI(testAuth(t, store), store).routes()
	if _, err := store.pool.Exec(context.Background(), "UPDATE users SET total_points=10 WHERE user_id=$1", registered.User.ID); err != nil {
		t.Fatal(err)
	}
	me = request("GET", "/v1/auth/me", "", second.AccessToken)
	assertStatus(me, 200)
	var current user
	if err := json.Unmarshal(me.Body.Bytes(), &current); err != nil {
		t.Fatal(err)
	}
	if current.ID != registered.User.ID || current.TotalPoints != 10 {
		t.Fatal("server used stale or wrong account data")
	}
	other := decodeSession(request("POST", "/v1/auth/register", `{"name":"Other","email":"other@example.test","password":"a long sample passphrase"}`, ""), 201)
	me = request("GET", "/v1/auth/me", "", other.AccessToken)
	assertStatus(me, 200)
	if strings.Contains(me.Body.String(), registered.User.ID) {
		t.Fatal("me returned another user's profile")
	}
	adminReq, err := validateRegistration(registerRequest{Name: "Admin", Email: "admin@example.test", Password: "an admin sample passphrase"})
	if err != nil {
		t.Fatal(err)
	}
	adminAccount, err := makeAccount(adminReq)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := store.CreateAdmin(context.Background(), adminAccount); err != nil {
		t.Fatal(err)
	}
	adminSession := decodeSession(request("POST", "/v1/auth/login", `{"email":"admin@example.test","password":"an admin sample passphrase"}`, ""), 200)
	if adminSession.User.Role != "ADMIN" {
		t.Fatal("admin login did not return current role")
	}
	guard := newAPI(a, store).authenticate(requireRole("ADMIN", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { w.WriteHeader(204) })))
	for token, status := range map[string]int{second.AccessToken: 403, adminSession.AccessToken: 204} {
		r := httptest.NewRequest("GET", "/admin", nil)
		r.Header.Set("Authorization", "Bearer "+token)
		w := httptest.NewRecorder()
		guard.ServeHTTP(w, r)
		assertStatus(w, status)
	}
	if _, err := store.pool.Exec(context.Background(), "UPDATE users SET role='USER' WHERE user_id=$1", adminSession.User.ID); err != nil {
		t.Fatal(err)
	}
	r := httptest.NewRequest("GET", "/admin", nil)
	r.Header.Set("Authorization", "Bearer "+adminSession.AccessToken)
	w := httptest.NewRecorder()
	guard.ServeHTTP(w, r)
	assertStatus(w, 403)
	assertStatus(request("GET", "/ready", "", ""), 200)
}

func TestPostgresAtomicityAndRaces(t *testing.T) {
	store := integrationStore(t)
	a := testAuth(t, store)
	ctx := context.Background()
	t.Run("registration rollback", func(t *testing.T) {
		account, err := makeAccount(registerRequest{Name: "Rollback", Email: "rollback@example.test", Password: "a long sample passphrase"})
		if err != nil {
			t.Fatal(err)
		}
		s, _, err := newSession(account.ID)
		if err != nil {
			t.Fatal(err)
		}
		s.RefreshHash = s.AccessHash // Forces session CHECK failure after user INSERT.
		if _, err = store.Register(ctx, account, s); err == nil {
			t.Fatal("expected forced session failure")
		}
		if _, err = store.FindByEmail(ctx, account.Email); !errors.Is(err, errNotFound) {
			t.Fatal("user insert survived rollback")
		}
	})
	t.Run("concurrent duplicate registration", func(t *testing.T) {
		var wg sync.WaitGroup
		results := make(chan error, 2)
		for i := 0; i < 2; i++ {
			wg.Add(1)
			go func() {
				defer wg.Done()
				_, err := a.register(ctx, registerRequest{Name: "Race", Email: "race@example.test", Password: "a long sample passphrase"})
				results <- err
			}()
		}
		wg.Wait()
		close(results)
		success, duplicate := 0, 0
		for err := range results {
			if err == nil {
				success++
			} else {
				var e *apiError
				if errors.As(err, &e) && e.Status == 409 {
					duplicate++
				} else {
					t.Fatal(err)
				}
			}
		}
		if success != 1 || duplicate != 1 {
			t.Fatalf("success=%d duplicate=%d", success, duplicate)
		}
		var count int
		if err := store.pool.QueryRow(ctx, "SELECT count(*) FROM auth_sessions s JOIN users u USING(user_id) WHERE u.email='race@example.test'").Scan(&count); err != nil || count != 1 {
			t.Fatalf("unexpected session count %d: %v", count, err)
		}
	})
	t.Run("concurrent refresh", func(t *testing.T) {
		original, err := a.login(ctx, loginRequest{Email: "race@example.test", Password: "a long sample passphrase"})
		if err != nil {
			t.Fatal(err)
		}
		type result struct {
			response sessionResponse
			err      error
		}
		results := make(chan result, 2)
		var wg sync.WaitGroup
		for i := 0; i < 2; i++ {
			wg.Add(1)
			go func() { defer wg.Done(); r, err := a.refresh(ctx, original.RefreshToken); results <- result{r, err} }()
		}
		wg.Wait()
		close(results)
		success, rejected := 0, 0
		for r := range results {
			if r.err == nil {
				success++
				if _, err := store.Authenticate(ctx, tokenHash(r.response.AccessToken)); err != nil {
					t.Fatal(err)
				}
			} else {
				var e *apiError
				if errors.As(r.err, &e) && e.Status == 401 {
					rejected++
				} else {
					t.Fatal(r.err)
				}
			}
		}
		if success != 1 || rejected != 1 {
			t.Fatalf("success=%d rejected=%d", success, rejected)
		}
		if _, err := store.Authenticate(ctx, tokenHash(original.AccessToken)); !errors.Is(err, errNotFound) {
			t.Fatal("old access still works")
		}
	})
}

func TestPostgresExpiry(t *testing.T) {
	store := integrationStore(t)
	a := testAuth(t, store)
	ctx := context.Background()
	r, err := a.register(ctx, registerRequest{Name: "Expiry", Email: "expiry@example.test", Password: "a long sample passphrase"})
	if err != nil {
		t.Fatal(err)
	}
	if _, err = store.pool.Exec(ctx, "UPDATE auth_sessions SET access_expires_at=now()-interval '1 second' WHERE access_hash=$1", tokenHash(r.AccessToken)); err != nil {
		t.Fatal(err)
	}
	if _, err = store.Authenticate(ctx, tokenHash(r.AccessToken)); !errors.Is(err, errNotFound) {
		t.Fatal("expired access accepted")
	}
	// Near absolute expiry, new access lifetime must be capped at refresh expiry.
	if _, err = store.pool.Exec(ctx, "UPDATE auth_sessions SET refresh_expires_at=now()+interval '1 minute' WHERE refresh_hash=$1", tokenHash(r.RefreshToken)); err != nil {
		t.Fatal(err)
	}
	rotated, err := a.refresh(ctx, r.RefreshToken)
	if err != nil {
		t.Fatal(err)
	}
	if !rotated.ExpiresAt.Equal(rotated.RefreshExpiresAt) || time.Until(rotated.ExpiresAt) > time.Minute {
		t.Fatal("access exceeded absolute expiry")
	}
	if _, err = store.pool.Exec(ctx, "UPDATE auth_sessions SET access_expires_at=now()-interval '2 seconds',refresh_expires_at=now()-interval '1 second' WHERE refresh_hash=$1", tokenHash(rotated.RefreshToken)); err != nil {
		t.Fatal(err)
	}
	if _, err = a.refresh(ctx, rotated.RefreshToken); err == nil {
		t.Fatal("expired refresh accepted")
	}
}
