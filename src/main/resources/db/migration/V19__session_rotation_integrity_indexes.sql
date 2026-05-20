-- Strengthen refresh/session integrity for rotation and revoke operations.

-- Keep only one active session row per user/device to avoid split-brain session state.
WITH ranked_active AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY user_id, device_id
               ORDER BY updated_at DESC, created_at DESC, id DESC
               ) AS rn
    FROM user_sessions
    WHERE revoked = false
      AND deleted = false
)
UPDATE user_sessions s
SET revoked = true,
    updated_at = now()
FROM ranked_active r
WHERE s.id = r.id
  AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_sessions_active_user_device
    ON user_sessions (user_id, device_id)
    WHERE revoked = false
      AND deleted = false;

-- Refresh hash lookup/update path is critical during token rotation.
CREATE INDEX IF NOT EXISTS idx_user_sessions_rotation_lookup
    ON user_sessions (user_id, device_id, refresh_token_hash)
    WHERE revoked = false
      AND deleted = false;
