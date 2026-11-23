package main

// Planned rewards flow: define an additive PostgreSQL migration for the
// catalog, redemption history, and point transactions. Read the balance,
// availability, and cost from the server. Seed the badge (10 points),
// wallpaper (20 points), and demo voucher (30 points) with stable identifiers.
//
// Scope redemption idempotency to the authenticated user and payload.
// Lock or conditionally debit the user's balance in the same transaction
// that writes the debit and redemption record. Verify rollback, concurrent
// debits, and repeat submissions before mounting catalog or redemption routes.
