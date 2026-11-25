-- Version 5: idempotent user pickup donation submissions.
CREATE TABLE donation_submit_requests (
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    request_key VARCHAR(128) NOT NULL,
    request_hash BYTEA NOT NULL CHECK (octet_length(request_hash) = 32),
    donation_id UUID NOT NULL REFERENCES donation_requests(donation_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    PRIMARY KEY (user_id, request_key)
);
