# Feature: Authentication (Identifier + Password)

## Purpose
Handle status lookup, sign-up progression, login, token refresh, logout, and password lifecycle for local accounts.

## Entry Points (API endpoints)
- `POST /api/v1/auth/identifier/status`
- `POST /api/v1/auth/identifier/sign-up`
- `POST /api/v1/auth/identifier/sign-up/complete`
- `POST /api/v1/auth/identifier/login`
- `POST /api/v1/auth/jwt/refresh`
- `POST /api/v1/auth/identifier/logout`
- `POST /api/v1/auth/password/forgot/request`
- `POST /api/v1/auth/password/forgot/reset`
- `POST /api/v1/auth/password/change`

## Flow (step-by-step backend flow)
1. Controller receives identifier-based request and resolves auth strategy.
2. Service validates account/provider state and credential/OTP requirements.
3. On login/refresh, JWTs are minted and refresh session is created/rotated.
4. Access/refresh cookies are written or cleared based on outcome.
5. Metrics and audit events are recorded for success/failure.

## Key Classes (controllers, services, repositories)
- Controller: `AuthenticationController`
- Services: `AuthCommonServiceImpl`, `EmailAuthenticationServiceImpl`, `PhoneAuthenticationServiceImpl`
- Resolver: `AuthenticationServiceResolver`
- Repositories: `AppUserRepository`, `SessionRepository`, `UserAuthProviderRepository`

## Security considerations
- Refresh token is required + device-scoped for rotation/logout.
- Refresh token value is persisted only as hash.
- Local login is blocked for Google-only accounts.
- Password reset/change revokes active sessions.

## Edge cases
- Missing refresh cookie or missing `device_id` returns auth error.
- Account exists but unverified email blocks login.
- Invalid principal type in auth context fails fast.
- Forgot-password response is intentionally generic to avoid account enumeration.

## Dependencies
- `JwtServices`
- `AuthenticationManager` + `PasswordEncoder`
- `OtpService`
- `SessionService`
- `AuditEventLogger` + Micrometer counters

## Notes for AI (important implementation assumptions)
- Identifier type is inferred/resolved through request payload and resolver strategy.
- Auth responses are wrapped in shared `ApiResponse` envelope.
- Cookie names/attributes are centralized in auth/security service code and app config.
