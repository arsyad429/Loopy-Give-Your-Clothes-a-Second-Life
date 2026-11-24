package main

import (
	"errors"
	"net/http"
	"regexp"
)

var requestKeyPattern = regexp.MustCompile(`^[a-zA-Z0-9_-]{8,128}$`)

func (a *api) mountRewardRoutes(mux *http.ServeMux) {
	userRoute := func(h http.HandlerFunc) http.Handler { return a.authenticate(requireRole("USER", h)) }
	mux.Handle("POST /v1/admin/donations/{id}/status", a.authenticate(requireRole("ADMIN", http.HandlerFunc(a.advanceDonation))))
	mux.Handle("GET /v1/donations", userRoute(func(w http.ResponseWriter, r *http.Request) { a.listDonationsFor(w, r, currentPrincipal(r).User.ID) }))
	mux.Handle("GET /v1/donations/{id}", userRoute(a.ownDonation))
	mux.Handle("GET /v1/points/balance", userRoute(func(w http.ResponseWriter, r *http.Request) {
		result, err := a.rewards.PointsBalance(r.Context(), currentPrincipal(r).User.ID)
		if err != nil {
			writeError(w, err)
			return
		}
		writeJSON(w, 200, result)
	}))
	mux.Handle("GET /v1/rewards", userRoute(func(w http.ResponseWriter, r *http.Request) {
		items, err := a.rewards.Rewards(r.Context())
		if err != nil {
			writeError(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"items": items})
	}))
	mux.Handle("GET /v1/redemptions", userRoute(func(w http.ResponseWriter, r *http.Request) {
		items, err := a.rewards.Redemptions(r.Context(), currentPrincipal(r).User.ID)
		if err != nil {
			writeError(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"items": items})
	}))
	mux.Handle("POST /v1/redemptions", userRoute(a.redeemReward))
}

func (a *api) ownDonation(w http.ResponseWriter, r *http.Request) {
	id, ok := donationID(w, r)
	if !ok {
		return
	}
	detail, err := a.donations.DonationDetail(r.Context(), id)
	if err == nil && detail.UserID != currentPrincipal(r).User.ID {
		err = errNotFound
	}
	writeDonationResult(w, detail, err)
}

func (a *api) advanceDonation(w http.ResponseWriter, r *http.Request) {
	id, ok := donationID(w, r)
	if !ok {
		return
	}
	var req struct {
		TargetStatus    string `json:"targetStatus"`
		ExpectedVersion int64  `json:"expectedVersion"`
	}
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	if req.ExpectedVersion < 1 || (req.TargetStatus != donationPickedUp && req.TargetStatus != donationCompleted) {
		writeError(w, donationValidation(map[string]string{"targetStatus": "Use PICKED_UP or COMPLETED with the latest positive expectedVersion."}))
		return
	}
	detail, err := a.rewards.AdvanceDonation(r.Context(), id, currentPrincipal(r).User.ID, req.ExpectedVersion, req.TargetStatus)
	writeDonationResult(w, detail, err)
}

func (a *api) redeemReward(w http.ResponseWriter, r *http.Request) {
	var req struct {
		RewardID string `json:"rewardId"`
	}
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	key := r.Header.Get("Idempotency-Key")
	if !requestKeyPattern.MatchString(key) || len(r.Header.Values("Idempotency-Key")) != 1 || req.RewardID == "" || len(req.RewardID) > 100 {
		writeError(w, donationValidation(map[string]string{"request": "Send rewardId and one Idempotency-Key of 8–128 letters, digits, underscores or hyphens."}))
		return
	}
	result, err := a.rewards.Redeem(r.Context(), currentPrincipal(r).User.ID, req.RewardID, key)
	if err != nil {
		if errors.Is(err, errNotFound) {
			err = unauthorized()
		}
		writeError(w, err)
		return
	}
	status := http.StatusCreated
	if result.Replayed {
		status = http.StatusOK
	}
	writeJSON(w, status, result)
}
