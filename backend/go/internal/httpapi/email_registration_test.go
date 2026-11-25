package httpapi

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"loopy/backend/internal/domain"
)

type capturedMail struct {
	to, code string
	count    int
	fail     bool
}

func (m *capturedMail) SendVerificationCode(_ context.Context, email, code string) error {
	m.to, m.code = email, code
	m.count++
	if m.fail {
		return errors.New("simulated email transport failure")
	}
	return nil
}

func TestPostgresEmailRegistrationAndProfile(t *testing.T) {
	pg := integrationStore(t)
	mail := &capturedMail{}
	verification, err := NewEmailRegistration(pg, mail, strings.Repeat("ab", 32))
	if err != nil {
		t.Fatal(err)
	}
	handler := NewServer(testAuth(t, pg), pg, nil).RequireEmailRegistration(verification).Routes()
	requests := 0
	call := func(method, path, bearer string, body any, status int, result any) []byte {
		t.Helper()
		data, err := json.Marshal(body)
		if err != nil {
			t.Fatal(err)
		}
		r := httptest.NewRequest(method, path, bytes.NewReader(data))
		r.Header.Set("Content-Type", "application/json")
		if bearer != "" {
			r.Header.Set("Authorization", "Bearer "+bearer)
		}
		requests++
		r.RemoteAddr = fmt.Sprintf("192.0.2.%d:1234", requests)
		w := httptest.NewRecorder()
		handler.ServeHTTP(w, r)
		if w.Code != status {
			t.Fatalf("%s %s = %d, expected %d: %s", method, path, w.Code, status, w.Body.String())
		}
		if result != nil {
			if err := json.Unmarshal(w.Body.Bytes(), result); err != nil {
				t.Fatal(err)
			}
		}
		return w.Body.Bytes()
	}

	const register = "/v1/auth/register"
	const login = "/v1/auth/login"
	const me = "/v1/auth/me"
	ctx := context.Background()
	signup := domain.RegisterRequest{Name: "Dara", Email: "dara@example.test", Password: "Password!9", Username: "Dara_User"}
	loginRequest := domain.LoginRequest{Email: signup.Email, Password: signup.Password}

	mail.fail = true
	call(http.MethodPost, register, "", signup, 503, nil)
	mail.fail = false
	mail.count = 0

	var challenge registrationChallenge
	body := call(http.MethodPost, register, "", signup, 202, &challenge)
	if !domain.UUIDPattern.MatchString(challenge.ID) || challenge.MaskedEmail != "d***@example.test" ||
		challenge.ExpiresIn != 300 || challenge.ResendAfter != 60 || mail.to != signup.Email ||
		!registrationCodePattern.MatchString(mail.code) || bytes.Contains(body, []byte("accessToken")) {
		t.Fatal("signup must create a masked challenge without a session")
	}
	firstCode := mail.code
	call(http.MethodPost, login, "", loginRequest, 401, nil)
	call(http.MethodPost, register, "", signup, 429, nil)
	call(http.MethodPost, register+"/resend", "", map[string]string{"challengeId": challenge.ID}, 429, nil)
	if mail.count != 1 {
		t.Fatal("cooldown must prevent additional delivery")
	}

	confirm := func(code string, status int, response *domain.SessionResponse) {
		t.Helper()
		input := map[string]string{"challengeId": challenge.ID, "code": code}
		if response == nil {
			call(http.MethodPost, register+"/verify", "", input, status, nil)
		} else {
			call(http.MethodPost, register+"/verify", "", input, status, response)
		}
	}
	wrongCode := "000000"
	if firstCode == wrongCode {
		wrongCode = "000001"
	}
	for attempt := 1; attempt <= 5; attempt++ {
		status := 422
		if attempt == 5 {
			status = 429
		}
		confirm(wrongCode, status, nil)
	}
	confirm(firstCode, 429, nil)
	if _, err := pg.Pool().Exec(ctx, `UPDATE email_registrations SET resend_after=now(),expires_at=now() WHERE challenge_id=$1`, challenge.ID); err != nil {
		t.Fatal(err)
	}
	confirm(firstCode, 410, nil)
	call(http.MethodPost, register+"/resend", "", map[string]string{"challengeId": challenge.ID}, 202, nil)
	if mail.count != 2 {
		t.Fatal("one fresh code should be sent")
	}
	if firstCode != mail.code {
		confirm(firstCode, 422, nil)
	}
	var registered domain.SessionResponse
	confirm(mail.code, 201, &registered)
	if !registered.User.EmailVerified || registered.User.Username != "dara_user" || registered.User.Role != "USER" {
		t.Fatalf("verified account was not created: %+v", registered.User)
	}
	confirm(mail.code, 410, nil)

	var second domain.SessionResponse
	call(http.MethodPost, login, "", loginRequest, 200, &second)
	call(http.MethodPatch, me, "", domain.ProfileUpdateRequest{Name: "Anonim"}, 401, nil)
	call(http.MethodPatch, me, registered.AccessToken, domain.ProfileUpdateRequest{Name: "Dara", Username: "bad name"}, 422, nil)
	other := domain.RegisterRequest{Name: "Existing", Email: "legacy@example.test", Password: "Other!85", Username: "taken_user"}
	if _, err := testAuth(t, pg).Register(ctx, other); err != nil {
		t.Fatal(err)
	}
	call(http.MethodPost, login, "", domain.LoginRequest{Email: other.Email, Password: other.Password}, 200, nil)
	call(http.MethodPatch, me, registered.AccessToken, domain.ProfileUpdateRequest{Name: "Dara", Username: "Taken_User"}, 409, nil)
	update := domain.ProfileUpdateRequest{Name: "Dara Baru", Username: "dara_baru", CurrentPassword: "Wrong!82", NewPassword: "Changed!83"}
	call(http.MethodPatch, me, registered.AccessToken, update, 422, nil)
	update.CurrentPassword = signup.Password
	var profile domain.User
	call(http.MethodPatch, me, registered.AccessToken, update, 200, &profile)
	if profile.Name != update.Name || profile.Username != update.Username || profile.ID != registered.User.ID ||
		profile.Role != "USER" || !profile.EmailVerified {
		t.Fatalf("invalid profile after update: %+v", profile)
	}
	call(http.MethodGet, me, registered.AccessToken, nil, 200, nil)
	call(http.MethodGet, me, second.AccessToken, nil, 401, nil)
	call(http.MethodPost, login, "", loginRequest, 401, nil)
	loginRequest.Password = update.NewPassword
	call(http.MethodPost, login, "", loginRequest, 200, nil)
}
