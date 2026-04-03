# Data Model

## Core Entities

### `users` (`AppUser`)
- Identifier fields: `email`, `phone_number`
- Credential/state: `password`, `status`, verification flags
- Security state: `failed_login_attempts`, `account_locked`, `locked_at`, `last_login_at`
- Relationships:
  - 1:N `user_sessions`
  - 1:N `user_auth_providers`
  - 1:1 `user_profiles`
  - M:N `roles`

### `user_auth_providers` (`UserAuthProvider`)
- Tracks allowed auth providers per user (`LOCAL`, `GOOGLE`)
- Optional `provider_id` for provider identity
- Unique constraint on `(user_id, provider)`

### `user_sessions` (`Session`)
- Device-bound refresh session records
- Stores `refresh_token_hash`, `device_id`, `ip_address`, `user_agent`
- Lifecycle fields: `expiry_date`, `last_used_at`, `revoked`

### `otp_tokens` (`OtpToken`)
- OTP challenge state keyed by identifier + type + purpose
- Stores `otp_hash`, `expires_at`, `attempts`, `consumed`, `verified_at`
- Includes request metadata (`request_ip`, `last_sent_at`)

### `user_profiles` (`Profile`)
- User-facing profile data: `username`, `display_name`, `bio`, avatar fields
- One-to-one ownership by `users`

### `roles` + `permissions`
- RBAC foundation for role/permission mapping
- `user_roles` join table: user-to-role assignment
- `role_permissions` join table: role-to-permission assignment

## Relationship Summary
- `users` is the root aggregate for authentication domain.
- Session, provider, and profile records hang from user identity.
- OTPs are independent challenge records keyed by identifier rather than FK to users.

## Entity Lifecycle Notes
- Base entities include UUID primary keys, audit fields, soft-delete flag, and optimistic lock version.
- Flyway migration scripts under `src/main/resources/db/migration` govern schema evolution.
