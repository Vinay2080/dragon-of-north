# Security

## Authentication Model (JWT)
- Access and refresh tokens are signed via RSA keys.
- Access token is used for request authentication (stateless).
- Refresh token is rotated and validated against persisted session state.
- Refresh token raw value is never stored; hashed value is stored in DB.

## Cookie Strategy
- Token transport uses cookies (not localStorage).
- CSRF cookie: `XSRF-TOKEN` (readable by client script for header echo).
- CSRF header expected: `X-XSRF-TOKEN`.
- Cookie policy is configurable via:
  - `app.security.cookie.same-site`
  - `app.security.cookie.secure`

## CSRF Strategy
- CSRF token repository: `CookieCsrfTokenRepository`.
- Bootstrap endpoint: `GET /api/v1/auth/csrf`.
- CSRF bypass list is explicit for selected pre-auth/public endpoints (login/status/oauth etc.).
- All non-bypassed state-changing endpoints require valid CSRF token/header pair.

## Authorization Model
- Public endpoints are explicitly whitelisted in `SecurityConfig.public_urls`.
- All non-whitelisted endpoints require authenticated principal.
- Method-level security is enabled (`@EnableMethodSecurity`) for future fine-grained rules.
- Role entities (`roles`, `permissions`) support RBAC expansion.

## User Lifecycle Enforcement

- `UserStateValidator` enforces lifecycle gates using `UserLifecycleOperation`.
- State-changing auth/session operations validate account state before mutation.
- Disallowed states fail fast with business errors (for example blocked/deleted restrictions).

## Security Headers
- CSP default policy: `default-src 'self'`
- HSTS enabled for subdomains, 1 year max-age
- Frame options: same-origin

## Abuse Resistance Controls
- `RateLimitFilter` adds per-endpoint bucket enforcement.
- OTP requests and login/signup paths are throttled.
- Account/login failures tracked via user fields (failed attempts, lock state).

## Audit + Monitoring
- Auth events logged with request/device/IP context.
- Micrometer counters track success/failure flows (`auth.login.*`, `auth.refresh.*`, etc.).
