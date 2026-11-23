package main

import (
	"errors"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
	"unicode/utf8"
)

func (a *api) mountDonationRoutes(mux *http.ServeMux) {
	admin := func(handler http.HandlerFunc) http.Handler {
		return a.authenticate(requireRole("ADMIN", handler))
	}
	mux.Handle("GET /v1/admin/summary", admin(a.donationSummary))
	mux.Handle("GET /v1/admin/donations", admin(a.listDonations))
	mux.Handle("GET /v1/admin/donations/{id}", admin(a.getDonation))
	mux.Handle("POST /v1/admin/donations/{id}/schedule", admin(a.scheduleDonation))
	mux.Handle("POST /v1/admin/donations/{id}/reject", admin(a.rejectDonation))
}

func (a *api) donationSummary(w http.ResponseWriter, r *http.Request) {
	summary, err := a.donations.DonationSummary(r.Context())
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, 200, summary)
}

func donationListFilter(q url.Values) (donationFilter, error) {
	var filter donationFilter
	for key, values := range q {
		switch key {
		case "status", "query", "limit", "cursor":
			if len(values) != 1 {
				return filter, donationValidation(map[string]string{key: "Use one value per parameter."})
			}
		default:
			return filter, &apiError{Status: 400, Code: "INVALID_QUERY", Message: "Use only status, query, limit, and cursor."}
		}
	}

	filter = donationFilter{Status: q.Get("status"), Query: strings.TrimSpace(q.Get("query")), Limit: 25}
	fields := make(map[string]string)
	if filter.Status != "" && !validDonationStatus(filter.Status) {
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
		cursor, ok := decodeDonationCursor(raw)
		if !ok {
			fields["cursor"] = "Use the nextCursor from a previous page."
		}
		filter.After = cursor
	}
	if len(fields) != 0 {
		return filter, donationValidation(fields)
	}
	return filter, nil
}

func (a *api) listDonations(w http.ResponseWriter, r *http.Request) {
	a.listDonationsFor(w, r, "")
}

func (a *api) listDonationsFor(w http.ResponseWriter, r *http.Request, owner string) {
	query, err := url.ParseQuery(r.URL.RawQuery)
	if err != nil {
		writeError(w, &apiError{Status: 400, Code: "INVALID_QUERY", Message: "Check query parameter encoding."})
		return
	}
	filter, err := donationListFilter(query)
	if err != nil {
		writeError(w, err)
		return
	}
	filter.OwnerID = owner
	items, err := a.donations.ListDonations(r.Context(), filter)
	if err != nil {
		writeError(w, err)
		return
	}
	response := struct {
		Items      []donationItem `json:"items"`
		NextCursor *string        `json:"nextCursor"`
	}{Items: items}
	if len(items) > filter.Limit {
		response.Items = items[:filter.Limit]
		next := encodeDonationCursor(response.Items[filter.Limit-1])
		response.NextCursor = &next
	}
	writeJSON(w, 200, response)
}

func donationID(w http.ResponseWriter, r *http.Request) (string, bool) {
	id := r.PathValue("id")
	if !donationIDPattern.MatchString(id) {
		writeError(w, &apiError{Status: 400, Code: "INVALID_DONATION_ID", Message: "Use a valid donation UUID."})
		return "", false
	}
	return id, true
}

func (a *api) getDonation(w http.ResponseWriter, r *http.Request) {
	id, ok := donationID(w, r)
	if !ok {
		return
	}
	detail, err := a.donations.DonationDetail(r.Context(), id)
	writeDonationResult(w, detail, err)
}

func (a *api) scheduleDonation(w http.ResponseWriter, r *http.Request) {
	id, ok := donationID(w, r)
	if !ok {
		return
	}
	var req scheduleDonationRequest
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	at, err := validateSchedule(req, time.Now())
	if err != nil {
		writeError(w, err)
		return
	}
	detail, err := a.donations.ReviewDonation(r.Context(), donationDecision{
		ID: id, AdminID: currentPrincipal(r).User.ID, ExpectedVersion: req.ExpectedVersion,
		Status: donationScheduled, ScheduledAt: &at,
	})
	writeDonationResult(w, detail, err)
}

func (a *api) rejectDonation(w http.ResponseWriter, r *http.Request) {
	id, ok := donationID(w, r)
	if !ok {
		return
	}
	var req rejectDonationRequest
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	reason, err := validateRejection(req)
	if err != nil {
		writeError(w, err)
		return
	}
	detail, err := a.donations.ReviewDonation(r.Context(), donationDecision{
		ID: id, AdminID: currentPrincipal(r).User.ID, ExpectedVersion: req.ExpectedVersion,
		Status: donationRejected, RejectionReason: &reason,
	})
	writeDonationResult(w, detail, err)
}

func writeDonationResult(w http.ResponseWriter, detail donationDetail, err error) {
	switch {
	case err == nil:
		writeJSON(w, 200, detail)
		return
	case errors.Is(err, errNotFound):
		err = &apiError{Status: 404, Code: "DONATION_NOT_FOUND", Message: "Donation request not found."}
	case errors.Is(err, errDonationChanged):
		err = &apiError{Status: 409, Code: "VERSION_CONFLICT", Message: "The donation changed. Fetch its latest details."}
	case errors.Is(err, errDonationFinal):
		err = &apiError{Status: 409, Code: "INVALID_TRANSITION", Message: "The donation is no longer pending review."}
	case errors.Is(err, errPickupInPast):
		err = donationValidation(map[string]string{"scheduledDate": "Choose a pickup date and time in the future."})
	}
	writeError(w, err)
}
