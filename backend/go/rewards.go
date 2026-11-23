package main

import (
	"context"
	"time"
)

type rewardsRepository interface {
	AdvanceDonation(context.Context, string, string, int64, string) (donationDetail, error)
	PointsBalance(context.Context, string) (pointsBalance, error)
	Rewards(context.Context) ([]reward, error)
	Redeem(context.Context, string, string, string) (redemptionResult, error)
	Redemptions(context.Context, string) ([]redemption, error)
}

type pointsBalance struct {
	Balance   int       `json:"balance"`
	UpdatedAt time.Time `json:"updatedAt"`
}

type reward struct {
	ID             string `json:"rewardId"`
	Name           string `json:"name"`
	Description    string `json:"description"`
	Icon           string `json:"icon"`
	RequiredPoints int    `json:"requiredPoints"`
	Type           string `json:"type"`
	IsActive       bool   `json:"isActive"`
}

type redemption struct {
	ID          string    `json:"redemptionId"`
	RewardID    string    `json:"rewardId"`
	RewardName  string    `json:"rewardName"`
	Description string    `json:"description"`
	Icon        string    `json:"icon"`
	Type        string    `json:"type"`
	PointsUsed  int       `json:"pointsUsed"`
	RedeemedAt  time.Time `json:"redeemedAt"`
}

type redemptionResult struct {
	Redemption       redemption `json:"redemption"`
	RemainingBalance int        `json:"remainingBalance"`
	Replayed         bool       `json:"replayed"`
}

func rewardError(status int, code, message string) error {
	return &apiError{Status: status, Code: code, Message: message}
}
