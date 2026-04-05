-- =========================
-- 1. Add PENDING_VERIFICATION to user status constraint
-- =========================
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_status_check;

ALTER TABLE users
    ADD CONSTRAINT users_status_check
        CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DELETED'));

-- =========================
-- 2. Add LOGIN_UNVERIFIED to OTP purpose constraint
-- =========================
ALTER TABLE otp_tokens
    DROP CONSTRAINT IF EXISTS otp_tokens_otp_purpose_check;

ALTER TABLE otp_tokens
    ADD CONSTRAINT otp_tokens_otp_purpose_check
        CHECK (otp_purpose IN ('SIGNUP', 'LOGIN', 'PASSWORD_RESET', 'TWO_FACTOR_AUTH', 'LOGIN_UNVERIFIED'));
