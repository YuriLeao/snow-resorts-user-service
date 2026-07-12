-- Device push tokens for Expo push notifications (friend requests, etc.).
CREATE TABLE users.push_tokens (
    user_id    UUID NOT NULL,
    token      VARCHAR(255) NOT NULL,
    platform   VARCHAR(10),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, token)
);

CREATE INDEX idx_push_tokens_user ON users.push_tokens (user_id);
