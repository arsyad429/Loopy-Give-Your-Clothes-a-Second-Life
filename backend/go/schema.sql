-- Version 1: accounts and revocable, per-device sessions.
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    name TEXT NOT NULL CHECK (char_length(name) BETWEEN 1 AND 100),
    email TEXT NOT NULL CONSTRAINT users_email_key UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    total_points INTEGER NOT NULL DEFAULT 0 CHECK (total_points >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (email = lower(btrim(email)) AND octet_length(email) <= 254)
);

CREATE TABLE auth_sessions (
    session_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    access_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(access_hash) = 32),
    refresh_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(refresh_hash) = 32),
    access_expires_at TIMESTAMPTZ NOT NULL,
    refresh_expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (access_expires_at <= refresh_expires_at),
    CHECK (access_hash <> refresh_hash)
);
CREATE INDEX auth_sessions_user_idx ON auth_sessions(user_id);
CREATE INDEX auth_sessions_expiry_idx ON auth_sessions(refresh_expires_at);
