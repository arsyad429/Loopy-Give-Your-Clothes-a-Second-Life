package main

import (
	"bytes"
	"context"
	"encoding/json"
	"image"
	"image/jpeg"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"net/textproto"
	"strings"
	"sync/atomic"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"
)

type visionFunc func(context.Context, []byte, string) (visionResult, error)

func (f visionFunc) analyze(ctx context.Context, jpeg []byte, kind string) (visionResult, error) {
	return f(ctx, jpeg, kind)
}

func TestPostgresClothingCheckWithLogin(t *testing.T) {
	ctx := context.Background()
	store := integrationStore(t)
	const password = "example test passphrase"
	app := newAPI(testAuth(t, store), store)
	var calls atomic.Int32
	var fail atomic.Bool
	app.checks = &clothingCheckService{store: store, vision: visionFunc(func(_ context.Context, jpg []byte, kind string) (visionResult, error) {
		calls.Add(1)
		if fail.Load() {
			return visionResult{}, &visionFailure{}
		}
		if _, err := jpeg.Decode(bytes.NewReader(jpg)); err != nil || kind != "TSHIRT" {
			t.Errorf("expected a normalized JPEG and TSHIRT: %v", err)
		}
		return visionResult{Category: "POTENTIALLY_ELIGIBLE", Description: "Pakaian terlihat utuh."}, nil
	})}
	handler := app.routes()

	send := func(req *http.Request, token string, expected int) *httptest.ResponseRecorder {
		t.Helper()
		if token != "" {
			req.Header.Set("Authorization", "Bearer "+token)
		}
		res := httptest.NewRecorder()
		handler.ServeHTTP(res, req)
		if res.Code != expected {
			t.Fatalf("%s %s = %d, want %d. Body: %s", req.Method, req.URL.Path, res.Code, expected, res.Body)
		}
		return res
	}
	accountRequest := func(path string, payload any, status int) sessionResponse {
		t.Helper()
		body, err := json.Marshal(payload)
		if err != nil {
			t.Fatal(err)
		}
		req := httptest.NewRequest(http.MethodPost, path, bytes.NewReader(body))
		req.Header.Set("Content-Type", "application/json")
		var response sessionResponse
		if err := json.NewDecoder(send(req, "", status).Body).Decode(&response); err != nil {
			t.Fatal(err)
		}
		return response
	}
	accountRequest("/v1/auth/register", registerRequest{"Donor", "donor@example.test", password}, 201)
	donor := accountRequest("/v1/auth/login", loginRequest{"donor@example.test", password}, 200)
	other := accountRequest("/v1/auth/register", registerRequest{"Other", "other@example.test", password}, 201)
	adminAccount, err := makeAccount(registerRequest{"Admin", "admin@example.test", password})
	if err != nil {
		t.Fatal(err)
	}
	if _, err := store.CreateAdmin(ctx, adminAccount); err != nil {
		t.Fatal(err)
	}
	admin := accountRequest("/v1/auth/login", loginRequest{"admin@example.test", password}, 200)

	var fixture bytes.Buffer
	if err := jpeg.Encode(&fixture, image.NewRGBA(image.Rect(0, 0, 32, 48)), nil); err != nil {
		t.Fatal(err)
	}
	upload := func(token, key, kind string, photo []byte, status int) *httptest.ResponseRecorder {
		t.Helper()
		var body bytes.Buffer
		form := multipart.NewWriter(&body)
		if err := form.WriteField("clothingType", kind); err != nil {
			t.Fatal(err)
		}
		part, err := form.CreatePart(textproto.MIMEHeader{
			"Content-Disposition": {`form-data; name="image"; filename="test.jpg"`},
			"Content-Type":        {jpegMIME},
		})
		if err != nil {
			t.Fatal(err)
		}
		if _, err = part.Write(photo); err != nil {
			t.Fatal(err)
		}
		if err := form.Close(); err != nil {
			t.Fatal(err)
		}
		req := httptest.NewRequest(http.MethodPost, "/v1/checks", &body)
		req.Header.Set("Content-Type", form.FormDataContentType())
		req.Header.Set("Idempotency-Key", key)
		return send(req, token, status)
	}

	upload("", "none", "TSHIRT", fixture.Bytes(), 401)
	upload(admin.AccessToken, "role", "TSHIRT", fixture.Bytes(), 403)
	upload(donor.AccessToken, "invalid", "TSHIRT", []byte("not a JPEG"), 422)
	if calls.Load() != 0 {
		t.Fatal("rejected uploads must not call Groq")
	}

	created := upload(donor.AccessToken, "same-key", "TSHIRT", fixture.Bytes(), 201)
	var check clothingCheck
	if err := json.NewDecoder(created.Body).Decode(&check); err != nil {
		t.Fatal(err)
	}
	checkPath := "/v1/checks/" + check.ID
	if !uuidPattern.MatchString(check.ID) || check.UserID != donor.User.ID ||
		check.Category != "POTENTIALLY_ELIGIBLE" || check.ClothingPhoto != checkPath+"/photo" ||
		created.Header().Get("Location") != checkPath {
		t.Fatalf("unexpected check: %+v", check)
	}

	var replay clothingCheck
	if err := json.NewDecoder(upload(donor.AccessToken, "same-key", "TSHIRT", fixture.Bytes(), 200).Body).Decode(&replay); err != nil {
		t.Fatal(err)
	}
	upload(donor.AccessToken, "same-key", "SHIRT", fixture.Bytes(), 409)
	if replay.ID != check.ID || calls.Load() != 1 {
		t.Fatal("replay or conflicting key must not call Groq again")
	}
	send(httptest.NewRequest(http.MethodGet, checkPath, nil), donor.AccessToken, 200)
	send(httptest.NewRequest(http.MethodGet, checkPath, nil), admin.AccessToken, 200)
	send(httptest.NewRequest(http.MethodGet, checkPath, nil), other.AccessToken, 404)
	send(httptest.NewRequest(http.MethodGet, check.ClothingPhoto, nil), other.AccessToken, 404)
	photo := send(httptest.NewRequest(http.MethodGet, check.ClothingPhoto, nil), donor.AccessToken, 200)
	if photo.Header().Get("Content-Type") != jpegMIME || !strings.Contains(photo.Header().Get("Cache-Control"), "no-store") {
		t.Fatal("expected private JPEG response")
	}
	if _, err := jpeg.Decode(photo.Body); err != nil {
		t.Fatal(err)
	}

	fail.Store(true)
	upload(donor.AccessToken, "retry-key", "TSHIRT", fixture.Bytes(), 502)
	fail.Store(false)
	upload(donor.AccessToken, "retry-key", "TSHIRT", fixture.Bytes(), 201)

	send(httptest.NewRequest(http.MethodPost, "/v1/auth/logout", nil), donor.AccessToken, 204)
	send(httptest.NewRequest(http.MethodGet, checkPath, nil), donor.AccessToken, 401)
}

func TestVisionMigrationPreservesPickupRecords(t *testing.T) {
	ctx := context.Background()
	store := integrationStore(t)
	sess, err := testAuth(t, store).register(ctx, registerRequest{"Existing", "existing@example.test", "an example long password"})
	if err != nil {
		t.Fatal(err)
	}
	// Simulate a running Go database at version 2 with an existing session and pickup.
	if _, err := store.pool.Exec(ctx, `DROP TABLE clothing_check_requests, clothing_check_photos;
		ALTER TABLE clothing_checks DROP COLUMN action_recommendation;
		DELETE FROM schema_migrations WHERE version=3`, pgx.QueryExecModeSimpleProtocol); err != nil {
		t.Fatal(err)
	}
	id := addPendingDonation(t, store, sess.User.ID, "Malang", time.Now())
	if err := store.migrate(ctx); err != nil {
		t.Fatal(err)
	}
	p, err := store.Authenticate(ctx, tokenHash(sess.AccessToken))
	if err != nil || p.User.ID != sess.User.ID {
		t.Fatalf("existing session not preserved: %v", err)
	}
	detail, err := store.DonationDetail(ctx, id)
	if err != nil || detail.Status != donationPending || detail.ClothingCheck.PhotoKey != "private/test-shirt.jpg" {
		t.Fatalf("existing pickup not preserved: %+v, %v", detail, err)
	}
}
