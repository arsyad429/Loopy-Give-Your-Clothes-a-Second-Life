package main

import (
	"errors"
	"net/url"
	"strings"
	"testing"
	"time"
)

func TestDonationReviewValidation(t *testing.T) {
	now := time.Date(2026, 11, 23, 5, 0, 0, 0, time.UTC)
	valid := scheduleDonationRequest{Date: "2026-11-24", Time: "09:30", Timezone: pickupTimezone, ExpectedVersion: 1}
	at, err := validateSchedule(valid, now)
	if err != nil || !at.Equal(time.Date(2026, 11, 24, 2, 30, 0, 0, time.UTC)) {
		t.Fatalf("schedule = %v, %v", at, err)
	}
	for _, test := range []struct {
		name  string
		input scheduleDonationRequest
		field string
	}{
		{"impossible date", scheduleDonationRequest{"2026-02-30", "09:30", pickupTimezone, 1}, "scheduledDate"},
		{"noncanonical time", scheduleDonationRequest{"2026-11-24", "9:30", pickupTimezone, 1}, "scheduledDate"},
		{"past time", scheduleDonationRequest{"2026-11-23", "11:59", pickupTimezone, 1}, "scheduledDate"},
		{"equal time", scheduleDonationRequest{"2026-11-23", "12:00", pickupTimezone, 1}, "scheduledDate"},
		{"unknown timezone", scheduleDonationRequest{valid.Date, valid.Time, "UTC", 1}, "timezone"},
		{"missing version", scheduleDonationRequest{valid.Date, valid.Time, pickupTimezone, 0}, "expectedVersion"},
	} {
		t.Run(test.name, func(t *testing.T) {
			_, err := validateSchedule(test.input, now)
			var apiErr *apiError
			if !errors.As(err, &apiErr) || apiErr.Status != 422 || apiErr.Fields[test.field] == "" {
				t.Fatalf("expected field %s in validation error, got %v", test.field, err)
			}
		})
	}
	if reason, err := validateRejection(rejectDonationRequest{"  Foto buram.  ", 1}); err != nil || reason != "Foto buram." {
		t.Fatalf("reason = %q, %v", reason, err)
	}
	for _, reason := range []string{"  ", strings.Repeat("é", 501), "invalid\x00reason"} {
		if _, err := validateRejection(rejectDonationRequest{reason, 1}); err == nil {
			t.Fatal("invalid reason accepted")
		}
	}
}

func TestDonationListFilter(t *testing.T) {
	item := donationItem{ID: "00000000-0000-0000-0000-000000000001", CreatedAt: time.Now().UTC()}
	cursor := encodeDonationCursor(item)
	filter, err := donationListFilter(url.Values{
		"status": {donationPending}, "query": {" #AB "}, "limit": {"2"}, "cursor": {cursor},
	})
	if err != nil || filter.Query != "AB" || filter.Limit != 2 || filter.Status != donationPending ||
		filter.After.ID != item.ID || !filter.After.CreatedAt.Equal(item.CreatedAt) {
		t.Fatalf("filter = %+v, %v", filter, err)
	}
	for _, invalid := range []url.Values{
		{"cursor": {strings.Repeat("x", 257)}},
		{"status": {"PENDING"}},
		{"limit": {"101"}},
		{"query": {"first", "second"}},
		{"unknown": {"parameter"}},
	} {
		if _, err := donationListFilter(invalid); err == nil {
			t.Fatalf("accepted query: %v", invalid)
		}
	}
}
