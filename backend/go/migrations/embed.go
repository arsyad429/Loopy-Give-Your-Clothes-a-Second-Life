package migrations

import _ "embed"

//go:embed 001_accounts_and_sessions.sql
var accountsAndSessionsSQL string

//go:embed 002_pickup_and_donations.sql
var pickupAndDonationsSQL string

//go:embed 003_vision_and_private_photos.sql
var visionAndPrivatePhotosSQL string

//go:embed 004_rewards_and_ledger.sql
var rewardsAndLedgerSQL string

//go:embed 005_donation_submit_idempotency.sql
var donationSubmitIdempotencySQL string

// Versions lists PostgreSQL DDL migrations in strict execution order (1..N).
var Versions = []string{
	accountsAndSessionsSQL,
	pickupAndDonationsSQL,
	visionAndPrivatePhotosSQL,
	rewardsAndLedgerSQL,
	donationSubmitIdempotencySQL,
}
