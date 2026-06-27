-- users schema: profiles, avatar references and friendships.

CREATE TABLE profiles (
    user_id           UUID PRIMARY KEY,
    display_name      VARCHAR(100) NOT NULL,
    avatar_s3_key     VARCHAR(512),
    avatar_url        VARCHAR(1024),
    avatar_updated_at TIMESTAMPTZ,
    last_resort_id    UUID,
    share_stats       VARCHAR(20) NOT NULL DEFAULT 'friends',
    share_location    VARCHAR(20) NOT NULL DEFAULT 'friends',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE friendships (
    user_id    UUID NOT NULL,
    friend_id  UUID NOT NULL,
    status     VARCHAR(20) NOT NULL,  -- PENDING | ACCEPTED | BLOCKED
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, friend_id)
);

CREATE INDEX idx_friendships_friend ON friendships (friend_id);
