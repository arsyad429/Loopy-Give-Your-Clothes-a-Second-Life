-- Version 3: add Vision to the existing accounts and pickup database.
ALTER TABLE clothing_checks
    ADD COLUMN action_recommendation TEXT CHECK (length(btrim(action_recommendation)) BETWEEN 1 AND 600);

CREATE TABLE clothing_check_photos (
    check_id UUID PRIMARY KEY REFERENCES clothing_checks(check_id) ON DELETE CASCADE,
    jpeg BYTEA NOT NULL CHECK (octet_length(jpeg) BETWEEN 3 AND 2097152)
);

CREATE TABLE clothing_check_requests (
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    request_key VARCHAR(128) NOT NULL,
    request_hash BYTEA NOT NULL CHECK (octet_length(request_hash) = 32),
    claim_id UUID,
    claimed_until TIMESTAMPTZ,
    check_id UUID,
    PRIMARY KEY (user_id, request_key),
    FOREIGN KEY (check_id, user_id) REFERENCES clothing_checks(check_id, user_id) ON DELETE CASCADE,
    CHECK ((claim_id IS NULL) = (claimed_until IS NULL)),
    CHECK (check_id IS NULL OR claim_id IS NULL)
);
