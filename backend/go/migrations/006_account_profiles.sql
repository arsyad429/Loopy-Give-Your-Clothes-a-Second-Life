-- Version 6: optional usernames for existing accounts and email verification state.
ALTER TABLE users
    ADD COLUMN username TEXT CONSTRAINT users_username_key UNIQUE,
    ADD COLUMN email_verified_at TIMESTAMPTZ,
    ADD CONSTRAINT users_username_format CHECK (username ~ '^[a-z0-9_]{3,20}$');
