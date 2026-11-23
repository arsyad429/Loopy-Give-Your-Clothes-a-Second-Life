package main

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"
)

func addPendingDonation(t *testing.T, store *pgStore, ownerID, city string, createdAt time.Time) string {
	t.Helper()
	ctx := context.Background()
	checkID, err := newID()
	if err != nil {
		t.Fatal(err)
	}
	id, err := newID()
	if err != nil {
		t.Fatal(err)
	}
	tx, err := store.pool.Begin(ctx)
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

func TestPostgresDonationReviewFlow(t *testing.T) {
	ctx := context.Background()
	store := integrationStore(t)
	auth := testAuth(t, store)
	const password = "example test passphrase"
	owner, err := auth.register(ctx, registerRequest{"Donatur Contoh", "donor@example.test", password})
	if err != nil {
		t.Fatal(err)
	}
	adminAccount, err := makeAccount(registerRequest{"Admin Contoh", "admin@example.test", password})
	if err != nil {
		t.Fatal(err)
	}
	admin, err := store.CreateAdmin(ctx, adminAccount)
	if err != nil {
		t.Fatal(err)
	}
	session, err := auth.login(ctx, loginRequest{admin.Email, password})
	if err != nil {
		t.Fatal(err)
	}

	start := time.Now().Add(-time.Hour).Truncate(time.Second)
	first := addPendingDonation(t, store, owner.User.ID, "Malang", start)
	second := addPendingDonation(t, store, owner.User.ID, "Batu", start.Add(time.Second))
	third := addPendingDonation(t, store, owner.User.ID, "Malang", start.Add(2*time.Second))
	path := "/v1/admin/donations"
	review := path + "/" + first
	handler := newAPI(auth, store).routes()
	request := func(method, path, body, token string) *httptest.ResponseRecorder {
		req := httptest.NewRequest(method, path, strings.NewReader(body))
		if token != "" {
			req.Header.Set("Authorization", "Bearer "+token)
		}
		if body != "" {
			req.Header.Set("Content-Type", "application/json")
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
	for _, route := range []struct{ method, path string }{
		{"GET", "/v1/admin/summary"}, {"GET", path}, {"GET", review},
		{"POST", review + "/schedule"}, {"POST", review + "/reject"},
	} {
		expect(request(route.method, route.path, "", ""), 401, nil)
		expect(request(route.method, route.path, "", owner.AccessToken), 403, nil)
	}

	var summary donationSummary
	expect(request("GET", "/v1/admin/summary", "", adminToken), 200, &summary)
	if summary.Total != 3 || summary.Pending != 3 {
		t.Fatalf("summary = %+v", summary)
	}
	var page struct {
		Items      []donationItem
		NextCursor *string
	}
	expect(request("GET", path+"?status=PENDING_REVIEW&query=mALANG&limit=1", "", adminToken), 200, &page)
	if len(page.Items) != 1 || page.Items[0].ID != first || page.NextCursor == nil {
		t.Fatalf("first page = %+v", page)
	}
	expect(request("GET", path+"?status=PENDING_REVIEW&query=mALANG&limit=1&cursor="+url.QueryEscape(*page.NextCursor), "", adminToken), 200, &page)
	if len(page.Items) != 1 || page.Items[0].ID != third || page.NextCursor != nil {
		t.Fatalf("second page = %+v", page)
	}
	expect(request("GET", path+"?query=%25", "", adminToken), 200, &page)
	if len(page.Items) != 0 || page.Items == nil {
		t.Fatalf("empty page = %+v, want empty array", page)
	}

	var detail donationDetail
	expect(request("GET", review, "", adminToken), 200, &detail)
	if detail.DonorName != "Donatur Contoh" || detail.Version != 1 || len(detail.History) != 1 || detail.ClothingCheck.PhotoKey == "" {
		t.Fatalf("pending detail = %+v", detail)
	}
	expect(request("GET", path+"/bad-id", "", adminToken), 400, nil)
	expect(request("GET", path+"/00000000-0000-0000-0000-000000000000", "", adminToken), 404, nil)
	expect(request("POST", review+"/reject", `{"rejectionReason":" ","expectedVersion":1}`, adminToken), 422, nil)
	expect(request("POST", review+"/schedule", `{"scheduledDate":"2000-01-01","scheduledTime":"12:00","timezone":"Asia/Jakarta","expectedVersion":1}`, adminToken), 422, nil)

	date := time.Now().In(jakarta).AddDate(0, 0, 2).Format("2006-01-02")
	schedule := fmt.Sprintf(`{"scheduledDate":%q,"scheduledTime":"09:00","timezone":"Asia/Jakarta","expectedVersion":1}`, date)
	expect(request("POST", review+"/schedule", schedule, adminToken), 200, &detail)
	wantTime, err := time.Parse(time.RFC3339, date+"T02:00:00Z")
	if err != nil || detail.Status != donationScheduled || detail.Version != 2 || detail.ScheduledAt == nil ||
		!detail.ScheduledAt.Equal(wantTime) || detail.ScheduledTime == nil || *detail.ScheduledTime != "09:00" ||
		len(detail.History) != 2 || detail.History[1].UpdatedBy != admin.ID || detail.RejectionReason != nil {
		t.Fatalf("scheduled detail = %+v", detail)
	}
	expect(request("POST", review+"/schedule", schedule, adminToken), 409, nil)
	expect(request("POST", review+"/reject", `{"rejectionReason":"Sudah dijadwalkan","expectedVersion":2}`, adminToken), 409, nil)

	expect(request("POST", path+"/"+third+"/reject", `{"rejectionReason":"  Foto buram.  ","expectedVersion":1}`, adminToken), 200, &detail)
	if detail.Status != donationRejected || detail.Version != 2 || detail.ScheduledAt != nil ||
		detail.RejectionReason == nil || *detail.RejectionReason != "Foto buram." || len(detail.History) != 2 {
		t.Fatalf("rejected detail = %+v", detail)
	}
	expect(request("GET", "/v1/admin/summary", "", adminToken), 200, &summary)
	if summary.Total != 3 || summary.Pending != 1 || summary.Scheduled != 1 || summary.Rejected != 1 {
		t.Fatalf("summary after review = %+v", summary)
	}

	var responses [2]*httptest.ResponseRecorder
	var concurrent sync.WaitGroup
	secondReview := path + "/" + second
	for i, action := range []struct{ endpoint, body string }{
		{"/schedule", schedule},
		{"/reject", `{"rejectionReason":"  Foto buram  ","expectedVersion":1}`},
	} {
		concurrent.Go(func() { responses[i] = request("POST", secondReview+action.endpoint, action.body, adminToken) })
	}
	concurrent.Wait()
	if a, b := responses[0].Code, responses[1].Code; (a != 200 || b != 409) && (a != 409 || b != 200) {
		t.Fatalf("concurrent decisions = %d/%d: %s/%s", a, b, responses[0].Body, responses[1].Body)
	}
	expect(request("GET", secondReview, "", adminToken), 200, &detail)
	if detail.Version != 2 || len(detail.History) != 2 ||
		(detail.Status == donationRejected && (detail.RejectionReason == nil || *detail.RejectionReason != "Foto buram")) {
		t.Fatalf("concurrent decision = %+v", detail)
	}

	rollbackID := addPendingDonation(t, store, owner.User.ID, "Malang", start.Add(3*time.Second))
	_, err = store.pool.Exec(ctx, `INSERT INTO donation_status_history(donation_id,version,previous_status,new_status,updated_by,updated_at)
		VALUES($1,2,'PENDING_REVIEW','REJECTED',$2,now())`, rollbackID, admin.ID)
	if err != nil {
		t.Fatal(err)
	}
	expect(request("POST", path+"/"+rollbackID+"/schedule", schedule, adminToken), 500, nil)
	persisted, err := store.DonationDetail(ctx, rollbackID)
	if err != nil || persisted.Version != 1 || persisted.Status != donationPending || persisted.ScheduledAt != nil {
		t.Fatalf("rollback detail = %+v, %v", persisted, err)
	}

	_, err = store.pool.Exec(ctx, `UPDATE users SET role='USER' WHERE user_id=$1`, admin.ID)
	if err != nil {
		t.Fatal(err)
	}
	expect(request(http.MethodGet, review, "", adminToken), 403, nil)
}

func TestPickupMigrationPreservesAuth(t *testing.T) {
	ctx := context.Background()
	store := integrationStore(t)
	auth := testAuth(t, store)
	session, err := auth.register(ctx, registerRequest{"Existing User", "existing@example.test", "a good existing password"})
	if err != nil {
		t.Fatal(err)
	}
	_, err = store.pool.Exec(ctx, `DROP TABLE donation_status_history, donation_requests, clothing_checks;
		DELETE FROM schema_migrations WHERE version=2`, pgx.QueryExecModeSimpleProtocol)
	if err != nil {
		t.Fatal(err)
	}
	for range 2 {
		if err = store.migrate(ctx); err != nil {
			t.Fatal(err)
		}
	}
	p, err := store.Authenticate(ctx, tokenHash(session.AccessToken))
	if err != nil || p.User.ID != session.User.ID {
		t.Fatalf("existing session = %+v, %v", p, err)
	}
	if summary, err := store.DonationSummary(ctx); err != nil || summary.Total != 0 {
		t.Fatalf("new schema summary = %+v, %v", summary, err)
	}
}
