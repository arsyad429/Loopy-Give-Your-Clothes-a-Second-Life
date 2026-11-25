package httpapi

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"image"
	"image/jpeg"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"net/textproto"
	"net/url"
	"os"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"loopy/backend/internal/domain"
	"loopy/backend/internal/store"
	"loopy/backend/internal/vision"
)

func integrationStore(t *testing.T) *store.PGStore {
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
	id, err := domain.NewID()
	if err != nil {
		admin.Close()
		t.Fatal(err)
	}
	schema := "httpapi_test_" + strings.ReplaceAll(id, "-", "")
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
	pg := store.NewPGStore(pool)
	if err = pg.Migrate(ctx); err != nil {
		t.Fatal(err)
	}
	return pg
}

func testAuth(t *testing.T, pg *store.PGStore) *AuthService {
	t.Helper()
	a, err := NewAuthService(pg)
	if err != nil {
		t.Fatal(err)
	}
	return a
}

type visionFunc func(context.Context, []byte, string) (domain.VisionResult, error)

func (f visionFunc) Analyze(ctx context.Context, jpg []byte, kind string) (domain.VisionResult, error) {
	return f(ctx, jpg, kind)
}

func addPendingDonation(t *testing.T, pg *store.PGStore, ownerID, city string, createdAt time.Time) string {
	t.Helper()
	ctx := context.Background()
	checkID, err := domain.NewID()
	if err != nil {
		t.Fatal(err)
	}
	id, err := domain.NewID()
	if err != nil {
		t.Fatal(err)
	}
	tx, err := pg.Pool().Begin(ctx)
	if err != nil {
		t.Fatal(err)
	}
	defer tx.Rollback(ctx)

	_, err = tx.Exec(ctx, `INSERT INTO clothing_checks(check_id,user_id,photo_key,clothing_type,ai_result,ai_description)
		VALUES($1,$2,'private/test-shirt.jpg','Kaus','POTENTIALLY_ELIGIBLE','Tidak tampak sobekan besar.')`, checkID, ownerID)
	if err != nil {
		t.Fatal(err)
	}
	_, err = tx.Exec(ctx, `INSERT INTO donation_requests(donation_id,user_id,check_id,phone_number,pickup_address,city,quantity,
		preferred_date,preferred_time,notes,created_at,updated_at)
		VALUES($1,$2,$3,'081200000000','Jl. Contoh No. 12',$4,3,'2026-12-01','Pagi','Sudah dicuci',$5,$5)`, id, ownerID, checkID, city, createdAt)
	if err != nil {
		t.Fatal(err)
	}
	_, err = tx.Exec(ctx, `INSERT INTO donation_status_history(donation_id,version,new_status,updated_by,updated_at)
		VALUES($1,1,'PENDING_REVIEW',$2,$3)`, id, ownerID, createdAt)
	if err != nil {
		t.Fatal(err)
	}
	if err = tx.Commit(ctx); err != nil {
		t.Fatal(err)
	}
	return id
}

func TestPostgresAuthFlow(t *testing.T) {
	pg := integrationStore(t)
	a := testAuth(t, pg)
	handler := NewServer(a, pg, nil).Routes()
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
	decodeSession := func(w *httptest.ResponseRecorder, status int) domain.SessionResponse {
		t.Helper()
		if w.Code != status {
			t.Fatalf("expected %d, got %d: %s", status, w.Code, w.Body.String())
		}
		var response domain.SessionResponse
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

	assertStatus(request("POST", "/v1/auth/register", `{"name":"Neva","email":"neva@example.test","password":"Loopy!8"}`, ""), 422)
	registered := decodeSession(request("POST", "/v1/auth/register", `{"name":" Neva ","email":" Neva@Example.test ","password":"Loopy!82"}`, ""), 201)
	if registered.User.Role != "USER" || registered.User.TotalPoints != 0 || registered.User.Name != "Neva" || registered.User.Email != "neva@example.test" {
		t.Fatal("registration defaults/normalization failed")
	}
	assertStatus(request("POST", "/v1/auth/register", `{"name":"Duplicate","email":"NEVA@example.test","password":"a long sample passphrase"}`, ""), 409)
	wrong := request("POST", "/v1/auth/login", `{"email":"neva@example.test","password":"an incorrect long password"}`, "")
	missing := request("POST", "/v1/auth/login", `{"email":"absent@example.test","password":"an incorrect long password"}`, "")
	assertStatus(wrong, 401)
	assertStatus(missing, 401)
	if wrong.Body.String() != missing.Body.String() {
		t.Fatal("login reveals account existence")
	}
	assertStatus(request("GET", "/v1/auth/me", "", registered.AccessToken), 200)
	assertStatus(request("GET", "/v1/auth/me", "", registered.RefreshToken), 401)
	second := decodeSession(request("POST", "/v1/auth/login", `{"email":"NEVA@example.test","password":"Loopy!82"}`, ""), 200)
	refreshed := decodeSession(request("POST", "/v1/auth/refresh", fmt.Sprintf(`{"refreshToken":%q}`, registered.RefreshToken), ""), 200)
	assertStatus(request("GET", "/v1/auth/me", "", registered.AccessToken), 401)
	assertStatus(request("GET", "/v1/auth/me", "", refreshed.AccessToken), 200)
	assertStatus(request("POST", "/v1/auth/logout", "", refreshed.AccessToken), 204)
	assertStatus(request("GET", "/v1/auth/me", "", refreshed.AccessToken), 401)
	assertStatus(request("GET", "/v1/auth/me", "", second.AccessToken), 200)
	assertStatus(request("GET", "/ready", "", ""), 200)
}

func TestPostgresAuthAtomicityAndExpiry(t *testing.T) {
	pg := integrationStore(t)
	a := testAuth(t, pg)
	ctx := context.Background()

	t.Run("registration rollback", func(t *testing.T) {
		account, err := MakeAccount(domain.RegisterRequest{Name: "Rollback", Email: "rollback@example.test", Password: "a long sample passphrase"})
		if err != nil {
			t.Fatal(err)
		}
		s, _, err := domain.NewSession(account.ID)
		if err != nil {
			t.Fatal(err)
		}
		s.RefreshHash = s.AccessHash
		if _, err = pg.Register(ctx, account, s); err == nil {
			t.Fatal("expected forced session failure")
		}
		if _, err = pg.FindByEmail(ctx, account.Email); !errors.Is(err, domain.ErrNotFound) {
			t.Fatal("user insert survived rollback")
		}
	})

	t.Run("concurrent duplicate registration", func(t *testing.T) {
		var wg sync.WaitGroup
		results := make(chan error, 2)
		for range 2 {
			wg.Add(1)
			go func() {
				defer wg.Done()
				_, err := a.Register(ctx, domain.RegisterRequest{Name: "Race", Email: "race@example.test", Password: "a long sample passphrase"})
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
				var e *domain.APIError
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
	})
}

func TestPostgresClothingCheckWithLogin(t *testing.T) {
	ctx := context.Background()
	pg := integrationStore(t)
	const password = "example test passphrase"
	var calls atomic.Int32
	var fail atomic.Bool
	checks := NewClothingCheckService(pg, visionFunc(func(_ context.Context, jpg []byte, kind string) (domain.VisionResult, error) {
		calls.Add(1)
		if fail.Load() {
			return domain.VisionResult{}, &vision.Failure{}
		}
		if _, err := jpeg.Decode(bytes.NewReader(jpg)); err != nil || kind != "TSHIRT" {
			t.Errorf("expected a normalized JPEG and TSHIRT: %v", err)
		}
		return domain.VisionResult{Category: "POTENTIALLY_ELIGIBLE", Description: "Pakaian terlihat utuh."}, nil
	}))
	handler := NewServer(testAuth(t, pg), pg, checks).Routes()

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
	accountRequest := func(path string, payload any, status int) domain.SessionResponse {
		t.Helper()
		body, _ := json.Marshal(payload)
		req := httptest.NewRequest(http.MethodPost, path, bytes.NewReader(body))
		req.Header.Set("Content-Type", "application/json")
		var response domain.SessionResponse
		if err := json.NewDecoder(send(req, "", status).Body).Decode(&response); err != nil {
			t.Fatal(err)
		}
		return response
	}
	donor := accountRequest("/v1/auth/register", domain.RegisterRequest{Name: "Donor", Email: "donor@example.test", Password: password}, 201)
	other := accountRequest("/v1/auth/register", domain.RegisterRequest{Name: "Other", Email: "other@example.test", Password: password}, 201)
	adminAccount, _ := MakeAccount(domain.RegisterRequest{Name: "Admin", Email: "admin@example.test", Password: password})
	_, _ = pg.CreateAdmin(ctx, adminAccount)
	admin := accountRequest("/v1/auth/login", domain.LoginRequest{Email: "admin@example.test", Password: password}, 200)

	var fixture bytes.Buffer
	_ = jpeg.Encode(&fixture, image.NewRGBA(image.Rect(0, 0, 32, 48)), nil)
	upload := func(token, key, kind string, photo []byte, status int) *httptest.ResponseRecorder {
		t.Helper()
		var body bytes.Buffer
		form := multipart.NewWriter(&body)
		_ = form.WriteField("clothingType", kind)
		part, _ := form.CreatePart(textproto.MIMEHeader{
			"Content-Disposition": {`form-data; name="image"; filename="test.jpg"`},
			"Content-Type":        {vision.JPEGMIME},
		})
		_, _ = part.Write(photo)
		_ = form.Close()
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
	var check domain.ClothingCheck
	_ = json.NewDecoder(created.Body).Decode(&check)
	checkPath := "/v1/checks/" + check.ID
	if !domain.UUIDPattern.MatchString(check.ID) || check.UserID != donor.User.ID || check.Category != "POTENTIALLY_ELIGIBLE" {
		t.Fatalf("unexpected check: %+v", check)
	}

	var replay domain.ClothingCheck
	_ = json.NewDecoder(upload(donor.AccessToken, "same-key", "TSHIRT", fixture.Bytes(), 200).Body).Decode(&replay)
	upload(donor.AccessToken, "same-key", "SHIRT", fixture.Bytes(), 409)
	if replay.ID != check.ID || calls.Load() != 1 {
		t.Fatal("replay or conflicting key must not call Groq again")
	}
	send(httptest.NewRequest(http.MethodGet, checkPath, nil), donor.AccessToken, 200)
	send(httptest.NewRequest(http.MethodGet, checkPath, nil), admin.AccessToken, 200)
	send(httptest.NewRequest(http.MethodGet, checkPath, nil), other.AccessToken, 404)
	send(httptest.NewRequest(http.MethodGet, check.ClothingPhoto, nil), other.AccessToken, 404)
	fail.Store(true)
	upload(donor.AccessToken, "retry-key", "TSHIRT", fixture.Bytes(), 502)
	fail.Store(false)
	upload(donor.AccessToken, "retry-key", "TSHIRT", fixture.Bytes(), 201)
}

func TestPostgresCreateDonationSubmission(t *testing.T) {
	ctx := context.Background()
	pg := integrationStore(t)
	auth := testAuth(t, pg)
	const password = "example test passphrase"
	donor, err := auth.Register(ctx, domain.RegisterRequest{Name: "Neva", Email: "neva@example.test", Password: password})
	if err != nil {
		t.Fatal(err)
	}
	other, err := auth.Register(ctx, domain.RegisterRequest{Name: "Rio", Email: "rio@example.test", Password: password})
	if err != nil {
		t.Fatal(err)
	}
	eligibleCheck, _ := domain.NewID()
	recheckID, _ := domain.NewID()
	for _, row := range []struct{ id, owner, result string }{
		{eligibleCheck, donor.User.ID, "POTENTIALLY_ELIGIBLE"},
		{recheckID, donor.User.ID, "NEEDS_RECHECK"},
	} {
		if _, err = pg.Pool().Exec(ctx, `INSERT INTO clothing_checks(check_id,user_id,photo_key,clothing_type,ai_result,ai_description)
			VALUES($1,$2,'private/test.jpg','TSHIRT',$3,'Hasil uji.')`, row.id, row.owner, row.result); err != nil {
			t.Fatal(err)
		}
	}

	handler := NewServer(auth, pg, nil).Routes()
	submit := func(token, key, body string) *httptest.ResponseRecorder {
		req := httptest.NewRequest(http.MethodPost, "/v1/donations", strings.NewReader(body))
		req.Header.Set("Content-Type", "application/json")
		if token != "" {
			req.Header.Set("Authorization", "Bearer "+token)
		}
		if key != "" {
			req.Header.Set("Idempotency-Key", key)
		}
		res := httptest.NewRecorder()
		handler.ServeHTTP(res, req)
		return res
	}

	tomorrow := time.Now().In(domain.JakartaLocation).AddDate(0, 0, 1).Format("2006-01-02")
	validBody := fmt.Sprintf(`{"checkId":%q,"cleanAndSafeConfirmed":true,"phoneNumber":"081234567890","pickupAddress":"Jl. Veteran No. 12","city":"Malang","quantity":2,"preferredDate":%q,"preferredTime":"Pagi (08.00–11.00)","notes":"Sudah dicuci"}`, eligibleCheck, tomorrow)

	if code := submit("", "pickup-key-1", validBody).Code; code != 401 {
		t.Fatalf("unauthenticated submit = %d, want 401", code)
	}
	if code := submit(other.AccessToken, "pickup-key-1", validBody).Code; code != 404 {
		t.Fatalf("other user check submit = %d, want 404", code)
	}
	ineligibleBody := fmt.Sprintf(`{"checkId":%q,"cleanAndSafeConfirmed":true,"phoneNumber":"081234567890","pickupAddress":"Jl. Veteran No. 12","city":"Malang","quantity":2,"preferredDate":%q,"preferredTime":"Pagi (08.00–11.00)"}`, recheckID, tomorrow)
	if code := submit(donor.AccessToken, "pickup-key-2", ineligibleBody).Code; code != 422 {
		t.Fatalf("ineligible check submit = %d, want 422", code)
	}
	badQtyBody := fmt.Sprintf(`{"checkId":%q,"cleanAndSafeConfirmed":true,"phoneNumber":"081234567890","pickupAddress":"Jl. Veteran No. 12","city":"Malang","quantity":0,"preferredDate":%q,"preferredTime":"Pagi (08.00–11.00)"}`, eligibleCheck, tomorrow)
	if code := submit(donor.AccessToken, "pickup-key-3", badQtyBody).Code; code != 422 {
		t.Fatalf("quantity 0 submit = %d, want 422", code)
	}

	created := submit(donor.AccessToken, "pickup-key-valid", validBody)
	if created.Code != 201 {
		t.Fatalf("valid submit = %d: %s", created.Code, created.Body.String())
	}
	var detail domain.DonationDetail
	if err := json.Unmarshal(created.Body.Bytes(), &detail); err != nil {
		t.Fatal(err)
	}
	if detail.Status != domain.DonationPending || detail.Version != 1 || len(detail.History) != 1 || detail.Quantity != 2 {
		t.Fatalf("unexpected created donation = %+v", detail)
	}

	replayed := submit(donor.AccessToken, "pickup-key-valid", validBody)
	if replayed.Code != 200 {
		t.Fatalf("replayed submit = %d: %s", replayed.Code, replayed.Body.String())
	}
	var replayedDetail domain.DonationDetail
	if err := json.Unmarshal(replayed.Body.Bytes(), &replayedDetail); err != nil || replayedDetail.ID != detail.ID {
		t.Fatalf("replay mismatch: %+v, %v", replayedDetail, err)
	}

	differentBody := fmt.Sprintf(`{"checkId":%q,"cleanAndSafeConfirmed":true,"phoneNumber":"081234567890","pickupAddress":"Jl. Veteran No. 12","city":"Malang","quantity":5,"preferredDate":%q,"preferredTime":"Pagi (08.00–11.00)"}`, eligibleCheck, tomorrow)
	if code := submit(donor.AccessToken, "pickup-key-valid", differentBody).Code; code != 409 {
		t.Fatalf("conflict key submit = %d, want 409", code)
	}
}

func TestPostgresDonationReviewAndCompletionFlow(t *testing.T) {
	ctx := context.Background()
	pg := integrationStore(t)
	auth := testAuth(t, pg)
	const password = "example test passphrase"
	owner, err := auth.Register(ctx, domain.RegisterRequest{Name: "Donatur Contoh", Email: "donor@example.test", Password: password})
	if err != nil {
		t.Fatal(err)
	}
	other, err := auth.Register(ctx, domain.RegisterRequest{Name: "Other", Email: "other@example.test", Password: password})
	if err != nil {
		t.Fatal(err)
	}
	adminAccount, _ := MakeAccount(domain.RegisterRequest{Name: "Admin Contoh", Email: "admin@example.test", Password: password})
	admin, _ := pg.CreateAdmin(ctx, adminAccount)
	session, _ := auth.Login(ctx, domain.LoginRequest{Email: admin.Email, Password: password})

	start := time.Now().Add(-time.Hour).Truncate(time.Second)
	first := addPendingDonation(t, pg, owner.User.ID, "Malang", start)
	second := addPendingDonation(t, pg, owner.User.ID, "Batu", start.Add(time.Second))
	third := addPendingDonation(t, pg, owner.User.ID, "Malang", start.Add(2*time.Second))
	handler := NewServer(auth, pg, nil).Routes()

	request := func(method, path, body, token, key string) *httptest.ResponseRecorder {
		req := httptest.NewRequest(method, path, strings.NewReader(body))
		if token != "" {
			req.Header.Set("Authorization", "Bearer "+token)
		}
		if body != "" {
			req.Header.Set("Content-Type", "application/json")
		}
		if key != "" {
			req.Header.Set("Idempotency-Key", key)
		}
		res := httptest.NewRecorder()
		handler.ServeHTTP(res, req)
		return res
	}
	expect := func(res *httptest.ResponseRecorder, status int, into any) {
		t.Helper()
		if res.Code != status {
			t.Fatalf("response = %d, want %d: %s", res.Code, status, res.Body)
		}
		if into != nil {
			if err := json.Unmarshal(res.Body.Bytes(), into); err != nil {
				t.Fatal(err)
			}
		}
	}

	adminToken := session.AccessToken
	path := "/v1/admin/donations"
	review := path + "/" + first
	expect(request("GET", "/v1/admin/summary", "", "", ""), 401, nil)
	expect(request("GET", "/v1/admin/summary", "", owner.AccessToken, ""), 403, nil)

	var page struct {
		Items      []domain.DonationItem
		NextCursor *string
	}
	expect(request("GET", path+"?status=PENDING_REVIEW&query=mALANG&limit=1", "", adminToken, ""), 200, &page)
	if len(page.Items) != 1 || page.Items[0].ID != first || page.NextCursor == nil {
		t.Fatalf("first page = %+v", page)
	}
	expect(request("GET", path+"?status=PENDING_REVIEW&query=mALANG&limit=1&cursor="+url.QueryEscape(*page.NextCursor), "", adminToken, ""), 200, &page)
	if len(page.Items) != 1 || page.Items[0].ID != third || page.NextCursor != nil {
		t.Fatalf("second page = %+v", page)
	}

	date := time.Now().In(domain.JakartaLocation).AddDate(0, 0, 2).Format("2006-01-02")
	schedule := fmt.Sprintf(`{"scheduledDate":%q,"scheduledTime":"09:00","timezone":"Asia/Jakarta","expectedVersion":1}`, date)
	var detail domain.DonationDetail
	expect(request("POST", review+"/schedule", schedule, adminToken, ""), 200, &detail)
	expect(request("POST", review+"/status", `{"targetStatus":"PICKED_UP","expectedVersion":2}`, adminToken, ""), 200, &detail)
	expect(request("POST", review+"/status", `{"targetStatus":"COMPLETED","expectedVersion":3}`, adminToken, ""), 200, &detail)

	latestPath := "/v1/donations/latest"
	expect(request("GET", latestPath, "", "", ""), 401, nil)
	expect(request("GET", latestPath, "", adminToken, ""), 403, nil)
	var latest struct{ Item *domain.DonationItem }
	expect(request("GET", latestPath, "", owner.AccessToken, ""), 200, &latest)
	if latest.Item == nil || latest.Item.ID != third {
		t.Fatalf("latest donation: %+v", latest.Item)
	}
	expect(request("GET", latestPath, "", other.AccessToken, ""), 200, &latest)
	if latest.Item != nil {
		t.Fatal("another donor must not see the owner's latest donation")
	}

	var balance domain.PointsBalance
	expect(request("GET", "/v1/points/balance", "", owner.AccessToken, ""), 200, &balance)
	if balance.Balance != 10 {
		t.Fatalf("balance=%d", balance.Balance)
	}
	expect(request("GET", "/v1/donations/"+first, "", other.AccessToken, ""), 404, nil)
	expect(request("GET", "/v1/donations/"+first, "", owner.AccessToken, ""), 200, nil)

	var redeemResult, replay domain.RedemptionResult
	expect(request("POST", "/v1/redemptions", `{"rewardId":"eco-supporter-badge"}`, owner.AccessToken, "request-0001"), 201, &redeemResult)
	expect(request("POST", "/v1/redemptions", `{"rewardId":"eco-supporter-badge"}`, owner.AccessToken, "request-0001"), 200, &replay)
	if redeemResult.Redemption.ID != replay.Redemption.ID || !replay.Replayed || replay.RemainingBalance != 0 {
		t.Fatalf("bad replay: %+v", replay)
	}
	expect(request("POST", "/v1/redemptions", `{"rewardId":"eco-supporter-badge"}`, owner.AccessToken, "request-0002"), 409, nil)
	_ = second
}
