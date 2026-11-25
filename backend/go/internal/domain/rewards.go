package domain

import "time"

// PointsBalance represents a user's current Loopy Points balance.
type PointsBalance struct {
	Balance   int       `json:"balance"`
	UpdatedAt time.Time `json:"updatedAt"`
}

// Reward represents an active item in the rewards catalog.
type Reward struct {
	ID             string `json:"rewardId"`
	Name           string `json:"name"`
	Description    string `json:"description"`
	Icon           string `json:"icon"`
	RequiredPoints int    `json:"requiredPoints"`
	Type           string `json:"type"`
	IsActive       bool   `json:"isActive"`
}

// Redemption represents a snapshot of a completed reward redemption.
type Redemption struct {
	ID          string    `json:"redemptionId"`
	RewardID    string    `json:"rewardId"`
	RewardName  string    `json:"rewardName"`
	Description string    `json:"description"`
	Icon        string    `json:"icon"`
	Type        string    `json:"type"`
	PointsUsed  int       `json:"pointsUsed"`
	RedeemedAt  time.Time `json:"redeemedAt"`
}

// RedemptionResult returns the redemption record alongside the remaining balance.
type RedemptionResult struct {
	Redemption       Redemption `json:"redemption"`
	RemainingBalance int        `json:"remainingBalance"`
	Replayed         bool       `json:"replayed"`
}
