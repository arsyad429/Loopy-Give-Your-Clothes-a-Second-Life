package domain

import (
	"context"
	"time"
)

// AuthRepository defines account and session persistence operations.
type AuthRepository interface {
	Register(context.Context, Account, Session) (User, error)
	CreateAdmin(context.Context, Account) (User, error)
	FindByEmail(context.Context, string) (Account, error)
	CreateSession(context.Context, Session) error
	Authenticate(context.Context, []byte) (Principal, error)
	Rotate(context.Context, []byte, Session) (User, time.Time, time.Time, error)
	Revoke(context.Context, []byte) error
	Ping(context.Context) error
}

// DonationRepository defines donor submission and admin review operations.
type DonationRepository interface {
	CreateDonation(context.Context, CreateDonationSubmission) (DonationDetail, bool, error)
	DonationSummary(context.Context) (DonationSummary, error)
	ListDonations(context.Context, DonationFilter) ([]DonationItem, error)
	DonationDetail(context.Context, string) (DonationDetail, error)
	ReviewDonation(context.Context, DonationDecision) (DonationDetail, error)
}

// RewardsRepository defines completion point crediting and reward redemption operations.
type RewardsRepository interface {
	AdvanceDonation(context.Context, string, string, int64, string) (DonationDetail, error)
	PointsBalance(context.Context, string) (PointsBalance, error)
	Rewards(context.Context) ([]Reward, error)
	Redeem(context.Context, string, string, string) (RedemptionResult, error)
	Redemptions(context.Context, string) ([]Redemption, error)
}

// ClothingCheckRepository defines idempotent AI check and private photo persistence operations.
type ClothingCheckRepository interface {
	ClaimClothingCheck(ctx context.Context, userID, key string, hash []byte) (CheckClaim, string, error)
	CompleteClothingCheck(ctx context.Context, claim CheckClaim, kind string, result VisionResult, jpeg []byte) (ClothingCheck, error)
	ReleaseClothingCheck(ctx context.Context, claim CheckClaim) error
	FindClothingCheck(ctx context.Context, actor User, id string) (ClothingCheck, error)
	FindClothingPhoto(ctx context.Context, actor User, id string) ([]byte, error)
}

// VisionAnalyzer evaluates clothing photos for donation eligibility.
type VisionAnalyzer interface {
	Analyze(ctx context.Context, jpeg []byte, kind string) (VisionResult, error)
}
