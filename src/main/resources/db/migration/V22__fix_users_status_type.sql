-- Hibernate binds AppUserStatus as a string, so keep users.status as VARCHAR.

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_status_check;

ALTER TABLE users
    ALTER COLUMN status TYPE VARCHAR(255)
        USING status::text;

ALTER TABLE users
    ADD CONSTRAINT users_status_check
        CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DELETED'));

