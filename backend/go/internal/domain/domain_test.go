package domain

import (
	"errors"
	"strings"
	"testing"
	"time"
)

func TestRegistrationPolicy(t *testing.T) {
	base := RegisterRequest{Name: " Neva ", Email: " Neva@Example.test ", Password: " a long sample passphrase "}
	normalized, err := ValidateRegistration(base)
	if err != nil || normalized.Name != "Neva" || normalized.Email != "neva@example.test" || normalized.Password != base.Password {
		t.Fatalf("normalization failed: %v", err)
	}
	for _, test := range []struct {
		name   string
		change func(*RegisterRequest)
	}{
		{"empty name", func(r *RegisterRequest) { r.Name = " " }},
		{"long name", func(r *RegisterRequest) { r.Name = strings.Repeat("a", 101) }},
		{"control name", func(r *RegisterRequest) { r.Name = "a\nb" }},
		{"display email", func(r *RegisterRequest) { r.Email = "Neva <neva@example.test>" }},
		{"invalid email", func(r *RegisterRequest) { r.Email = "missing-at" }},
		{"email newline", func(r *RegisterRequest) { r.Email = "neva@exam\nple.test" }},
		{"short password", func(r *RegisterRequest) { r.Password = strings.Repeat("a", 14) }},
		{"long password", func(r *RegisterRequest) { r.Password = strings.Repeat("a", 129) }},
	} {
		t.Run(test.name, func(t *testing.T) {
			r := base
			test.change(&r)
			if _, err := ValidateRegistration(r); err == nil {
				t.Fatal("invalid input accepted")
			}
		})
	}
	base.Name = strings.Repeat("衣", 100)
	base.Password = strings.Repeat("🧥", 128)
	if _, err := ValidateRegistration(base); err != nil {
		t.Fatalf("unicode boundary rejected: %v", err)
	}
}

func TestDonationReviewValidation(t *testing.T) {
	now := time.Date(2026, 11, 23, 5, 0, 0, 0, time.UTC)
	valid := ScheduleDonationRequest{Date: "2026-11-24", Time: "09:30", Timezone: PickupTimezone, ExpectedVersion: 1}
	at, err := ValidateSchedule(valid, now)
	if err != nil || !at.Equal(time.Date(2026, 11, 24, 2, 30, 0, 0, time.UTC)) {
		t.Fatalf("schedule = %v, %v", at, err)
	}
	for _, test := range []struct {
		name  string
		input ScheduleDonationRequest
		field string
	}{
		{"impossible date", ScheduleDonationRequest{"2026-02-30", "09:30", PickupTimezone, 1}, "scheduledDate"},
		{"noncanonical time", ScheduleDonationRequest{"2026-11-24", "9:30", PickupTimezone, 1}, "scheduledDate"},
		{"past time", ScheduleDonationRequest{"2026-11-23", "11:59", PickupTimezone, 1}, "scheduledDate"},
		{"equal time", ScheduleDonationRequest{"2026-11-23", "12:00", PickupTimezone, 1}, "scheduledDate"},
		{"unknown timezone", ScheduleDonationRequest{valid.Date, valid.Time, "UTC", 1}, "timezone"},
		{"missing version", ScheduleDonationRequest{valid.Date, valid.Time, PickupTimezone, 0}, "expectedVersion"},
	} {
		t.Run(test.name, func(t *testing.T) {
			_, err := ValidateSchedule(test.input, now)
			var apiErr *APIError
			if !errors.As(err, &apiErr) || apiErr.Status != 422 || apiErr.Fields[test.field] == "" {
				t.Fatalf("expected field %s in validation error, got %v", test.field, err)
			}
		})
	}
	if reason, err := ValidateRejection(RejectDonationRequest{"  Foto buram.  ", 1}); err != nil || reason != "Foto buram." {
		t.Fatalf("reason = %q, %v", reason, err)
	}
	for _, reason := range []string{"  ", strings.Repeat("é", 501), "invalid\x00reason"} {
		if _, err := ValidateRejection(RejectDonationRequest{reason, 1}); err == nil {
			t.Fatal("invalid reason accepted")
		}
	}
}

func TestCreateDonationValidation(t *testing.T) {
	now := time.Date(2026, 11, 25, 10, 0, 0, 0, JakartaLocation).UTC()
	valid := CreateDonationRequest{
		CheckID:               "00000000-0000-0000-0000-000000000001",
		CleanAndSafeConfirmed: true,
		PhoneNumber:           " 0812-3456-7890 ",
		PickupAddress:         " Jl. Veteran No. 12, Lowokwaru ",
		City:                  " Malang ",
		Quantity:              3,
		PreferredDate:         "2026-11-25",
		PreferredTime:         "Pagi (08.00–11.00)",
		Notes:                 " Sudah dicuci bersih ",
	}
	sub, err := ValidateCreateDonation(valid, "user-1", "req-key-1234", now)
	if err != nil {
		t.Fatalf("valid donation rejected: %v", err)
	}
	if sub.PhoneNumber != "0812-3456-7890" || sub.City != "Malang" || sub.Notes != "Sudah dicuci bersih" || len(sub.RequestHash) != 32 {
		t.Fatalf("unexpected normalized submission: %+v", sub)
	}

	for _, test := range []struct {
		name   string
		field  string
		mutate func(*CreateDonationRequest)
	}{
		{"invalid checkId", "checkId", func(r *CreateDonationRequest) { r.CheckID = "not-a-uuid" }},
		{"unconfirmed clean", "cleanAndSafeConfirmed", func(r *CreateDonationRequest) { r.CleanAndSafeConfirmed = false }},
		{"short phone", "phoneNumber", func(r *CreateDonationRequest) { r.PhoneNumber = "0812" }},
		{"alpha phone", "phoneNumber", func(r *CreateDonationRequest) { r.PhoneNumber = "0812abcdef" }},
		{"short address", "pickupAddress", func(r *CreateDonationRequest) { r.PickupAddress = "Jl. " }},
		{"short city", "city", func(r *CreateDonationRequest) { r.City = "A" }},
		{"zero quantity", "quantity", func(r *CreateDonationRequest) { r.Quantity = 0 }},
		{"over quantity", "quantity", func(r *CreateDonationRequest) { r.Quantity = 101 }},
		{"past date", "preferredDate", func(r *CreateDonationRequest) { r.PreferredDate = "2026-11-24" }},
		{"bad date format", "preferredDate", func(r *CreateDonationRequest) { r.PreferredDate = "25-11-2026" }},
		{"empty time", "preferredTime", func(r *CreateDonationRequest) { r.PreferredTime = "   " }},
		{"long notes", "notes", func(r *CreateDonationRequest) { r.Notes = strings.Repeat("a", 301) }},
	} {
		t.Run(test.name, func(t *testing.T) {
			req := valid
			test.mutate(&req)
			_, err := ValidateCreateDonation(req, "user-1", "req-key-1234", now)
			var apiErr *APIError
			if !errors.As(err, &apiErr) || apiErr.Status != 422 || apiErr.Fields[test.field] == "" {
				t.Fatalf("expected 422 field error on %s, got %v", test.field, err)
			}
		})
	}
}
