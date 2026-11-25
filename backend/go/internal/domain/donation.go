package domain

import (
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"regexp"
	"strings"
	"time"
	_ "time/tzdata"
	"unicode"
	"unicode/utf8"
)

// Supported donation lifecycle statuses and Jakarta pickup scheduling configuration.
const (
	DonationPending   = "PENDING_REVIEW"
	DonationScheduled = "PICKUP_SCHEDULED"
	DonationPickedUp  = "PICKED_UP"
	DonationCompleted = "COMPLETED"
	DonationRejected  = "REJECTED"

	PickupTimezone = "Asia/Jakarta"
	pickupLayout   = "2006-01-02 15:04"
)

var (
	// UUIDPattern validates canonical UUID strings.
	UUIDPattern = regexp.MustCompile(`^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$`)
	// RequestKeyPattern validates Idempotency-Key values for donation and reward endpoints.
	RequestKeyPattern = regexp.MustCompile(`^[a-zA-Z0-9_-]{8,128}$`)
	// JakartaLocation is the fixed WIB time zone used for pickup scheduling.
	JakartaLocation = pickupLocation()
)

func pickupLocation() *time.Location {
	location, err := time.LoadLocation(PickupTimezone)
	if err != nil {
		panic(err)
	}
	return location
}

// DonationSummary aggregates counts across all donation statuses for the admin dashboard.
type DonationSummary struct {
	Total     int64 `json:"total"`
	Pending   int64 `json:"pending"`
	Scheduled int64 `json:"scheduled"`
	PickedUp  int64 `json:"pickedUp"`
	Completed int64 `json:"completed"`
	Rejected  int64 `json:"rejected"`
}

// DonationItem represents a donation row in list/queue views.
type DonationItem struct {
	ID        string    `json:"donationId"`
	DonorName string    `json:"donorName"`
	City      string    `json:"city"`
	Quantity  int       `json:"quantity"`
	Status    string    `json:"status"`
	Version   int64     `json:"version"`
	CreatedAt time.Time `json:"createdAt"`
	UpdatedAt time.Time `json:"updatedAt"`
}

// UseUTC normalizes timestamps on DonationItem to UTC.
func (d *DonationItem) UseUTC() {
	d.CreatedAt, d.UpdatedAt = d.CreatedAt.UTC(), d.UpdatedAt.UTC()
}

// ClothingReview embeds the linked AI clothing check summary inside DonationDetail.
type ClothingReview struct {
	CheckID       string `json:"checkId"`
	PhotoKey      string `json:"photoKey"`
	ClothingType  string `json:"clothingType"`
	AIResult      string `json:"aiResult"`
	AIDescription string `json:"aiDescription"`
}

// DonationHistory represents one audit trail entry in donation_status_history.
type DonationHistory struct {
	Version        int64     `json:"version"`
	PreviousStatus *string   `json:"previousStatus"`
	NewStatus      string    `json:"newStatus"`
	UpdatedBy      string    `json:"updatedBy"`
	UpdatedAt      time.Time `json:"updatedAt"`
}

// DonationDetail contains the full donation request, AI check, schedule, and audit history.
type DonationDetail struct {
	DonationItem
	UserID          string            `json:"-"`
	PhoneNumber     string            `json:"phoneNumber"`
	PickupAddress   string            `json:"pickupAddress"`
	PreferredDate   string            `json:"preferredDate"`
	PreferredTime   string            `json:"preferredTime"`
	Notes           string            `json:"notes"`
	ClothingCheck   ClothingReview    `json:"clothingCheck"`
	ScheduledAt     *time.Time        `json:"scheduledAt"`
	ScheduledDate   *string           `json:"scheduledDate"`
	ScheduledTime   *string           `json:"scheduledTime"`
	Timezone        *string           `json:"timezone"`
	RejectionReason *string           `json:"rejectionReason"`
	History         []DonationHistory `json:"history"`
}

// SetSchedule derives WIB display fields from ScheduledAt.
func (d *DonationDetail) SetSchedule() {
	if d.ScheduledAt == nil {
		return
	}
	utc := d.ScheduledAt.UTC()
	local := utc.In(JakartaLocation)
	date, clock, zone := local.Format("2006-01-02"), local.Format("15:04"), PickupTimezone
	d.ScheduledAt = &utc
	d.ScheduledDate, d.ScheduledTime, d.Timezone = &date, &clock, &zone
}

// DonationFilter specifies pagination, ownership, status, and search filters.
type DonationFilter struct {
	OwnerID string
	Status  string
	Query   string
	Limit   int
	After   DonationCursor
}

// DonationCursor encodes keyset pagination coordinates.
type DonationCursor struct {
	CreatedAt time.Time `json:"t"`
	ID        string    `json:"id"`
}

// DonationDecision holds an admin schedule or rejection command.
type DonationDecision struct {
	ID              string
	AdminID         string
	ExpectedVersion int64
	Status          string
	ScheduledAt     *time.Time
	RejectionReason *string
}

// ScheduleDonationRequest holds admin pickup schedule input.
type ScheduleDonationRequest struct {
	Date            string `json:"scheduledDate"`
	Time            string `json:"scheduledTime"`
	Timezone        string `json:"timezone"`
	ExpectedVersion int64  `json:"expectedVersion"`
}

// RejectDonationRequest holds admin rejection input.
type RejectDonationRequest struct {
	Reason          string `json:"rejectionReason"`
	ExpectedVersion int64  `json:"expectedVersion"`
}

// CreateDonationRequest holds user pickup request submission fields.
type CreateDonationRequest struct {
	CheckID               string `json:"checkId"`
	CleanAndSafeConfirmed bool   `json:"cleanAndSafeConfirmed"`
	PhoneNumber           string `json:"phoneNumber"`
	PickupAddress         string `json:"pickupAddress"`
	City                  string `json:"city"`
	Quantity              int    `json:"quantity"`
	PreferredDate         string `json:"preferredDate"`
	PreferredTime         string `json:"preferredTime"`
	Notes                 string `json:"notes"`
}

// CreateDonationSubmission is the validated, normalized pickup request command.
type CreateDonationSubmission struct {
	UserID        string
	Key           string
	RequestHash   []byte
	CheckID       string
	PhoneNumber   string
	PickupAddress string
	City          string
	Quantity      int
	PreferredDate string
	PreferredTime string
	Notes         string
}

// ValidDonationStatus reports whether status is a valid donation lifecycle state.
func ValidDonationStatus(status string) bool {
	switch status {
	case DonationPending, DonationScheduled, DonationPickedUp, DonationCompleted, DonationRejected:
		return true
	default:
		return false
	}
}

// EncodeDonationCursor serializes a DonationItem into an opaque keyset cursor.
func EncodeDonationCursor(item DonationItem) string {
	raw, _ := json.Marshal(DonationCursor{CreatedAt: item.CreatedAt, ID: item.ID})
	return base64.RawURLEncoding.EncodeToString(raw)
}

// DecodeDonationCursor parses and validates a pagination cursor token.
func DecodeDonationCursor(value string) (DonationCursor, bool) {
	var cursor DonationCursor
	if len(value) > 256 {
		return cursor, false
	}
	raw, err := base64.RawURLEncoding.DecodeString(value)
	if err != nil || json.Unmarshal(raw, &cursor) != nil {
		return cursor, false
	}
	return cursor, !cursor.CreatedAt.IsZero() && UUIDPattern.MatchString(cursor.ID)
}

// ValidateSchedule checks admin scheduling inputs and ensures the time is in the future.
func ValidateSchedule(req ScheduleDonationRequest, now time.Time) (time.Time, error) {
	fields := make(map[string]string)
	if req.Timezone != PickupTimezone {
		fields["timezone"] = "Use Asia/Jakarta."
	}
	if req.ExpectedVersion < 1 {
		fields["expectedVersion"] = "Use the positive version from the donation detail."
	}
	value := req.Date + " " + req.Time
	at, err := time.ParseInLocation(pickupLayout, value, JakartaLocation)
	switch {
	case err != nil || at.Format(pickupLayout) != value:
		fields["scheduledDate"] = "Use date YYYY-MM-DD and time HH:mm."
	case !at.After(now):
		fields["scheduledDate"] = "Choose a pickup date and time in the future."
	}
	if len(fields) > 0 {
		return time.Time{}, DonationValidation(fields)
	}
	return at.UTC(), nil
}

// ValidateRejection trims and validates an admin rejection reason.
func ValidateRejection(req RejectDonationRequest) (string, error) {
	fields := make(map[string]string)
	if req.ExpectedVersion < 1 {
		fields["expectedVersion"] = "Use the positive version from the donation detail."
	}
	reason := strings.TrimSpace(req.Reason)
	if reason == "" || len([]rune(reason)) > 500 || strings.ContainsRune(reason, 0) {
		fields["rejectionReason"] = "Enter a reason of 1–500 characters without NUL."
	}
	if len(fields) > 0 {
		return "", DonationValidation(fields)
	}
	return reason, nil
}

// ValidateCreateDonation validates and hashes a donor pickup request payload.
func ValidateCreateDonation(req CreateDonationRequest, userID, key string, now time.Time) (CreateDonationSubmission, error) {
	fields := make(map[string]string)

	checkID := strings.TrimSpace(req.CheckID)
	if !UUIDPattern.MatchString(checkID) {
		fields["checkId"] = "Use a valid clothing check UUID."
	}
	if !req.CleanAndSafeConfirmed {
		fields["cleanAndSafeConfirmed"] = "Confirm that the clothes are clean and safe to wear."
	}

	phone := strings.TrimSpace(req.PhoneNumber)
	if !validPhoneNumber(phone) {
		fields["phoneNumber"] = "Enter a valid phone number (8–20 characters)."
	}

	address := normalizeMultiline(req.PickupAddress)
	if !validText(address, 5, 300, true) {
		fields["pickupAddress"] = "Enter a pickup address of 5–300 characters."
	}

	city := strings.TrimSpace(req.City)
	if !validText(city, 2, 100, false) {
		fields["city"] = "Enter a city or area of 2–100 characters."
	}

	if req.Quantity < 1 || req.Quantity > 100 {
		fields["quantity"] = "Enter a clothing quantity between 1 and 100."
	}

	preferredDate := strings.TrimSpace(req.PreferredDate)
	if !validPreferredDate(preferredDate, now) {
		fields["preferredDate"] = "Choose today or a future date in YYYY-MM-DD format."
	}

	preferredTime := strings.TrimSpace(req.PreferredTime)
	if !validText(preferredTime, 1, 50, false) {
		fields["preferredTime"] = "Choose a preferred pickup time."
	}

	notes := normalizeMultiline(req.Notes)
	if notes != "" && !validText(notes, 1, 300, true) {
		fields["notes"] = "Notes must be at most 300 characters without control characters."
	}

	if len(fields) > 0 {
		return CreateDonationSubmission{}, DonationValidation(fields)
	}

	hash := sha256.New()
	_, _ = fmt.Fprintf(hash, "%s\x00%s\x00%s\x00%s\x00%d\x00%s\x00%s\x00%s\x00%t",
		checkID, phone, address, city, req.Quantity, preferredDate, preferredTime, notes, req.CleanAndSafeConfirmed)

	return CreateDonationSubmission{
		UserID:        userID,
		Key:           key,
		RequestHash:   hash.Sum(nil),
		CheckID:       checkID,
		PhoneNumber:   phone,
		PickupAddress: address,
		City:          city,
		Quantity:      req.Quantity,
		PreferredDate: preferredDate,
		PreferredTime: preferredTime,
		Notes:         notes,
	}, nil
}

func validPhoneNumber(value string) bool {
	if !utf8.ValidString(value) {
		return false
	}
	n := utf8.RuneCountInString(value)
	if n < 8 || n > 20 {
		return false
	}
	digits := 0
	for _, r := range value {
		switch {
		case r >= '0' && r <= '9':
			digits++
		case r == '+' || r == '-' || r == ' ' || r == '(' || r == ')':
		default:
			return false
		}
	}
	return digits >= 8
}

func normalizeMultiline(value string) string {
	return strings.TrimSpace(strings.ReplaceAll(value, "\r\n", "\n"))
}

func validText(value string, minLen, maxLen int, allowNewline bool) bool {
	if !utf8.ValidString(value) {
		return false
	}
	n := utf8.RuneCountInString(value)
	if n < minLen || n > maxLen {
		return false
	}
	return !strings.ContainsFunc(value, func(r rune) bool {
		if allowNewline && r == '\n' {
			return false
		}
		return unicode.IsControl(r)
	})
}

func validPreferredDate(value string, now time.Time) bool {
	parsed, err := time.ParseInLocation("2006-01-02", value, JakartaLocation)
	if err != nil || parsed.Format("2006-01-02") != value {
		return false
	}
	localNow := now.In(JakartaLocation)
	today := time.Date(localNow.Year(), localNow.Month(), localNow.Day(), 0, 0, 0, 0, JakartaLocation)
	return !parsed.Before(today)
}
