-- Version 3: completion credits and reward redemptions, in the same transaction as the balance.
CREATE TABLE rewards (
    reward_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    icon TEXT NOT NULL CHECK (icon IN ('leaf', 'image', 'gift')),
    required_points INTEGER NOT NULL CHECK (required_points > 0),
    type TEXT NOT NULL CHECK (type IN ('DIGITAL', 'DEMO_VOUCHER')),
    is_active BOOLEAN NOT NULL DEFAULT true
);

INSERT INTO rewards (reward_id, name, description, icon, required_points, type) VALUES
('eco-supporter-badge', 'Eco Supporter Badge', 'Badge digital untuk langkah baikmu.', 'leaf', 10, 'DIGITAL'),
('loopy-digital-wallpaper', 'Loopy Digital Wallpaper', 'Warna baru untuk layar ponselmu.', 'image', 20, 'DIGITAL'),
('voucher-demo', 'Voucher Demo', 'SIMULASI · tidak berlaku di merchant.', 'gift', 30, 'DEMO_VOUCHER');

CREATE TABLE redemptions (
    redemption_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    reward_id TEXT NOT NULL REFERENCES rewards(reward_id),
    reward_name TEXT NOT NULL,
    reward_description TEXT NOT NULL,
    reward_icon TEXT NOT NULL,
    reward_type TEXT NOT NULL,
    points_used INTEGER NOT NULL CHECK (points_used > 0),
    remaining_balance INTEGER NOT NULL CHECK (remaining_balance >= 0),
    idempotency_key TEXT NOT NULL,
    redeemed_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (user_id, idempotency_key)
);
CREATE INDEX redemptions_owner_idx ON redemptions(user_id, redeemed_at DESC, redemption_id);

CREATE TABLE point_transactions (
    transaction_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    donation_id UUID UNIQUE REFERENCES donation_requests(donation_id),
    redemption_id UUID UNIQUE REFERENCES redemptions(redemption_id),
    points INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    CHECK ((donation_id IS NOT NULL AND redemption_id IS NULL AND points = 10)
        OR (donation_id IS NULL AND redemption_id IS NOT NULL AND points < 0))
);
CREATE INDEX point_transactions_owner_idx ON point_transactions(user_id, created_at DESC);
