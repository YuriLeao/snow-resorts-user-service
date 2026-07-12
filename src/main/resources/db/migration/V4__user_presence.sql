-- Lightweight app activity for friend online status (refreshed by mobile heartbeat).
CREATE TABLE users.user_presence (
    user_id      UUID PRIMARY KEY,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
