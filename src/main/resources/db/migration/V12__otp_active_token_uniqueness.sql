-- Ensure deterministic OTP lifecycle under concurrency by allowing
-- only one active (unconsumed) OTP per identifier/type/purpose.

-- Defensive cleanup before creating the partial unique index:
-- keep only latest unconsumed OTP active, consume older active tokens.
WITH ranked_active AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY identifier, type, otp_purpose
               ORDER BY created_at DESC, id DESC
           ) AS rn
    FROM otp_tokens
    WHERE consumed = FALSE
)
UPDATE otp_tokens t
SET consumed = TRUE
FROM ranked_active r
WHERE t.id = r.id
  AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_otp_single_active_per_flow
    ON otp_tokens (identifier, type, otp_purpose)
    WHERE consumed = FALSE;
