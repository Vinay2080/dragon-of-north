ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS avatar_source VARCHAR(32) NOT NULL DEFAULT 'NONE';

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS avatar_external_url VARCHAR(500);

-- Preserve existing user-managed avatars by default. Missing avatars stay syncable from Google.
UPDATE user_profiles
SET avatar_source = 'USER_DEFINED'
WHERE avatar_url IS NOT NULL
  AND BTRIM(avatar_url) <> ''
  AND avatar_source = 'NONE';

