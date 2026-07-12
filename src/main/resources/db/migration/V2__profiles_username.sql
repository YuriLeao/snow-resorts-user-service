-- Public @username for friend discovery (unique, case-insensitive).

ALTER TABLE profiles ADD COLUMN username VARCHAR(20);

DO $$
DECLARE
    r RECORD;
    base text;
    candidate text;
    suffix int;
BEGIN
    FOR r IN SELECT user_id, display_name FROM profiles WHERE username IS NULL ORDER BY created_at LOOP
        base := lower(regexp_replace(substring(r.display_name FROM 1 FOR 20), '[^a-zA-Z0-9_]', '', 'g'));
        IF base IS NULL OR length(base) < 3 THEN
            base := 'user' || substring(replace(r.user_id::text, '-', '') FROM 1 FOR 8);
        END IF;
        IF length(base) > 20 THEN
            base := substring(base FROM 1 FOR 20);
        END IF;
        candidate := base;
        suffix := 1;
        WHILE EXISTS (SELECT 1 FROM profiles p WHERE lower(p.username) = lower(candidate)) LOOP
            candidate := substring(base FROM 1 FOR greatest(1, 20 - length(suffix::text))) || suffix;
            suffix := suffix + 1;
        END LOOP;
        UPDATE profiles SET username = candidate WHERE user_id = r.user_id;
    END LOOP;
END $$;

ALTER TABLE profiles ALTER COLUMN username SET NOT NULL;
CREATE UNIQUE INDEX idx_profiles_username ON profiles (LOWER(username));
