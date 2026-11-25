CREATE TABLE clothing_checks (
    check_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    photo_key TEXT NOT NULL CHECK (btrim(photo_key) <> ''),
    clothing_type TEXT NOT NULL CHECK (btrim(clothing_type) <> ''),
    ai_result TEXT NOT NULL CHECK (ai_result IN ('POTENTIALLY_ELIGIBLE', 'NEEDS_RECHECK', 'NOT_RECOMMENDED')),
    ai_description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (check_id, user_id)
);

CREATE TABLE donation_requests (
    donation_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    check_id UUID NOT NULL,
    phone_number TEXT NOT NULL CHECK (btrim(phone_number) <> ''),
    pickup_address TEXT NOT NULL CHECK (btrim(pickup_address) <> ''),
    city TEXT NOT NULL CHECK (btrim(city) <> ''),
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    preferred_date DATE NOT NULL,
    preferred_time TEXT NOT NULL CHECK (btrim(preferred_time) <> ''),
    notes TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'PENDING_REVIEW'
        CHECK (status IN ('PENDING_REVIEW', 'PICKUP_SCHEDULED', 'PICKED_UP', 'COMPLETED', 'REJECTED')),
    scheduled_at TIMESTAMPTZ,
    rejection_reason TEXT,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version >= 1),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (check_id, user_id) REFERENCES clothing_checks(check_id, user_id),
    CHECK ((scheduled_at IS NOT NULL) = (status IN ('PICKUP_SCHEDULED', 'PICKED_UP', 'COMPLETED'))),
    CHECK ((rejection_reason IS NOT NULL) = (status = 'REJECTED')),
    CHECK (rejection_reason IS NULL OR char_length(btrim(rejection_reason)) BETWEEN 1 AND 500)
);

CREATE INDEX clothing_checks_owner_idx ON clothing_checks(user_id, created_at DESC);
CREATE INDEX donation_requests_queue_idx ON donation_requests(created_at, donation_id);
CREATE INDEX donation_requests_status_queue_idx ON donation_requests(status, created_at, donation_id);
CREATE INDEX donation_requests_owner_idx ON donation_requests(user_id, created_at DESC, donation_id DESC);

CREATE TABLE donation_status_history (
    donation_id UUID NOT NULL REFERENCES donation_requests(donation_id),
    version BIGINT NOT NULL CHECK (version >= 1),
    previous_status TEXT,
    new_status TEXT NOT NULL,
    updated_by UUID NOT NULL REFERENCES users(user_id),
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (donation_id, version),
    CHECK (
        (version = 1 AND previous_status IS NULL AND new_status = 'PENDING_REVIEW')
        OR (version > 1 AND previous_status IS NOT NULL AND (
            (previous_status = 'PENDING_REVIEW' AND new_status IN ('PICKUP_SCHEDULED', 'REJECTED'))
            OR (previous_status = 'PICKUP_SCHEDULED' AND new_status = 'PICKED_UP')
            OR (previous_status = 'PICKED_UP' AND new_status = 'COMPLETED')
        ))
    )
);
