-- =========================
-- EVENT PUBLICATION
-- =========================
CREATE TABLE event_publication
(
    completion_attempts    INTEGER NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    publication_date       TIMESTAMP WITH TIME ZONE,
    id                     UUID    NOT NULL,
    event_type             VARCHAR(255),
    listener_id            VARCHAR(255),
    serialized_event       VARCHAR(255),
    status                 VARCHAR(255),
    CONSTRAINT event_publication_pkey PRIMARY KEY (id),
    CONSTRAINT event_publication_status_check
        CHECK (status IN ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'))
);

-- =========================
-- OTP TOKENS
-- =========================
CREATE
    SEQUENCE otp_tokens_seq START
    WITH 1 INCREMENT BY 50;

CREATE TABLE otp_tokens
(
    attempts     INTEGER                  NOT NULL,
    consumed     BOOLEAN                  NOT NULL,
    type         VARCHAR(6)               NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    id           BIGINT                   NOT NULL,
    last_sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at  TIMESTAMP WITH TIME ZONE,
    version      BIGINT,
    otp_purpose  VARCHAR(20)              NOT NULL,
    request_ip   VARCHAR(45),
    otp_hash     VARCHAR(200)             NOT NULL,
    identifier   VARCHAR(256)             NOT NULL,
    CONSTRAINT otp_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT otp_tokens_type_check CHECK (type IN ('EMAIL', 'PHONE')),
    CONSTRAINT otp_tokens_otp_purpose_check
        CHECK (otp_purpose IN ('SIGNUP', 'LOGIN', 'PASSWORD_RESET', 'TWO_FACTOR_AUTH'))
);

CREATE INDEX idx_otp_expires_at ON otp_tokens (expires_at);
CREATE INDEX idx_otp_identifier_type_created ON otp_tokens (identifier, type, created_at);
CREATE INDEX idx_otp_identifier_type_last_sent ON otp_tokens (identifier, type, last_sent_at);

-- =========================
-- PERMISSIONS
-- =========================
CREATE TABLE permissions
(
    deleted    BOOLEAN DEFAULT FALSE    NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version    BIGINT,
    id         UUID                     NOT NULL,
    name       VARCHAR(100)             NOT NULL,
    created_by VARCHAR(255)             NOT NULL,
    updated_by VARCHAR(255)             NOT NULL,
    CONSTRAINT permissions_pkey PRIMARY KEY (id),
    CONSTRAINT permissions_name_key UNIQUE (name)
);

-- =========================
-- ROLES
-- =========================
CREATE TABLE roles
(
    deleted     BOOLEAN DEFAULT FALSE    NOT NULL,
    system_role BOOLEAN                  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT,
    id          UUID                     NOT NULL,
    role_name   VARCHAR(50)              NOT NULL,
    created_by  VARCHAR(255)             NOT NULL,
    updated_by  VARCHAR(255)             NOT NULL,
    CONSTRAINT roles_pkey PRIMARY KEY (id),
    CONSTRAINT roles_role_name_check CHECK (role_name IN ('USER', 'ADMIN'))
);

-- =========================
-- ROLE PERMISSIONS
-- =========================
CREATE TABLE role_permissions
(
    permission_id UUID NOT NULL,
    role_id       UUID NOT NULL,
    CONSTRAINT role_permissions_pkey PRIMARY KEY (permission_id, role_id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id)
        REFERENCES permissions (id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id)
        REFERENCES roles (id)
);

-- =========================
-- USERS
-- =========================
CREATE TABLE users
(
    account_locked           BOOLEAN                  NOT NULL,
    deleted                  BOOLEAN DEFAULT FALSE    NOT NULL,
    failed_login_attempts    INTEGER                  NOT NULL,
    is_email_verified        BOOLEAN                  NOT NULL,
    is_phone_number_verified BOOLEAN                  NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at            TIMESTAMP,
    locked_at                TIMESTAMP,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    version                  BIGINT,
    id                       UUID                     NOT NULL,
    created_by               VARCHAR(255)             NOT NULL,
    email                    VARCHAR(255),
    password                 VARCHAR(255)             NOT NULL,
    phone_number             VARCHAR(255),
    status                   VARCHAR(255)             NOT NULL,
    updated_by               VARCHAR(255)             NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_phone_number_key UNIQUE (phone_number),
    CONSTRAINT users_status_check
        CHECK (status IN ('NOT_EXIST', 'CREATED', 'VERIFIED', 'DELETED'))
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_phone ON users (phone_number);

-- =========================
-- USER ROLES
-- =========================
CREATE TABLE user_roles
(
    roles_id UUID NOT NULL,
    user_id  UUID NOT NULL,
    CONSTRAINT user_roles_pkey PRIMARY KEY (roles_id, user_id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (roles_id)
        REFERENCES roles (id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id)
);

-- =========================
-- USER SESSIONS
-- =========================
CREATE TABLE user_sessions
(
    deleted            BOOLEAN DEFAULT FALSE    NOT NULL,
    revoked            BOOLEAN                  NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    expiry_date        TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at       TIMESTAMP WITH TIME ZONE,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT,
    id                 UUID                     NOT NULL,
    user_id            UUID                     NOT NULL,
    refresh_token_hash VARCHAR(512)             NOT NULL,
    user_agent         VARCHAR(1000),
    created_by         VARCHAR(255)             NOT NULL,
    device_id          VARCHAR(255)             NOT NULL,
    ip_address         VARCHAR(255),
    updated_by         VARCHAR(255)             NOT NULL,
    CONSTRAINT user_sessions_pkey PRIMARY KEY (id),
    CONSTRAINT fk_user_session_user FOREIGN KEY (user_id)
        REFERENCES users (id)
);