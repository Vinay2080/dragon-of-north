-- =========================
-- 1. Drop refresh_tokens table
-- =========================
DROP TABLE IF EXISTS refresh_tokens CASCADE;

-- =========================
-- 2. Drop unwanted unique constraints on user_sessions
-- =========================
ALTER TABLE user_sessions
    DROP CONSTRAINT IF EXISTS uk78eira0fxlpwm9dcnjlp3c45y;
ALTER TABLE user_sessions
    DROP CONSTRAINT IF EXISTS uk7qdxwsqyute99enhqsg15io9;

-- =========================
-- 3. Fix role_permissions PK order
-- =========================
ALTER TABLE role_permissions
    DROP CONSTRAINT IF EXISTS role_permissions_pkey;
ALTER TABLE role_permissions
    ADD CONSTRAINT role_permissions_pkey
        PRIMARY KEY (permission_id, role_id);

-- =========================
-- 4. Fix user_roles PK order
-- =========================
ALTER TABLE user_roles
    DROP CONSTRAINT IF EXISTS user_roles_pkey;
ALTER TABLE user_roles
    ADD CONSTRAINT user_roles_pkey
        PRIMARY KEY (roles_id, user_id);