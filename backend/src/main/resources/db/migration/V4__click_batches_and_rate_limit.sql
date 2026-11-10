ALTER TABLE game_state ADD COLUMN click_allowance DOUBLE PRECISION NOT NULL DEFAULT 600;

CREATE TABLE click_batch (
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL
);
