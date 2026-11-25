package httpapi

import (
	"errors"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
	"unicode/utf8"

	"loopy/backend/internal/domain"
)

func (s *Server) mountDonationRoutes(mux *http.ServeMux) {
	admin := func(handler http.HandlerFunc) http.Handler {
		return s.Authenticate(RequireRole("ADMIN", handler))
	}
	mux.Handle("POST /v1/donations", s.Authenticate(RequireRole("USER", http.HandlerFunc(s.createDonation))))
	mux.Handle("GET /v1/admin/summary", admin(s.donationSummary))
	mux.Handle("GET /v1/admin/donations", admin(s.listDonations))
	mux.Handle("GET /v1/admin/donations/{id}", admin(s.getDonation))
	mux.Handle("POST /v1/admin/donations/{id}/schedule", admin(s.scheduleDonation))
	mux.Handle("POST /v1/admin/donations/{id}/reject", admin(s.rejectDonation))
}

func (s *Server) createDonation(w http.ResponseWriter, r *http.Request) {
	keys := r.Header.Values("Idempotency-Key")
	if len(keys) != 1 || !domain.RequestKeyPattern.MatchString(keys[0]) {
		writeError(w, &domain.APIError{
			Status:  400,
			Code:    "INVALID_IDEMPOTENCY_KEY",
			Message: "Send one Idempotency-Key of 8–128 letters, digits, underscores or hyphens.",
		})
		return
	}
	var req domain.CreateDonationRequest
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	sub, err := domain.ValidateCreateDonation(req, currentPrincipal(r).User.ID, keys[0], time.Now())
	if err != nil {
		writeError(w, err)
		return
	}
	detail, isNew, err := s.donations.CreateDonation(r.Context(), sub)
	if err != nil {
		switch {
		case errors.Is(err, domain.ErrNotFound):
			err = &domain.APIError{Status: 404, Code: "CHECK_NOT_FOUND", Message: "Clothing check not found."}
		case errors.Is(err, domain.ErrCheckNotEligible):
			err = &domain.APIError{Status: 422, Code: "CHECK_NOT_ELIGIBLE", Message: "Only potentially eligible clothing checks can be submitted for pickup."}
		case errors.Is(err, domain.ErrDonationKeyConflict):
			err = &domain.APIError{Status: 409, Code: "IDEMPOTENCY_CONFLICT", Message: "This request key was used for a different pickup submission."}
		}
		writeError(w, err)
		return
	}
	status := http.StatusOK
	if isNew {
		w.Header().Set("Location", "/v1/donations/"+detail.ID)
		status = http.StatusCreated
	}
	writeJSON(w, status, detail)
}

func (s *Server) donationSummary(w http.ResponseWriter, r *http.Request) {
	summary, err := s.donations.DonationSummary(r.Context())
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, 200, summary)
}

func donationListFilter(q url.Values) (domain.DonationFilter, error) {
	var filter domain.DonationFilter
	for key, values := range q {
		switch key {
		case "status", "query", "limit", "cursor":
			if len(values) != 1 {
				return filter, domain.DonationValidation(map[string]string{key: "Use one value per parameter."})
			}
		default:
			return filter, &domain.APIError{Status: 400, Code: "INVALID_QUERY", Message: "Use only status, query, limit, and cursor."}
		}
	}

	filter = domain.DonationFilter{Status: q.Get("status"), Query: strings.TrimSpace(q.Get("query")), Limit: 25}
	fields := make(map[string]string)
	if filter.Status != "" && !domain.ValidDonationStatus(filter.Status) {
		fields["status"] = "Select a supported donation status."
	}
	if !utf8.ValidString(filter.Query) || utf8.RuneCountInString(filter.Query) > 100 || strings.ContainsRune(filter.Query, 0) {
		fields["query"] = "Use up to 100 valid characters without NUL."
	}
	filter.Query = strings.TrimPrefix(filter.Query, "#")
	if raw := q.Get("limit"); raw != "" {
		limit, err := strconv.Atoi(raw)
		if err != nil || limit < 1 || limit > 100 {
			fields["limit"] = "Use a number from 1 to 100."
		} else {
			filter.Limit = limit
		}
	}
	if raw := q.Get("cursor"); raw != "" {
		cursor, ok := domain.DecodeDonationCursor(raw)
		if !ok {
			fields["cursor"] = "Use the nextCursor from a previous page."
		}
		filter.After = cursor
	}
	if len(fields) != 0 {
		return filter, domain.DonationValidation(fields)
	}
	return filter, nil
}

func (s *Server) listDonations(w http.ResponseWriter, r *http.Request) {
	s.listDonationsFor(w, r, "")
}

func (s *Server) listDonationsFor(w http.ResponseWriter, r *http.Request, owner string) {
	query, err := url.ParseQuery(r.URL.RawQuery)
	if err != nil {
		writeError(w, &domain.APIError{Status: 400, Code: "INVALID_QUERY", Message: "Check query parameter encoding."})
		return
	}
	filter, err := donationListFilter(query)
	if err != nil {
		writeError(w, err)
		return
	}
	filter.OwnerID = owner
	items, err := s.donations.ListDonations(r.Context(), filter)
	if err != nil {
		writeError(w, err)
		return
	}
	response := struct {
		Items      []domain.DonationItem `json:"items"`
		NextCursor *string               `json:"nextCursor"`
	}{Items: items}
	if len(items) > filter.Limit {
		response.Items = items[:filter.Limit]
		next := domain.EncodeDonationCursor(response.Items[filter.Limit-1])
		response.NextCursor = &next
	}
	writeJSON(w, 200, response)
}

func donationID(w http.ResponseWriter, r *http.Request) (string, bool) {
	id := r.PathValue("id")
	if !domain.UUIDPattern.MatchString(id) {
		writeError(w, &domain.APIError{Status: 400, Code: "INVALID_DONATION_ID", Message: "Use a valid donation UUID."})
		return "", false
	}
	return id, true
}

func (s *Server) getDonation(w http.ResponseWriter, r *http.Request) {
	id, ok := donationID(w, r)
	if !ok {
		return
	}
	detail, err := s.donations.DonationDetail(r.Context(), id)
	writeDonationResult(w, detail, err)
}

func (s *Server) scheduleDonation(w http.ResponseWriter, r *http.Request) {
	id, ok := donationID(w, r)
	if !ok {
		return
	}
	var req domain.ScheduleDonationRequest
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	at, err := domain.ValidateSchedule(req, time.Now())
	if err != nil {
		writeError(w, err)
		return
	}
	detail, err := s.donations.ReviewDonation(r.Context(), domain.DonationDecision{
		ID:              id,
		AdminID:         currentPrincipal(r).User.ID,
		ExpectedVersion: req.ExpectedVersion,
		Status:          domain.DonationScheduled,
		ScheduledAt:     &at,
	})
	writeDonationResult(w, detail, err)
}

func (s *Server) rejectDonation(w http.ResponseWriter, r *http.Request) {
	id, ok := donationID(w, r)
	if !ok {
		return
	}
	var req domain.RejectDonationRequest
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	reason, err := domain.ValidateRejection(req)
	if err != nil {
		writeError(w, err)
		return
	}
	detail, err := s.donations.ReviewDonation(r.Context(), domain.DonationDecision{
		ID:              id,
		AdminID:         currentPrincipal(r).User.ID,
		ExpectedVersion: req.ExpectedVersion,
		Status:          domain.DonationRejected,
		RejectionReason: &reason,
	})
	writeDonationResult(w, detail, err)
}

func writeDonationResult(w http.ResponseWriter, detail domain.DonationDetail, err error) {
	switch {
	case err == nil:
		writeJSON(w, 200, detail)
		return
	case errors.Is(err, domain.ErrNotFound):
		err = &domain.APIError{Status: 404, Code: "DONATION_NOT_FOUND", Message: "Donation request not found."}
	case errors.Is(err, domain.ErrDonationChanged):
		err = &domain.APIError{Status: 409, Code: "VERSION_CONFLICT", Message: "The donation changed. Fetch its latest details."}
	case errors.Is(err, domain.ErrDonationFinal):
		err = &domain.APIError{Status: 409, Code: "INVALID_TRANSITION", Message: "The donation is no longer pending review."}
	case errors.Is(err, domain.ErrPickupInPast):
		err = domain.DonationValidation(map[string]string{"scheduledDate": "Choose a pickup date and time in the future."})
	}
	writeError(w, err)
}
