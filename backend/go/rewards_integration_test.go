package main

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"
	"time"
)

func TestCompletionAndRedemptionHTTP(t *testing.T) {
	ctx := context.Background()
	store := integrationStore(t)
	auth := testAuth(t, store)
	owner, err := auth.register(ctx, registerRequest{"Donatur", "completion@example.test", "test completion password"})
	if err != nil {
		t.Fatal(err)
	}
	other, err := auth.register(ctx, registerRequest{"Other", "other@example.test", "test completion password"})
	if err != nil {
		t.Fatal(err)
	}
	adminAccount, err := makeAccount(registerRequest{"Admin", "completion-admin@example.test", "test completion password"})
	if err != nil {
		t.Fatal(err)
	}
	admin, err := store.CreateAdmin(ctx, adminAccount)
	if err != nil {
		t.Fatal(err)
	}
	adminSession, err := auth.login(ctx, loginRequest{admin.Email, "test completion password"})
	if err != nil {
		t.Fatal(err)
	}
	id := addPendingDonation(t, store, owner.User.ID, "Malang", time.Now())
	handler := newAPI(auth, store).routes()
	request := func(method, path, body, token, key string) *httptest.ResponseRecorder {
		r := httptest.NewRequest(method, path, strings.NewReader(body))
		if token != "" {
			r.Header.Set("Authorization", "Bearer "+token)
		}
		if body != "" {
			r.Header.Set("Content-Type", "application/json")
		}
		if key != "" {
			r.Header.Set("Idempotency-Key", key)
		}
		w := httptest.NewRecorder()
		handler.ServeHTTP(w, r)
		return w
	}
	expect := func(w *httptest.ResponseRecorder, status int, into any) {
		t.Helper()
		if w.Code != status {
			t.Fatalf("HTTP %d, want %d: %s", w.Code, status, w.Body)
		}
		if into != nil {
			if err := json.Unmarshal(w.Body.Bytes(), into); err != nil {
				t.Fatal(err)
			}
		}
	}
	path := "/v1/admin/donations/" + id + "/status"
	expect(request("POST", path, `{"targetStatus":"COMPLETED","expectedVersion":1}`, "", ""), 401, nil)
	expect(request("POST", path, `{"targetStatus":"COMPLETED","expectedVersion":1}`, owner.AccessToken, ""), 403, nil)
	expect(request("POST", path, `{"targetStatus":"COMPLETED","expectedVersion":1}`, adminSession.AccessToken, ""), 409, nil)
	at := time.Now().Add(time.Hour)
	if _, err = store.ReviewDonation(ctx, donationDecision{ID: id, AdminID: admin.ID, ExpectedVersion: 1, Status: donationScheduled, ScheduledAt: &at}); err != nil {
		t.Fatal(err)
	}
	expect(request("POST", path, `{"targetStatus":"COMPLETED","expectedVersion":2}`, adminSession.AccessToken, ""), 409, nil)
	expect(request("POST", path, `{"targetStatus":"PICKED_UP","expectedVersion":2}`, adminSession.AccessToken, ""), 200, nil)
	var balance pointsBalance
	expect(request("GET", "/v1/points/balance", "", owner.AccessToken, ""), 200, &balance)
	if balance.Balance != 0 {
		t.Fatal("picked-up donation must not earn points")
	}
	expect(request("POST", path, `{"targetStatus":"COMPLETED","expectedVersion":3}`, adminSession.AccessToken, ""), 200, nil)
	expect(request("POST", path, `{"targetStatus":"COMPLETED","expectedVersion":3}`, adminSession.AccessToken, ""), 409, nil)
	expect(request("POST", path, `{"targetStatus":"COMPLETED","expectedVersion":4}`, adminSession.AccessToken, ""), 409, nil)
	expect(request("GET", "/v1/points/balance", "", owner.AccessToken, ""), 200, &balance)
	if balance.Balance != 10 {
		t.Fatalf("balance=%d", balance.Balance)
	}
	expect(request("GET", "/v1/donations/"+id, "", other.AccessToken, ""), 404, nil)
	var donations struct {
		Items []donationItem `json:"items"`
	}
	expect(request("GET", "/v1/donations", "", other.AccessToken, ""), 200, &donations)
	if len(donations.Items) != 0 {
		t.Fatal("another account can see the donor's donation")
	}
	expect(request("GET", "/v1/donations/"+id, "", owner.AccessToken, ""), 200, nil)
	var catalog struct {
		Items []reward `json:"items"`
	}
	expect(request("GET", "/v1/rewards", "", owner.AccessToken, ""), 200, &catalog)
	if len(catalog.Items) != 3 || catalog.Items[0].RequiredPoints != 10 {
		t.Fatalf("catalog=%+v", catalog)
	}
	body := `{"rewardId":"eco-supporter-badge"}`
	expect(request("POST", "/v1/redemptions", body, owner.AccessToken, ""), 422, nil)
	var first, replay redemptionResult
	expect(request("POST", "/v1/redemptions", body, owner.AccessToken, "request-0001"), 201, &first)
	expect(request("POST", "/v1/redemptions", body, owner.AccessToken, "request-0001"), 200, &replay)
	if first.Redemption.ID != replay.Redemption.ID || !replay.Replayed || replay.RemainingBalance != 0 {
		t.Fatalf("bad replay: %+v", replay)
	}
	expect(request("POST", "/v1/redemptions", `{"rewardId":"loopy-digital-wallpaper"}`, owner.AccessToken, "request-0001"), 409, nil)
	expect(request("POST", "/v1/redemptions", body, owner.AccessToken, "request-0002"), 409, nil)
	var history struct {
		Items []redemption `json:"items"`
	}
	expect(request("GET", "/v1/redemptions", "", owner.AccessToken, ""), 200, &history)
	if len(history.Items) != 1 || history.Items[0].PointsUsed != 10 {
		t.Fatalf("history=%+v", history)
	}
	expect(request("GET", "/v1/redemptions", "", other.AccessToken, ""), 200, &history)
	if len(history.Items) != 0 {
		t.Fatal("redemptions leaked to another user")
	}
	expect(request("GET", "/v1/points/balance", "", owner.AccessToken, ""), 200, &balance)
	if balance.Balance != 0 {
		t.Fatal("insufficient/replayed request changed balance")
	}
	var entries int
	if err = store.pool.QueryRow(ctx, `SELECT count(*) FROM point_transactions WHERE user_id=$1`, owner.User.ID).Scan(&entries); err != nil || entries != 2 {
		t.Fatalf("ledger count=%d, %v", entries, err)
	}
}

func TestRewardsConcurrencyAndRollback(t *testing.T) {
	ctx := context.Background()
	store := integrationStore(t)
	auth := testAuth(t, store)
	owner, err := auth.register(ctx, registerRequest{"Concurrent", "concurrent-rewards@example.test", "test completion password"})
	if err != nil {
		t.Fatal(err)
	}
	adminAccount, err := makeAccount(registerRequest{"Admin", "rollback-admin@example.test", "test completion password"})
	if err != nil {
		t.Fatal(err)
	}
	admin, err := store.CreateAdmin(ctx, adminAccount)
	if err != nil {
		t.Fatal(err)
	}
	id := addPendingDonation(t, store, owner.User.ID, "Malang", time.Now())
	at := time.Now().Add(time.Hour)
	if _, err = store.ReviewDonation(ctx, donationDecision{ID: id, AdminID: admin.ID, ExpectedVersion: 1, Status: donationScheduled, ScheduledAt: &at}); err != nil {
		t.Fatal(err)
	}
	if _, err = store.AdvanceDonation(ctx, id, admin.ID, 2, donationPickedUp); err != nil {
		t.Fatal(err)
	}
	// A database failure must roll back status, history, ledger and balance together.
	_, err = store.pool.Exec(ctx, `CREATE FUNCTION fail_points() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'test failure'; END $$;
		CREATE TRIGGER fail_points BEFORE INSERT ON point_transactions FOR EACH ROW EXECUTE FUNCTION fail_points()`)
	if err != nil {
		t.Fatal(err)
	}
	if _, err = store.AdvanceDonation(ctx, id, admin.ID, 3, donationCompleted); err == nil {
		t.Fatal("expected completion storage failure")
	}
	detail, err := store.DonationDetail(ctx, id)
	if err != nil || detail.Status != donationPickedUp || detail.Version != 3 || len(detail.History) != 3 {
		t.Fatalf("completion rollback=%+v,%v", detail, err)
	}
	_, err = store.pool.Exec(ctx, `DROP TRIGGER fail_points ON point_transactions`)
	if err != nil {
		t.Fatal(err)
	}
	var wg sync.WaitGroup
	errs := make(chan error, 8)
	for range 8 {
		wg.Add(1)
		go func() {
			defer wg.Done()
			_, err := store.AdvanceDonation(ctx, id, admin.ID, 3, donationCompleted)
			errs <- err
		}()
	}
	wg.Wait()
	close(errs)
	success := 0
	for err := range errs {
		if err == nil {
			success++
		}
	}
	if success != 1 {
		t.Fatalf("completion winners=%d", success)
	}
	balance, err := store.PointsBalance(ctx, owner.User.ID)
	if err != nil || balance.Balance != 10 {
		t.Fatalf("completion balance=%+v,%v", balance, err)
	}
	_, err = store.pool.Exec(ctx, `CREATE TRIGGER fail_points BEFORE INSERT ON point_transactions FOR EACH ROW EXECUTE FUNCTION fail_points()`)
	if err != nil {
		t.Fatal(err)
	}
	if _, err = store.Redeem(ctx, owner.User.ID, "eco-supporter-badge", "rollback-key"); err == nil {
		t.Fatal("expected redemption storage failure")
	}
	balance, _ = store.PointsBalance(ctx, owner.User.ID)
	history, _ := store.Redemptions(ctx, owner.User.ID)
	if balance.Balance != 10 || len(history) != 0 {
		t.Fatal("failed redemption consumed points or saved history")
	}
	_, err = store.pool.Exec(ctx, `DROP TRIGGER fail_points ON point_transactions`)
	if err != nil {
		t.Fatal(err)
	}
	// All retries with the same key return the same redemption, with one debit.
	results := make(chan redemptionResult, 8)
	errs = make(chan error, 8)
	for range 8 {
		wg.Add(1)
		go func() {
			defer wg.Done()
			result, err := store.Redeem(ctx, owner.User.ID, "eco-supporter-badge", "same-key-123")
			results <- result
			errs <- err
		}()
	}
	wg.Wait()
	close(results)
	close(errs)
	for err := range errs {
		if err != nil {
			t.Fatal(err)
		}
	}
	savedID := ""
	for result := range results {
		if savedID == "" {
			savedID = result.Redemption.ID
		}
		if savedID != result.Redemption.ID {
			t.Fatal("duplicate redemption")
		}
	}
	_, err = store.pool.Exec(ctx, `UPDATE users SET total_points=10 WHERE user_id=$1`, owner.User.ID)
	if err != nil {
		t.Fatal(err)
	}
	// Different keys still cannot spend the same ten points more than once.
	errs = make(chan error, 8)
	for n := range 8 {
		wg.Add(1)
		go func(n int) {
			defer wg.Done()
			_, err := store.Redeem(ctx, owner.User.ID, "eco-supporter-badge", fmt.Sprintf("distinct-key-%d", n))
			errs <- err
		}(n)
	}
	wg.Wait()
	close(errs)
	success = 0
	for err := range errs {
		if err == nil {
			success++
		}
	}
	if success != 1 {
		t.Fatalf("redemption winners=%d", success)
	}
	balance, err = store.PointsBalance(ctx, owner.User.ID)
	if err != nil || balance.Balance != 0 {
		t.Fatalf("remaining balance=%+v,%v", balance, err)
	}
}
