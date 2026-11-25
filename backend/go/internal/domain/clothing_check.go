package domain

import (
	"regexp"
	"time"
)

// ClothingKeyPattern validates Idempotency-Key values for clothing checks.
var ClothingKeyPattern = regexp.MustCompile(`^[A-Za-z0-9._-]{1,128}$`)

var supportedClothingTypes = map[string]bool{
	"TSHIRT": true, "SHIRT": true, "POLO": true, "BLOUSE": true, "TANK_TOP": true,
	"SWEATER": true, "HOODIE": true, "JACKET": true, "CARDIGAN": true,
	"LONG_PANTS": true, "SHORTS": true, "JEANS": true, "JOGGER": true, "SKIRT": true,
	"DRESS": true, "GAMIS": true, "TUNIC": true, "SPORTSWEAR": true, "SLEEPWEAR": true, "OTHER": true,
}

// IsValidClothingType reports whether kind is a supported clothing category code.
func IsValidClothingType(kind string) bool {
	return supportedClothingTypes[kind]
}

// VisionResult holds the structured AI eligibility assessment.
type VisionResult struct {
	Category       string  `json:"aiResult"`
	Description    string  `json:"aiDescription"`
	Recommendation *string `json:"actionRecommendation"`
}

// ClothingCheck represents a persisted AI clothing check record.
type ClothingCheck struct {
	ID            string `json:"checkId"`
	UserID        string `json:"userId"`
	ClothingPhoto string `json:"clothingPhoto"`
	ClothingType  string `json:"clothingType"`
	VisionResult
	CreatedAt time.Time `json:"createdAt"`
}

// CheckClaim tracks an in-flight idempotent clothing check reservation.
type CheckClaim struct {
	UserID string
	Key    string
	ID     string
}

// ClothingUpload holds a validated multipart clothing image upload.
type ClothingUpload struct {
	Kind string
	MIME string
	Data []byte
}
