package main

// Planned donor pickup flow: add authenticated USER routes for submission,
// list, and detail. Resolve the owner from the session, require an eligible
// clothing check owned by that donor and the clean/safe confirmation, and
// validate the address, contact, quantity, and preferred pickup window.
// Insert the PENDING_REVIEW request and its first audit entry in one
// PostgreSQL transaction. Scope idempotency to the owner and payload.
//
// Pickup completion extends the ADMIN transitions in donation.go. A
// PICKED_UP to COMPLETED transition needs one transaction for the versioned
// status, audit, unique point award, and balance update. Retrying must not
// award points twice. Exercise ownership, concurrent retries, and rollback
// against PostgreSQL before mounting the additional routes.
