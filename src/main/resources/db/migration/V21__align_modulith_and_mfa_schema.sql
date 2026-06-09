-- Align database schema with current Modulith JPA and MFA/session models.

CREATE TABLE IF NOT EXISTS event_publication_archive
(
    id                     UUID    NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    completion_attempts    INTEGER NOT NULL,
    status                 VARCHAR(255),
    CONSTRAINT pk_event_publication_archive PRIMARY KEY (id)
);

ALTER TABLE user_sessions
    ADD COLUMN IF NOT EXISTS mfa_method_amr VARCHAR(32);

DO
$$
    BEGIN
        IF to_regtype('user_status') IS NULL THEN
            CREATE TYPE user_status AS ENUM ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DELETED');
        END IF;
    END
$$;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_status_check;

ALTER TABLE users
    ALTER COLUMN status TYPE user_status
        USING (
        CASE status
            WHEN 'NOT_EXIST' THEN 'DELETED'
            WHEN 'CREATED' THEN 'PENDING_VERIFICATION'
            WHEN 'VERIFIED' THEN 'ACTIVE'
            ELSE status
            END
        )::user_status;

ALTER TABLE user_auth_providers
    ALTER COLUMN provider TYPE VARCHAR(255) USING provider::VARCHAR(255);

