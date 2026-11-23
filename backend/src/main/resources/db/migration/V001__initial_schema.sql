CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE CHECK (email = lower(email)),
    password_hash TEXT,
    role TEXT NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    total_points INTEGER NOT NULL DEFAULT 0 CHECK (total_points >= 0)
);

CREATE TABLE user_sessions (
    token_hash BYTEA PRIMARY KEY CHECK (octet_length(token_hash) = 32),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX user_sessions_user_id_idx ON user_sessions(user_id);

CREATE TABLE clothing_checks (
    check_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    clothing_type TEXT NOT NULL CHECK (clothing_type ~ '^[A-Z][A-Z0-9_]{0,31}$'),
    ai_result TEXT NOT NULL CHECK (
        ai_result IN ('POTENTIALLY_ELIGIBLE', 'NEEDS_RECHECK', 'NOT_RECOMMENDED')
    ),
    ai_description TEXT NOT NULL CHECK (length(btrim(ai_description)) BETWEEN 1 AND 400),
    action_recommendation TEXT CHECK (
        length(btrim(action_recommendation)) BETWEEN 1 AND 600
    ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (user_id, check_id),
    CHECK (ai_result <> 'NOT_RECOMMENDED' OR action_recommendation IS NOT NULL)
);

CREATE INDEX clothing_checks_owner_created_idx
    ON clothing_checks(user_id, created_at DESC);

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
    FOREIGN KEY (user_id, check_id)
        REFERENCES clothing_checks(user_id, check_id) ON DELETE CASCADE,
    CHECK ((claim_id IS NULL) = (claimed_until IS NULL)),
    CHECK (check_id IS NULL OR claim_id IS NULL)
);
