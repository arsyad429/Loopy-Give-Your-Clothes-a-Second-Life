package main

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"errors"
	"regexp"
	"strings"
	"time"
	_ "time/tzdata"
)

const (
	donationPending   = "PENDING_REVIEW"
	donationScheduled = "PICKUP_SCHEDULED"
	donationPickedUp  = "PICKED_UP"
	donationCompleted = "COMPLETED"
	donationRejected  = "REJECTED"

	pickupTimezone = "Asia/Jakarta"
	pickupLayout   = "2006-01-02 15:04"
)

var (
	donationIDPattern  = regexp.MustCompile(`^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$`)
	errDonationChanged = errors.New("donation version changed")
	errDonationFinal   = errors.New("donation is no longer pending review")
	errPickupInPast    = errors.New("pickup time is not in the future")
)

type donationRepository interface {
	DonationSummary(context.Context) (donationSummary, error)
	ListDonations(context.Context, donationFilter) ([]donationItem, error)
	DonationDetail(context.Context, string) (donationDetail, error)
	ReviewDonation(context.Context, donationDecision) (donationDetail, error)
}

type donationSummary struct {
	Total     int64 `json:"total"`
	Pending   int64 `json:"pending"`
	Scheduled int64 `json:"scheduled"`
	PickedUp  int64 `json:"pickedUp"`
	Completed int64 `json:"completed"`
	Rejected  int64 `json:"rejected"`
}

type donationItem struct {
	ID        string    `json:"donationId"`
	DonorName string    `json:"donorName"`
	City      string    `json:"city"`
	Quantity  int       `json:"quantity"`
	Status    string    `json:"status"`
	Version   int64     `json:"version"`
	CreatedAt time.Time `json:"createdAt"`
	UpdatedAt time.Time `json:"updatedAt"`
}

type clothingReview struct {
	CheckID       string `json:"checkId"`
	PhotoKey      string `json:"photoKey"`
	ClothingType  string `json:"clothingType"`
	AIResult      string `json:"aiResult"`
	AIDescription string `json:"aiDescription"`
}

type donationHistory struct {
	Version        int64     `json:"version"`
	PreviousStatus *string   `json:"previousStatus"`
	NewStatus      string    `json:"newStatus"`
	UpdatedBy      string    `json:"updatedBy"`
	UpdatedAt      time.Time `json:"updatedAt"`
}

type donationDetail struct {
	donationItem
	UserID          string            `json:"-"`
	PhoneNumber     string            `json:"phoneNumber"`
	PickupAddress   string            `json:"pickupAddress"`
	PreferredDate   string            `json:"preferredDate"`
	PreferredTime   string            `json:"preferredTime"`
	Notes           string            `json:"notes"`
	ClothingCheck   clothingReview    `json:"clothingCheck"`
	ScheduledAt     *time.Time        `json:"scheduledAt"`
	ScheduledDate   *string           `json:"scheduledDate"`
	ScheduledTime   *string           `json:"scheduledTime"`
	Timezone        *string           `json:"timezone"`
	RejectionReason *string           `json:"rejectionReason"`
	History         []donationHistory `json:"history"`
}

type donationFilter struct {
	OwnerID string
	Status  string
	Query   string
	Limit   int
	After   donationCursor
}

type donationCursor struct {
	CreatedAt time.Time `json:"t"`
	ID        string    `json:"id"`
}

type donationDecision struct {
	ID              string
	AdminID         string
	ExpectedVersion int64
	Status          string
	ScheduledAt     *time.Time
	RejectionReason *string
}

type scheduleDonationRequest struct {
	Date            string `json:"scheduledDate"`
	Time            string `json:"scheduledTime"`
	Timezone        string `json:"timezone"`
	ExpectedVersion int64  `json:"expectedVersion"`
}

type rejectDonationRequest struct {
	Reason          string `json:"rejectionReason"`
	ExpectedVersion int64  `json:"expectedVersion"`
}

func validDonationStatus(status string) bool {
	switch status {
	case donationPending, donationScheduled, donationPickedUp, donationCompleted, donationRejected:
		return true
	default:
		return false
	}
}

func encodeDonationCursor(item donationItem) string {
	raw, _ := json.Marshal(donationCursor{CreatedAt: item.CreatedAt, ID: item.ID})
	return base64.RawURLEncoding.EncodeToString(raw)
}

func decodeDonationCursor(value string) (donationCursor, bool) {
	var cursor donationCursor
	if len(value) > 256 {
		return cursor, false
	}
	raw, err := base64.RawURLEncoding.DecodeString(value)
	if err != nil || json.Unmarshal(raw, &cursor) != nil {
		return cursor, false
	}
	return cursor, !cursor.CreatedAt.IsZero() && donationIDPattern.MatchString(cursor.ID)
}

func pickupLocation() *time.Location {
	location, err := time.LoadLocation(pickupTimezone)
	if err != nil {
		panic(err)
	}
	return location
}

var jakarta = pickupLocation()

func validateSchedule(req scheduleDonationRequest, now time.Time) (time.Time, error) {
	fields := make(map[string]string)
	if req.Timezone != pickupTimezone {
		fields["timezone"] = "Use Asia/Jakarta."
	}
	if req.ExpectedVersion < 1 {
		fields["expectedVersion"] = "Use the positive version from the donation detail."
	}
	value := req.Date + " " + req.Time
	at, err := time.ParseInLocation(pickupLayout, value, jakarta)
	switch {
	case err != nil || at.Format(pickupLayout) != value:
		fields["scheduledDate"] = "Use date YYYY-MM-DD and time HH:mm."
	case !at.After(now):
		fields["scheduledDate"] = "Choose a pickup date and time in the future."
	}
	if len(fields) > 0 {
		return time.Time{}, donationValidation(fields)
	}
	return at.UTC(), nil
}

func validateRejection(req rejectDonationRequest) (string, error) {
	fields := make(map[string]string)
	if req.ExpectedVersion < 1 {
		fields["expectedVersion"] = "Use the positive version from the donation detail."
	}
	reason := strings.TrimSpace(req.Reason)
	if reason == "" || len([]rune(reason)) > 500 || strings.ContainsRune(reason, 0) {
		fields["rejectionReason"] = "Enter a reason of 1–500 characters without NUL."
	}
	if len(fields) > 0 {
		return "", donationValidation(fields)
	}
	return reason, nil
}

func donationValidation(fields map[string]string) *apiError {
	return &apiError{Status: 422, Code: "VALIDATION_ERROR", Message: "Check the submitted fields.", Fields: fields}
}

func (d *donationDetail) setSchedule() {
	if d.ScheduledAt == nil {
		return
	}
	utc := d.ScheduledAt.UTC()
	local := utc.In(jakarta)
	date, clock, zone := local.Format("2006-01-02"), local.Format("15:04"), pickupTimezone
	d.ScheduledAt = &utc
	d.ScheduledDate, d.ScheduledTime, d.Timezone = &date, &clock, &zone
}
