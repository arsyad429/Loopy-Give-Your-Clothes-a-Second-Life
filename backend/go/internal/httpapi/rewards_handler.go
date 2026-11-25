package httpapi

import (
	"errors"
	"net/http"

	"loopy/backend/internal/domain"
)

func (s *Server) mountRewardRoutes(mux *http.ServeMux) {
	userRoute := func(h http.HandlerFunc) http.Handler {
		return s.Authenticate(RequireRole("USER", h))
	}
	mux.Handle("POST /v1/admin/donations/{id}/status", s.Authenticate(RequireRole("ADMIN", http.HandlerFunc(s.advanceDonation))))
	mux.Handle("GET /v1/donations", userRoute(func(w http.ResponseWriter, r *http.Request) {
		s.listDonationsFor(w, r, currentPrincipal(r).User.ID)
	}))
	mux.Handle("GET /v1/donations/{id}", userRoute(s.ownDonation))
	mux.Handle("GET /v1/points/balance", userRoute(func(w http.ResponseWriter, r *http.Request) {
		result, err := s.rewards.PointsBalance(r.Context(), currentPrincipal(r).User.ID)
		if err != nil {
			writeError(w, err)
			return
		}
		writeJSON(w, 200, result)
	}))
	mux.Handle("GET /v1/rewards", userRoute(func(w http.ResponseWriter, r *http.Request) {
		items, err := s.rewards.Rewards(r.Context())
		if err != nil {
			writeError(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"items": items})
	}))
	mux.Handle("GET /v1/redemptions", userRoute(func(w http.ResponseWriter, r *http.Request) {
		items, err := s.rewards.Redemptions(r.Context(), currentPrincipal(r).User.ID)
		if err != nil {
			writeError(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"items": items})
	}))
	mux.Handle("POST /v1/redemptions", userRoute(s.redeemReward))
}

func (s *Server) ownDonation(w http.ResponseWriter, r *http.Request) {
	id, ok := donationID(w, r)
	if !ok {
		return
	}
	detail, err := s.donations.DonationDetail(r.Context(), id)
	if err == nil && detail.UserID != currentPrincipal(r).User.ID {
		err = domain.ErrNotFound
	}
	writeDonationResult(w, detail, err)
}

func (s *Server) advanceDonation(w http.ResponseWriter, r *http.Request) {
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
	if req.ExpectedVersion < 1 || (req.TargetStatus != domain.DonationPickedUp && req.TargetStatus != domain.DonationCompleted) {
		writeError(w, domain.DonationValidation(map[string]string{"targetStatus": "Use PICKED_UP or COMPLETED with the latest positive expectedVersion."}))
		return
	}
	detail, err := s.rewards.AdvanceDonation(r.Context(), id, currentPrincipal(r).User.ID, req.ExpectedVersion, req.TargetStatus)
	writeDonationResult(w, detail, err)
}

func (s *Server) redeemReward(w http.ResponseWriter, r *http.Request) {
	var req struct {
		RewardID string `json:"rewardId"`
	}
	if err := decodeJSON(w, r, &req); err != nil {
		writeError(w, err)
		return
	}
	key := r.Header.Get("Idempotency-Key")
	if !domain.RequestKeyPattern.MatchString(key) || len(r.Header.Values("Idempotency-Key")) != 1 || req.RewardID == "" || len(req.RewardID) > 100 {
		writeError(w, domain.DonationValidation(map[string]string{"request": "Send rewardId and one Idempotency-Key of 8–128 letters, digits, underscores or hyphens."}))
		return
	}
	result, err := s.rewards.Redeem(r.Context(), currentPrincipal(r).User.ID, req.RewardID, key)
	if err != nil {
		if errors.Is(err, domain.ErrNotFound) {
			err = domain.Unauthorized()
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
