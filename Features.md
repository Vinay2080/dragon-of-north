# Dragon of North — Complete Feature Inventory (Major + Minor)

This document is a code-grounded catalog of what the project currently implements, including both big and small
features, plus **why each feature exists**.

---

## 1) Authentication Architecture

### 1.1 Stateless security with JWT + Spring Security

- **What:** The backend is configured as stateless (`SessionCreationPolicy.STATELESS`) and authenticates requests
  through a JWT filter.
- **Why:** Stateless APIs scale better horizontally and avoid server-side HTTP session stickiness.
- **Where:** `SecurityConfig` and `JwtFilter`.

### 1.2 Access token + refresh token model (not access-only)

- **What:** Separate token types are issued (`access_token`, `refresh_token`) with distinct expirations.
- **Why:** Short-lived access token reduces risk exposure; refresh token preserves UX without forcing frequent logins.
- **Where:** `JwtServicesImpl` token constants + generation methods.

### 1.3 RSA asymmetric JWT signing

- **What:** Tokens are signed with private key and validated with public key.
- **Why:** Stronger key management pattern for production systems and safer token verification distribution.
- **Where:** `JwtServicesImpl` constructor loads keys via `KeyUtils`.

### 1.4 Token type claim enforcement

- **What:** Filter verifies only `access_token` is accepted for request auth.
- **Why:** Prevents refresh token misuse as bearer access token.
- **Where:** `JwtFilter` checks `token_type` claim.

### 1.5 Role claims in JWT and authority mapping

- **What:** Access tokens embed role names; filter maps them to `ROLE_*` authorities.
- **Why:** Enables authorization decisions without DB round-trip on each request.
- **Where:** `JwtServicesImpl` adds `roles` claim; `JwtFilter` maps claims to authorities.

### 1.6 Dual token transport support (cookie + Authorization header)

- **What:** Filter reads JWT from `Authorization: Bearer` or `access_token` cookie.
- **Why:** Supports browser cookie flows and API-tool/manual bearer flows.
- **Where:** `JwtFilter` token extraction logic.

### 1.7 Secure auth cookies

- **What:** Access/refresh cookies are `HttpOnly`, `Secure`, `SameSite=None`, with explicit max-age; cookies are cleared
  on logout.
- **Why:** Mitigates XSS token theft and supports cross-site frontend deployments with cookies.
- **Where:** `AuthCommonServiceImpl` cookie helper methods.

### 1.8 Public/private endpoint split

- **What:** Auth + OTP + docs + health endpoints are public; all others require authentication.
- **Why:** Principle of least privilege and explicit security boundary.
- **Where:** `SecurityConfig.public_urls`.

### 1.9 Method-level authorization enabled

- **What:** `@EnableMethodSecurity` and `@PreAuthorize` on protected endpoints.
- **Why:** Defense in depth beyond URL matching.
- **Where:** `SecurityConfig`, `SessionController`.

---

## 2) Identifier-based Authentication Flows

### 2.1 Email and phone authentication support

- **What:** Two authentication service implementations exist and are selected by identifier type.
- **Why:** Multi-channel onboarding and login flexibility.
- **Where:** `EmailAuthenticationServiceImpl`, `PhoneAuthenticationServiceImpl`, `AuthenticationServiceResolver`.

### 2.2 Identifier format/type mismatch validation

- **What:** Resolver validates email regex and phone pattern before dispatching.
- **Why:** Prevents logically invalid combinations (e.g., phone string with EMAIL type).
- **Where:** `AuthenticationServiceResolver.resolve`.

### 2.3 Explicit account lifecycle states

- **What:** User lifecycle supports states like `NOT_EXIST`, `CREATED`, `VERIFIED`, `DELETED`.
- **Why:** Makes onboarding and eligibility checks explicit and predictable.
- **Where:** `AppUserStatus` usage in auth flows and README/API docs.

### 2.4 Signup completion gates verification + role assignment

- **What:** `completeSignUp` updates state and assigns default role.
- **Why:** Keeps role assignment and account activation controlled after verification.
- **Where:** `EmailAuthenticationServiceImpl` / `PhoneAuthenticationServiceImpl` + `AuthCommonServiceImpl`.

### 2.5 Password hashing with BCrypt

- **What:** Passwords are encoded before save.
- **Why:** Prevents plaintext password storage and supports adaptive hash cost.
- **Where:** Signup flows in auth service implementations; encoder bean in `SecurityConfig`.

---

## 3) Session Management and Token Rotation

### 3.1 Device-aware session creation

- **What:** Login stores session metadata (`deviceId`, IP, user-agent, expiry, last-used).
- **Why:** Enables per-device visibility and targeted revocation.
- **Where:** `AuthCommonServiceImpl.login`, `SessionServiceImpl.createSession`, `Session` model.

### 3.2 Refresh token hash storage (not raw token)

- **What:** Refresh token is SHA-256 hashed before persistence.
- **Why:** Reduces blast radius if DB leaks (raw refresh tokens not stored).
- **Where:** `TokenHasher`, `SessionServiceImpl`.

### 3.3 Refresh token rotation

- **What:** On refresh, old refresh token session is validated then hash is replaced with new refresh token hash.
- **Why:** Rotation lowers replay risk from stolen old refresh token.
- **Where:** `AuthCommonServiceImpl.refreshToken`, `SessionServiceImpl.validateAndRotateSession`.

### 3.4 Session revocation capabilities

- **What:** Revoke current session (logout), revoke one by ID, revoke all other sessions.
- **Why:** Critical account takeover mitigation and multi-device control.
- **Where:** `AuthenticationController.logout`, `SessionController`, `SessionServiceImpl`, `SessionRepository`.

### 3.5 Session ownership enforcement

- **What:** Revoke-by-id uses user-scoped lookup (`findByIdAndAppUserId`).
- **Why:** Prevents one user revoking another user’s sessions.
- **Where:** `SessionServiceImpl.revokeSessionById`, `SessionRepository`.

### 3.6 Session cleanup jobs

- **What:** Deletes expired sessions and old revoked sessions on schedule.
- **Why:** Keeps session table lean and limits stale security artifacts.
- **Where:** `CleanupTask.cleanupSessions`, `application.yaml` cleanup settings.

---

## 4) OTP Engine (Email + Phone)

### 4.1 Purpose-aware OTP model

- **What:** OTP purpose enum includes `SIGNUP`, `LOGIN`, `PASSWORD_RESET`, `TWO_FACTOR_AUTH`.
- **Why:** Separates security contexts so OTP from one flow can’t be reused in another.
- **Where:** `OtpPurpose`, OTP request DTO schemas.

### 4.2 OTP hash-at-rest with BCrypt

- **What:** OTP values are BCrypt-hashed before persistence.
- **Why:** Prevents OTP plaintext leakage from DB.
- **Where:** `OtpServiceImpl.createOtp`, `OtpToken.otpHash`.

### 4.3 OTP verification hardening

- **What:** Handles expired OTP, max attempts, consumed OTP, and purpose mismatch.
- **Why:** Protects against brute-force and replay.
- **Where:** `OtpServiceImpl.verifyToken`, `OtpVerificationStatus`.

### 4.4 OTP request abuse controls

- **What:** Resend cooldown + max requests per window are enforced.
- **Why:** Stops spam and OTP flooding attacks.
- **Where:** `OtpServiceImpl.enforceRateLimits`, `application.yaml` OTP properties.

### 4.5 OTP persistence and indexing

- **What:** OTP table has indexes for identifier/type/timestamps; optimistic locking version field.
- **Why:** Faster lookups and safer concurrent updates.
- **Where:** `OtpToken` entity.

### 4.6 Asynchronous OTP delivery through AWS

- **What:** Email via SES and SMS via SNS are sent asynchronously.
- **Why:** Faster API response and production-grade delivery channels.
- **Where:** `EmailOtpSender`, `SesEmailService`, `PhoneOtpSender`, `SesConfig`, `SnsConfig`.

### 4.7 OTP cleanup scheduler

- **What:** Expired OTPs are periodically purged.
- **Why:** Improves table hygiene and security posture.
- **Where:** `CleanupTask.cleanupExpiredOtpTokens`.

---

## 5) Rate Limiting & Abuse Prevention

### 5.1 Endpoint-specific rate limiting

- **What:** Different rate limit types for signup, login, OTP endpoints.
- **Why:** Risk-based throttling (not one-size-fits-all limits).
- **Where:** `application.yaml` rate-limit endpoint/rule mapping.

### 5.2 Redis-backed distributed buckets (Bucket4j)

- **What:** Bucket state stored in Redis via Lettuce proxy manager.
- **Why:** Consistent limits across multiple app instances; better than in-memory for distributed deployments.
- **Where:** `RateLimitConfig`, `RateLimitBucketServiceImpl`.

### 5.3 Smart limiter keys

- **What:** Uses authenticated user id when available, otherwise client IP.
- **Why:** Fairness and abuse resistance for both logged-in and anonymous traffic.
- **Where:** `RateLimitKeyResolver`.

### 5.4 Helpful rate limit response headers

- **What:** Sends `X-RateLimit-Remaining`, `X-RateLimit-Capacity`, `Retry-After`.
- **Why:** Gives clients clear throttling visibility and retry guidance.
- **Where:** `RateLimitFilter`.

### 5.5 Rate-limit observability metrics

- **What:** Separate Micrometer counters for blocked vs successful requests by limit type.
- **Why:** Easier tuning, alerting, and abuse trend analysis.
- **Where:** `RateLimitConfig` counter beans, `RateLimitFilter` updates.

---

## 6) API Design, Validation, and Error Handling

### 6.1 Standard API response envelope

- **What:** Uniform `ApiResponse` shape for success/failed responses.
- **Why:** Predictable frontend parsing and cleaner contracts.
- **Where:** `ApiResponse`.

### 6.2 Centralized exception mapping

- **What:** Global exception handler translates validation/business/runtime errors into structured responses.
- **Why:** Consistent client behavior and cleaner controller code.
- **Where:** `ApplicationExceptionHandler`.

### 6.3 Structured error catalog

- **What:** Enumerated business error codes + HTTP status mapping.
- **Why:** Stable, explicit error contract for frontend and monitoring.
- **Where:** `ErrorCode`.

### 6.4 DTO-level validation and schema examples

- **What:** Controllers use validated request DTOs and OpenAPI example payloads.
- **Why:** Better API quality, safer input handling, and faster integrator onboarding.
- **Where:** `AuthenticationController`, `OtpController`, `SessionController`.

---

## 7) Frontend Security/UX Features

### 7.1 Persistent per-browser device id

- **What:** Frontend generates/stores UUID device id in localStorage.
- **Why:** Enables stable device-aware sessions and revoke-other logic.
- **Where:** `frontend/src/utils/device.js`.

### 7.2 Auth context with startup session probe

- **What:** Auth provider validates auth state by calling session endpoint.
- **Why:** Keeps login state aligned with backend cookie/session validity.
- **Where:** `frontend/src/context/AuthContext.jsx`.

### 7.3 Auto refresh-on-401 with single-flight guard

- **What:** API service retries once after refresh and prevents parallel refresh storms.
- **Why:** Better UX and safer token refresh behavior under concurrent requests.
- **Where:** `frontend/src/services/apiService.js`.

### 7.4 Rate-limit aware UI feedback

- **What:** Frontend listens to rate-limit headers and shows remaining quota/countdown.
- **Why:** Prevents user confusion when throttled and encourages pacing.
- **Where:** `RateLimitInfo.jsx`, `apiService.js` header extraction.

### 7.5 Protected routing

- **What:** Private dashboard route guarded by auth state + loading gate.
- **Why:** Prevents unauthorized rendering flashes and secures client routing.
- **Where:** `ProtectedRoute.jsx`, `App.jsx`.

### 7.6 Session management UI

- **What:** Dashboard lists sessions, marks current device, supports single revoke + revoke others.
- **Why:** User-visible account security controls.
- **Where:** `DashboardPage.jsx`, `frontend/src/config.js` session endpoints.

---

## 8) Infra, Docs, and Operations Readiness

### 8.1 Postgres + Redis externalized config via env

- **What:** DB/Redis/JWT/AWS and auth throttling settings are env-driven.
- **Why:** Production-friendly deployment and environment separation.
- **Where:** `application.yaml`.

### 8.2 Swagger/OpenAPI with security schemes

- **What:** OpenAPI config includes metadata, server entries, and auth schemes.
- **Why:** Better API discoverability and testing for collaborators/interviewers.
- **Where:** `OpenApiConfig`.

### 8.3 Prometheus/Actuator exposure

- **What:** Health/metrics/prometheus endpoints are enabled.
- **Why:** Basic observability for runtime health and auth/rate-limit insights.
- **Where:** `application.yaml` management section.

### 8.4 Startup data initialization

- **What:** Roles are auto-created on startup; test data initializer exists.
- **Why:** Stable environments and faster local onboarding/testing.
- **Where:** `RolesInitializer`, `TestDataInitializer`.

---

## 9) Testing Coverage (What exists)

### 9.1 Broad unit tests across layers

- Controllers, services, repositories, security config/filter, resolver, rate limiting, exception handlers.

### 9.2 Integration tests with Testcontainers gating

- Session and OTP flow integration tests exist; Docker availability condition is used for environment-aware execution.

### 9.3 Why this matters

- Auth/security systems fail on edge cases first; deep test coverage reduces regression risk in critical flows.

(See `/src/test/java/...` including `SessionFlowIT`, `OtpFlowIT`, `JwtFilterTest`, `RateLimitFilterTest`,
controller/service tests.)

# Dragon of North — Complete Feature Inventory (Major + Minor)

This document is a **full project feature inventory**: what exists today, including small technical decisions, and **why
each feature is useful**.

---

## 1) Authentication and Identity Features

### 1.1 Identifier-based auth (email or phone)

- **What exists:** Authentication flows are designed around an `identifier` + `identifier_type` model, supporting both
  email and phone users.
- **Why it is there:** Gives flexibility for real-world onboarding (email-first or phone-first markets) without
  duplicating auth architecture.

### 1.2 Explicit account lifecycle states

- **What exists:** User status includes `NOT_EXIST`, `CREATED`, `VERIFIED`, and `DELETED`-style lifecycle handling in
  APIs and enums.
- **Why it is there:** Prevents ambiguous account states; makes signup and verification flows deterministic and
  testable.

### 1.3 Signup + signup completion separation

- **What exists:** Signup is split into initial user creation and a dedicated completion endpoint.
- **Why it is there:** Aligns with OTP-first verification patterns; avoids directly promoting an unverified user to
  fully active.

### 1.4 Login with device context

- **What exists:** Login requires/stores `device_id` and captures user-agent and IP metadata.
- **Why it is there:** Enables per-device session security, device-level revocation, and incident investigation.

### 1.5 Logout bound to current device

- **What exists:** Logout revokes session by refresh token + device id and clears auth cookies.
- **Why it is there:** Ensures logout is meaningful for distributed multi-device sessions, not just client-side token
  deletion.

---

## 2) JWT, Cookies, and Session Security

### 2.1 Stateless API security with JWT

- **What exists:** Security config is stateless (`SessionCreationPolicy.STATELESS`) and JWT-backed.
- **Why it is there:** Horizontal scalability and reduced server memory pressure compared with in-memory HTTP sessions.

### 2.2 Access + refresh token split (not refresh-only)

- **What exists:** Distinct access and refresh tokens are generated and validated with token type claims.
- **Why it is there:** Limits blast radius of short-lived access tokens while preserving UX via refresh.

### 2.3 Refresh token rotation

- **What exists:** Refresh flow rotates refresh token and updates stored hashed session token (
  `validateAndRotateSession`).
- **Why it is there:** Replay resistance; old refresh tokens become stale after use.

### 2.4 RSA-signed JWTs (asymmetric keys)

- **What exists:** JWT service loads private/public keys and signs/verifies with RSA.
- **Why it is there:** Stronger key-management model for production deployments and verifier/signing separation.

### 2.5 Token typing and issuer claims

- **What exists:** Claims include `token_type` and issuer metadata.
- **Why it is there:** Prevents misuse of refresh token as access token and improves verification strictness.

### 2.6 Role claims inside access token

- **What exists:** Role names are embedded in access token claims.
- **Why it is there:** Avoids DB lookup on every request for common authorization checks.

### 2.7 HTTP-only secure cookies for both tokens

- **What exists:** Cookies are `HttpOnly`, `Secure`, and `SameSite=None`.
- **Why it is there:** Reduces XSS token theft risk and supports cross-site frontend/backend deployment patterns.

### 2.8 Cookie cleanup on failures and logout

- **What exists:** Access/refresh cookies are actively cleared on logout and refresh failures.
- **Why it is there:** Security hygiene to reduce stale credential persistence on clients.

### 2.9 Hashed refresh tokens in persistence

- **What exists:** Refresh token is SHA-256 hashed before storing in DB.
- **Why it is there:** Database compromise does not directly expose raw refresh credentials.

### 2.10 JWT extraction from header or cookie

- **What exists:** JWT filter accepts Bearer auth and `access_token` cookie.
- **Why it is there:** Supports browser cookie auth and API-tool/manual bearer workflows.

---

## 3) Session Management Features

### 3.1 Session entity with operational metadata

- **What exists:** Session stores device id, IP, user-agent, expiry, last-used-at, revoked flag.
- **Why it is there:** Enables user-visible session inventory and security analytics.

### 3.2 One active session record per device/user pair

- **What exists:** Existing same-device session is replaced on new login for that device.
- **Why it is there:** Prevents duplicate stale rows and keeps device view clean.

### 3.3 User session listing API

- **What exists:** `GET /api/v1/sessions/get/all` returns session summary list.
- **Why it is there:** Gives transparency to users about account access footprint.

### 3.4 Revoke specific session API

- **What exists:** `DELETE /api/v1/sessions/delete/{sessionId}` revokes by session id.
- **Why it is there:** Fine-grained incident response (“logout from one compromised device”).

### 3.5 Revoke all-other-sessions API

- **What exists:** `POST /api/v1/sessions/revoke-others` keeps current device, revokes the rest.
- **Why it is there:** Fast account lockdown after suspicious activity.

### 3.6 Session validation in refresh path

- **What exists:** Refresh checks DB session presence, revoked status, and expiration before issuing tokens.
- **Why it is there:** Puts server-side control over refresh lifecycle (not pure JWT trust).

### 3.7 Background session cleanup

- **What exists:** Scheduled cleanup removes expired sessions and old revoked sessions.
- **Why it is there:** Storage hygiene and performance stability over long uptime.

---

## 4) OTP Engine Features

### 4.1 Multi-channel OTP (email + phone)

- **What exists:** Separate OTP request/verify endpoints for email and phone.
- **Why it is there:** Channel flexibility and smoother global user onboarding.

### 4.2 Purpose-scoped OTPs

- **What exists:** OTP purpose enum includes `SIGNUP`, `LOGIN`, `PASSWORD_RESET`, `TWO_FACTOR_AUTH`.
- **Why it is there:** Prevents OTP reuse across unrelated flows and supports future security expansion.

### 4.3 OTP hashing at rest

- **What exists:** OTP values are BCrypt-hashed before persistence.
- **Why it is there:** Avoids storing plain OTP secrets in DB.

### 4.4 OTP verification state machine

- **What exists:** Verification returns granular outcomes such as success, invalid, expired, consumed, attempt exceeded,
  invalid purpose.
- **Why it is there:** Better UX and safer backend decisions than binary pass/fail.

### 4.5 OTP attempt tracking + consume-on-success

- **What exists:** Attempts are incremented and token is marked consumed when verified.
- **Why it is there:** Mitigates brute force and replay.

### 4.6 OTP resend cooldown + request window limits

- **What exists:** Cooldown and per-window max requests are enforced.
- **Why it is there:** Spam prevention, abuse control, and sender cost control.

### 4.7 OTP persistence with optimistic locking

- **What exists:** OTP token model includes `@Version`.
- **Why it is there:** Reduces race-condition issues under concurrent verify/resend requests.

### 4.8 Scheduled OTP cleanup

- **What exists:** Expired OTPs are periodically deleted.
- **Why it is there:** Keeps OTP table small and operationally healthy.

---

## 5) Rate Limiting and Abuse Prevention

### 5.1 Endpoint-classified rate limiting

- **What exists:** Endpoint patterns map to rate-limit types (`signup`, `login`, `otp`).
- **Why it is there:** Different risk surfaces need different budgets.

### 5.2 Bucket4j token-bucket algorithm

- **What exists:** Capacity/refill settings per type with shared configuration.
- **Why it is there:** Predictable burst + sustained throughput control.

### 5.3 Redis-backed distributed bucket state (not in-memory)

- **What exists:** Bucket4j uses Redis/Lettuce proxy manager.
- **Why it is there:** Consistent throttling across multiple backend instances.

### 5.4 Dynamic keying (user when authenticated, IP otherwise)

- **What exists:** Resolver uses authenticated identity when available, else client IP.
- **Why it is there:** Fairer controls and stronger anti-abuse coverage pre- and post-login.

### 5.5 Standard limit telemetry headers

- **What exists:** API returns `X-RateLimit-Remaining`, `X-RateLimit-Capacity`, `Retry-After`.
- **Why it is there:** Frontend and clients can adapt behavior and show useful wait guidance.

### 5.6 Rate-limit metrics counters

- **What exists:** Per-type counters for blocked and successful requests.
- **Why it is there:** Production observability and tuning support.

### 5.7 Fail-open strategy on limiter backend errors

- **What exists:** On internal rate limiter failure, requests are currently allowed.
- **Why it is there:** Availability-first posture; authentication endpoint stays reachable during transient Redis
  issues.

---

## 6) Security Hardening and Error Handling

### 6.1 Spring Security hardening headers

- **What exists:** CSP, HSTS, frame-options, and controlled security chain setup.
- **Why it is there:** Baseline browser hardening against common web attacks.

### 6.2 Method-level security enabled

- **What exists:** `@EnableMethodSecurity` with `@PreAuthorize` usage on session endpoints.
- **Why it is there:** Fine-grained endpoint authorization gates.

### 6.3 Centralized exception contract

- **What exists:** Global exception advice maps errors to standardized API response shape.
- **Why it is there:** Stable client contract and easier frontend error handling.

### 6.4 Filter-level exception normalization

- **What exists:** Dedicated filter catches `BusinessException` thrown before controllers.
- **Why it is there:** Consistent JSON errors even for filter-chain failures (e.g., rate limiting).

### 6.5 Strong DTO validation

- **What exists:** Controllers use `@Valid` request models across auth/otp/session APIs.
- **Why it is there:** Rejects malformed input early and reduces unsafe code paths.

---

## 7) Authorization and Role Model

### 7.1 Role + permission entities

- **What exists:** Roles and permissions are modeled with many-to-many relationships.
- **Why it is there:** Scales from simple RBAC to more granular permission-driven controls.

### 7.2 Authority expansion in user details

- **What exists:** Security principal exports both role-prefixed and permission-prefixed authorities.
- **Why it is there:** Enables future fine-grained authorization without redesigning auth core.

### 7.3 Startup role initialization

- **What exists:** Missing system roles are auto-initialized on app startup.
- **Why it is there:** Prevents boot-time/config drift issues in fresh environments.

---

## 8) Data, Persistence, and Auditing Features

### 8.1 PostgreSQL + Spring Data JPA

- **What exists:** Relational persistence with dedicated repositories for users, roles, sessions, OTPs.
- **Why it is there:** Strong consistency and query flexibility for auth-critical operations.

### 8.2 Base entity with UUIDs and audit fields

- **What exists:** Shared `BaseEntity` includes UUID id, created/updated timestamps, createdBy/updatedBy.
- **Why it is there:** Traceability and consistency across all entities.

### 8.3 AuditorAware integration

- **What exists:** Auditor resolves actor identity (authenticated principal or system fallback).
- **Why it is there:** Reliable audit attribution for automated and user-driven writes.

### 8.4 Soft-delete support field

- **What exists:** Base entity contains `deleted` flag.
- **Why it is there:** Supports non-destructive lifecycle patterns where needed.

### 8.5 Optimistic locking on core entities

- **What exists:** `@Version` fields are used (e.g., base entity, OTP token).
- **Why it is there:** Guards against concurrent update anomalies.

---

## 9) API Documentation and Operability

### 9.1 OpenAPI/Swagger with cookie security schemes

- **What exists:** OpenAPI config includes bearer + cookie auth schemes.
- **Why it is there:** Easier QA/testing and better API discoverability.

### 9.2 Endpoint examples in controller annotations

- **What exists:** Request payload examples are embedded in auth/otp/session controllers.
- **Why it is there:** Reduces onboarding friction and API misuse.

### 9.3 Actuator + Prometheus exposure

- **What exists:** Health/metrics/prometheus endpoints are configured.
- **Why it is there:** Production readiness and monitoring integration.

### 9.4 AWS SES/SNS integration points

- **What exists:** SES/SNS dependencies and OTP sender services/config are included.
- **Why it is there:** Real delivery channels for OTP in production environments.

---

## 10) Frontend Features (Security + UX)

### 10.1 Protected routing and bootstrap auth check

- **What exists:** Protected routes wait for auth bootstrap, then redirect unauthenticated users.
- **Why it is there:** Prevents accidental content exposure and route flicker.

### 10.2 Device id generation and reuse

- **What exists:** Frontend generates/persists a device id and uses it in login/refresh/logout/revoke-others flows.
- **Why it is there:** Keeps backend session model consistent from browser side.

### 10.3 Auth context lifecycle management

- **What exists:** Central auth provider manages login/logout state and storage.
- **Why it is there:** Keeps auth state predictable across pages.

### 10.4 API service with 401-triggered refresh retry

- **What exists:** API layer auto-attempts refresh and retries failed request once.
- **Why it is there:** Better UX with silent token refresh behavior.

### 10.5 Frontend rate-limit awareness

- **What exists:** API layer captures rate-limit headers; `RateLimitInfo` component subscribes and shows
  feedback/countdown.
- **Why it is there:** User transparency and reduced repeated failed attempts.

### 10.6 Session management UI

- **What exists:** Dashboard fetches sessions, shows stats, revokes one/all-other sessions, and handles action/result
  states.
- **Why it is there:** User-controlled account security from UI.

### 10.7 OTP UI quality details

- **What exists:** Multi-box OTP input UX, resend timer, per-error messaging, and completion flow.
- **Why it is there:** Improves conversion and lowers OTP support friction.

---

## 11) Testing and Quality Features

### 11.1 Broad unit + slice coverage

- **What exists:** Tests cover controllers, services, repositories, config/security, exception handling, rate limiting,
  and token hashing.
- **Why it is there:** Protects auth-critical logic and reduces regression risk.

### 11.2 Integration tests for auth/session/otp flows

- **What exists:** Integration tests validate multi-step flows and cookie/session behavior.
- **Why it is there:** Confirms subsystem integration (not only isolated unit behavior).

### 11.3 Testcontainers and Docker-gated integration runs

- **What exists:** Test setup supports containerized integration dependencies and conditional execution.
- **Why it is there:** More realistic CI/local integration confidence.

---

## 12) Small but Important Engineering Details

- **Consistent API response wrapper (`ApiResponse`)** for success/failure contracts.
- **Identifier normalization** (email lowercase/trim, phone cleanup) before OTP operations.
- **Current-device preserving revoke strategy** (`revoke-others`) for safer account lockdown UX.
- **Session list ordering by recent usage** (`lastUsedAt DESC`) for better user relevance.
- **Startup seed data support** for predictable testing/demo users and sessions.
- **Separate profile for integration tests (Failsafe)** to keep unit and integration pipelines clean.
- **Dependency pinning/overrides for known CVE-sensitive artifacts** in dependency management.

---

## Notes on “present vs. fully enabled”

Some capabilities are intentionally scaffolded for future depth:

- OTP purposes already include `PASSWORD_RESET` and `TWO_FACTOR_AUTH` for future flow expansion.
- Role/permission model is in place even if your current public API surface focuses on core user flows.

This is good architecture: you already have extension points without forcing premature complexity.

# Dragon of North — Complete Feature Inventory (Major + Minor)

This document is a code-grounded inventory of implemented features, including small but important design choices and why
they exist.

## 1) Authentication and account lifecycle

### Identifier-based auth (email + phone)

- Supports both `EMAIL` and `PHONE` identifiers with resolver-based service dispatch.
- Why: keeps core auth flow consistent while allowing identifier-specific logic and validation.

### Account lifecycle states

- Uses statuses like `NOT_EXIST`, `CREATED`, `VERIFIED`, `DELETED` for explicit user journey state management.
- Why: frontend can branch by status safely (e.g., continue signup vs login), and backend enforces valid transitions.

### Signup and signup-complete split

- Signup creates user in `CREATED`; completion transitions only `CREATED -> VERIFIED`.
- Why: prevents bypassing verification and keeps account activation explicit.

### Default role assignment on verification

- Assigns `USER` role if a user has no roles.
- Why: ensures minimum authorization baseline for newly verified accounts.

### Identifier mismatch validation

- Resolver validates identifier format against requested type (email regex for EMAIL, numeric pattern for PHONE).
- Why: prevents inconsistent payloads and auth confusion.

## 2) JWT, token model, and session security

### RSA-signed JWTs (asymmetric crypto)

- Uses private/public key pair loading via `KeyUtils` and signs tokens with RSA.
- Why: stronger key separation and safer verification boundaries versus shared secret in distributed setups.

### Access + refresh token pair (not just one token)

- Access token carries roles claim and short-lived auth context.
- Refresh token is separate with type claim and longer lifetime.
- Why: balances UX and security; short access lifetime with renewable authenticated sessions.

### Token type claims (`access_token` vs `refresh_token`)

- JWT claims include `token_type`; filter accepts only access tokens for request auth.
- Why: avoids token confusion attacks (using refresh token where access token is required).

### HTTP-only secure cookies for auth tokens

- Access/refresh are written to cookies with `HttpOnly`, `Secure`, `SameSite=None`.
- Why: reduces JS exposure risk and supports browser-based auth flows with credentials.

### Stateless request authorization

- Security filter chain is `STATELESS`, JWT filter authenticates each request.
- Why: horizontal scalability and no server-side HTTP session stickiness.

### Auth from header or cookie

- JWT filter reads `Authorization: Bearer ...` or `access_token` cookie.
- Why: supports API clients and browser clients with one backend.

## 3) Refresh token rotation and session tracking

### Persistent session entity per device

- Session stores `deviceId`, `ipAddress`, `userAgent`, `lastUsedAt`, `expiryDate`, revoked flag.
- Why: enables device-aware security and visibility.

### Refresh token hash storage (not raw)

- Refresh token is SHA-256 hashed before DB persistence.
- Why: limits damage if DB rows leak; raw token not directly reusable.

### One active session per user-device key

- Existing session for same user+device is replaced on new login.
- Why: avoids duplicate stale rows and ambiguous device session state.

### Refresh token rotation on refresh

- Refresh flow generates new refresh token, validates current session, rotates stored hash, updates last-used timestamp.
- Why: replay resistance and stronger long-lived session security.

### Device-bound refresh validation

- Refresh rotation checks token hash + `device_id` + user ownership.
- Why: makes stolen token replay harder across devices.

### Logout revokes current device session and clears cookies

- Logout revokes matching session and clears access/refresh cookies.
- Why: immediate local sign-out plus backend session invalidation.

## 4) Session management features (user-facing)

### List all current user sessions

- Endpoint returns session summaries ordered by last used.
- Why: account transparency and security hygiene.

### Revoke one session by id

- User can revoke a specific owned session.
- Why: targeted device logout for compromise/lost device scenarios.

### Revoke all other sessions (keep current device)

- Bulk revoke for all sessions except provided device.
- Why: one-click containment after suspicious activity.

## 5) OTP engine and anti-abuse logic

### OTP over email and phone

- Separate sender implementations for email and SMS paths.
- Why: multi-channel verification support.

### Purpose-scoped OTP

- OTP purpose enum includes `SIGNUP`, `LOGIN`, `PASSWORD_RESET`, `TWO_FACTOR_AUTH`.
- Why: prevents cross-flow OTP reuse and enables future security flows.

### OTP hashing and lifecycle controls

- OTP stored hashed (BCrypt), tracks attempts, consumed state, expiry.
- Why: protects secrets at rest and enforces single-use semantics.

### Verification outcome semantics

- Returns explicit statuses (`SUCCESS`, `INVALID_OTP`, `EXPIRED_OTP`, `ALREADY_USED`, `MAX_ATTEMPT_EXCEEDED`,
  `INVALID_PURPOSE`).
- Why: deterministic UX and safer backend handling.

### OTP request rate controls

- Enforces resend cooldown and request-window limits per identifier/type/purpose.
- Why: protects against brute-force/spam and messaging abuse.

## 6) Rate limiting (global endpoint-level)

### Redis-backed distributed token buckets (Bucket4j)

- Rate-limit buckets are stored via Redis proxy manager (Lettuce + Bucket4j).
- Why: works consistently across multiple app instances (not JVM-local in-memory only).

### Endpoint pattern -> rate-limit type mapping

- Config maps endpoint patterns (`signup`, `login`, OTP endpoints) to typed rules.
- Why: different risk profiles can have different capacity/refill policies.

### Auth-aware key resolution

- Uses authenticated user id when available, otherwise client IP.
- Why: fair limiting for logged-in users while protecting unauthenticated surfaces.

### Rate-limit response metadata

- Adds `X-RateLimit-Remaining`, `X-RateLimit-Capacity`, and `Retry-After` headers.
- Why: clients can adapt behavior and show user-friendly feedback.

### Micrometer counters for rate-limit outcomes

- Per-type `rate_limit.blocked` and `rate_limit.success` counters.
- Why: observable abuse patterns and policy tuning.

### Fail-open behavior if Redis check fails

- Allows request on limiter backend errors.
- Why: availability-first tradeoff (documented in code).

## 7) Security hardening and web protection

### Security headers

- Configures CSP (`default-src 'self'`), HSTS, and frame options.
- Why: baseline browser hardening for common web threats.

### Method security enabled

- `@EnableMethodSecurity` plus endpoint auth constraints.
- Why: defense in depth beyond route-level rules.

### BCrypt password encoder

- Password hashing with `BCryptPasswordEncoder`.
- Why: strong password storage baseline.

### CORS for production + local origins

- Allows local dev patterns plus deployed frontend domains with credentials support.
- Why: secure cross-origin cookie auth and usable dev workflow.

## 8) Error handling and API contract consistency

### Global exception-to-response mapping

- `ApplicationExceptionHandler` converts validation/business/auth/system errors to consistent API response shape.
- Why: predictable client behavior and easier integration.

### Filter-level exception handler

- `ExceptionHandlerFilter` catches `BusinessException` thrown before controller layer.
- Why: keeps rate-limit/JWT filter errors in same JSON response contract.

### Structured validation errors

- Field-level validation errors are mapped to a list in response.
- Why: frontend can highlight exact invalid inputs.

## 9) Data model and persistence design

### JPA entities with base auditing

- Core models include `AppUser`, `Session`, `Role`, `Permission`, `OtpToken` with auditing base fields.
- Why: maintainable domain design and traceability.

### Repository queries optimized for auth/session flows

- Includes lookup by identifier, status projection, session ownership checks, bulk revoke query, and cleanup queries.
- Why: explicit and efficient behavior for auth-critical paths.

## 10) Operational readiness

### Scheduled cleanup jobs

- Periodically deletes expired OTPs, stale unverified users, expired sessions, and old revoked sessions.
- Why: data hygiene, reduced storage bloat, and lower security risk from stale artifacts.

### OpenAPI with cookie + bearer schemes

- API docs define bearer and cookie schemes (`access_token`, `refresh_token`) and server metadata.
- Why: better onboarding and accurate auth docs for UI/tools.

### Actuator + Prometheus exposure

- Health/metrics/prometheus endpoints configured.
- Why: production monitoring and SRE visibility.

### AWS SES/SNS integrations

- SES and SNS dependencies/config are present with dedicated sender services.
- Why: real-world delivery infrastructure for OTP.

### CVE-conscious dependency management

- Dependency management pins secure versions for known vulnerable transitive libraries.
- Why: proactive supply-chain security posture.

## 11) Frontend auth/security features

### Central API service with cookie credentials

- All requests include credentials and standardized error envelopes.
- Why: consistent client behavior for cookie-based auth.

### Automatic refresh retry on 401

- API service attempts one refresh and retries original request.
- Why: seamless session continuity for users.

### Device ID generation + persistence

- Browser generates/stores `deviceId` via `crypto.randomUUID()`.
- Why: device-aware session ownership, revoke-others, and refresh binding.

### Route protection and boot-time auth check

- Protected routes wait for auth check; context probes session endpoint on startup.
- Why: prevents flashing protected pages and keeps auth state synchronized.

### Session-aware dashboard UI

- Dashboard fetches sessions, shows stats, supports revoke one/revoke others.
- Why: gives users direct control of account/device security.

### Client-side rate-limit UX component

- Renders remaining quota and countdown from response headers.
- Why: reduces user frustration and repeated abuse retries.

## 12) Testing and quality gates

### Unit tests across core layers

- Tests exist for controllers, services, security utilities, repositories, resolver, rate-limit components.
- Why: confidence in auth-critical logic and regressions detection.

### Integration tests with Testcontainers support

- Integration flows cover OTP and session behaviors with docker-availability gating.
- Why: realistic environment coverage for DB/infra dependent flows.

### Test profile data seeding

- Test initializer seeds role/user/session fixtures.
- Why: deterministic integration scenarios and easier endpoint verification.

---

## Quick “small-but-important” features checklist

- JWT contains roles claim for authorization context.
- Token-type claim prevents misuse of refresh token as access token.
- Refresh token rotation updates stored hash every refresh.
- Session revoke supports single session and revoke-all-others.
- Session lookup validates ownership (`sessionId + userId`).
- OTP is hashed, attempt-limited, expiry-limited, and purpose-scoped.
- OTP requests have cooldown + request-window enforcement.
- Rate limiting is distributed via Redis (not local in-memory only).
- Rate-limit headers are surfaced to frontend and rendered in UI.
- Filter-layer and controller-layer errors both return unified response shape.
- Scheduled cleanup removes expired/stale security artifacts.
- OpenAPI includes cookie auth schemes in addition to bearer.

# Dragon of North — Complete Feature Inventory (Major + Minor)

This document lists the implemented features in the repository, including smaller engineering decisions and **why** they
are useful.

---

## 1) Authentication and Account Lifecycle

### 1.1 Identifier-based authentication (email + phone)

- **What**: The backend supports identifier flows through a resolver and dedicated auth services for email and phone
  identifiers.
- **Why**: Keeps authentication extensible and avoids hard-coding one identifier strategy.

### 1.2 Account lifecycle states

- **What**: Explicit user states (`NOT_EXIST`, `CREATED`, `VERIFIED`, `DELETED`) are used in signup/status APIs.
- **Why**: Prevents invalid transitions and makes onboarding logic deterministic.

### 1.3 Signup split into start + complete

- **What**: `/identifier/sign-up` creates user flow, while `/identifier/sign-up/complete` finalizes verification status.
- **Why**: Encourages a safer “verify before full activation” flow.

### 1.4 Credential login via Spring Security

- **What**: Login uses `AuthenticationManager` with `UsernamePasswordAuthenticationToken`.
- **Why**: Reuses Spring Security primitives and centralizes authentication behavior.

### 1.5 Default role assignment on first auth lifecycle

- **What**: Users without roles get a `USER` role assignment from `RoleRepository`.
- **Why**: Ensures minimum permissions and predictable authorization defaults.

### 1.6 Controlled status transitions

- **What**: Status update logic only allows `CREATED -> VERIFIED` and blocks already-verified users.
- **Why**: Prevents illegal account-state mutations.

---

## 2) JWT, Cookies, and Session Security

### 2.1 Stateless security architecture

- **What**: Spring Security is configured with `SessionCreationPolicy.STATELESS` and CSRF disabled for REST token flow.
- **Why**: Supports horizontal scalability and keeps auth state outside server memory.

### 2.2 Access + Refresh token pair

- **What**: Login issues both access and refresh JWT tokens.
- **Why**: Short-lived access tokens reduce risk, refresh preserves UX.

### 2.3 Token typing in claims

- **What**: Tokens include `token_type` claims and validation checks enforce expected type.
- **Why**: Prevents misuse of refresh tokens as access tokens.

### 2.4 RSA-based JWT signing + issuer validation

- **What**: JWTs are signed with private key, validated with public key, and issuer checked.
- **Why**: Strong signature security and stricter token trust boundary.

### 2.5 Role claims in access token

- **What**: Access token stores role names and `JwtFilter` maps them to Spring authorities.
- **Why**: Enables role-based authorization without DB lookup per request.

### 2.6 Cookie-based token delivery (HTTP-only, secure)

- **What**: Access and refresh cookies are set `HttpOnly`, `Secure`, and `SameSite=None`.
- **Why**: Reduces XSS exposure and supports cross-site frontend/backend setups.

### 2.7 Refresh token rotation

- **What**: Refresh endpoint generates a **new refresh token**, validates old session, rotates stored hash, and returns
  new access+refresh cookies.
- **Why**: Limits replay window if a refresh token leaks.

### 2.8 Session-backed refresh validation

- **What**: Refresh is tied to DB session checks (`revoked`, `expired`, token hash, device ID).
- **Why**: Gives server-side revocation control over JWT-based auth.

### 2.9 Logout revokes session and clears cookies

- **What**: Logout revokes current session by refresh token + device ID and clears auth cookies.
- **Why**: Explicit device logout rather than relying only on token expiry.

### 2.10 Refresh tokens stored as hashes

- **What**: Raw refresh tokens are SHA-256 hashed before persistence.
- **Why**: Reduces blast radius if DB is compromised.

---

## 3) Session Management (Multi-device)

### 3.1 Session metadata capture

- **What**: Session stores `deviceId`, `ipAddress`, `userAgent`, `lastUsedAt`, `expiryDate`, `revoked`.
- **Why**: Enables security visibility and anomaly detection per device.

### 3.2 One active session record per user/device pair

- **What**: Existing session for same user+device is replaced on new login.
- **Why**: Keeps session data clean for each device identity.

### 3.3 User session APIs

- **What**: Implemented endpoints for listing sessions, revoking one session, and revoking all others.
- **Why**: Gives users direct device/session control.

### 3.4 Session cleanup job

- **What**: Scheduled cleanup removes expired sessions and old revoked sessions.
- **Why**: Controls table growth and keeps session storage healthy.

---

## 4) OTP Engine and Verification Hardening

### 4.1 Multi-channel OTP

- **What**: OTP can be sent via email or phone sender abstraction.
- **Why**: Better channel flexibility and service decoupling.

### 4.2 Purpose-aware OTP

- **What**: OTP supports `SIGNUP`, `LOGIN`, `PASSWORD_RESET`, `TWO_FACTOR_AUTH` purposes.
- **Why**: Prevents cross-use of OTPs between different security actions.

### 4.3 OTP hashed with BCrypt

- **What**: OTP values are stored as BCrypt hashes.
- **Why**: Avoids storing plaintext OTP in DB.

### 4.4 OTP verification outcomes as enums

- **What**: Detailed status values (`SUCCESS`, `INVALID_OTP`, `EXPIRED_OTP`, `ALREADY_USED`, etc.).
- **Why**: Consistent API behavior and easier UI logic.

### 4.5 Anti-abuse checks in OTP service

- **What**: Cooldown window, max requests/window, max verify attempts, expiry checks.
- **Why**: Reduces brute-force and OTP spam abuse.

### 4.6 OTP entity with optimistic locking and indexes

- **What**: `@Version` field and indexes on identifier/type/time columns.
- **Why**: Improves concurrency safety and query efficiency.

### 4.7 OTP cleanup job

- **What**: Scheduled cleanup removes expired OTPs.
- **Why**: Keeps OTP table efficient and compliant with ephemeral-token behavior.

---

## 5) Rate Limiting and Abuse Prevention

### 5.1 Distributed rate limiting with Redis + Bucket4j

- **What**: Bucket state is backed by Redis through Bucket4j proxy manager.
- **Why**: Rate limits remain consistent across multiple app instances.

### 5.2 Endpoint-specific limit profiles

- **What**: Different rule types for signup/login/otp endpoints from configuration.
- **Why**: Risk-based throttling (high-risk endpoints get tighter control).

### 5.3 Dynamic endpoint matching

- **What**: Filter matches configured Ant patterns to rate-limit types.
- **Why**: Add/change guarded endpoints without code rewrite.

### 5.4 User-or-IP key strategy

- **What**: Rate-limit key uses authenticated user when present, IP fallback when not.
- **Why**: Protects anonymous flows while preserving fair per-user throttling.

### 5.5 Rate-limit headers in responses

- **What**: Returns `X-RateLimit-Remaining`, `X-RateLimit-Capacity`, and `Retry-After`.
- **Why**: Allows frontend and clients to adapt behavior proactively.

### 5.6 Micrometer counters for blocked/success requests

- **What**: Separate counters per `RateLimitType` for blocked and successful checks.
- **Why**: Improves observability and threshold tuning.

### 5.7 Explicit fail-open behavior on limiter backend errors

- **What**: If Redis/rate-limit check fails, request is currently allowed.
- **Why**: Favors availability; design is documented in code comments.

---

## 6) API Security and Platform Hardening

### 6.1 Custom JWT filter supports header and cookie token extraction

- **What**: Reads bearer token from header; falls back to `access_token` cookie.
- **Why**: Supports browser and tool/client use cases.

### 6.2 Public endpoint whitelist + protected defaults

- **What**: Auth/OTP/docs/health endpoints are public; all others require auth.
- **Why**: Secure-by-default request policy.

### 6.3 Security headers

- **What**: CSP default-src self, HSTS, frame options configured.
- **Why**: Adds transport/browser security baseline.

### 6.4 CORS wiring in security chain

- **What**: Uses configured `CorsConfigurationSource` in security filter chain.
- **Why**: Enables controlled cross-origin frontend integration.

---

## 7) Error Handling and API Contracts

### 7.1 Centralized exception mapping

- **What**: `@RestControllerAdvice` maps business/auth/validation/runtime errors to standard response shape.
- **Why**: Predictable client error handling.

### 7.2 Validation error details

- **What**: Field-level validation details are included for invalid request payloads.
- **Why**: Better DX and frontend form error rendering.

### 7.3 Exception handling filter

- **What**: Filter-level exception translation is implemented.
- **Why**: Captures exceptions occurring before controller layer.

### 7.4 Structured `ApiResponse` envelope

- **What**: Success/failure wrappers are used consistently in controllers.
- **Why**: Uniform API contract across endpoints.

---

## 8) Data, Persistence, and Domain Modeling

### 8.1 PostgreSQL + Spring Data JPA

- **What**: Relational storage for users/roles/sessions/otps with repositories.
- **Why**: Strong consistency and rich querying for auth workloads.

### 8.2 Audit-friendly base entity model

- **What**: Core entities extend shared base model and use auditing support.
- **Why**: Standardized timestamps/metadata.

### 8.3 Role bootstrap on startup

- **What**: Startup initializer ensures all `RoleName` values exist in DB.
- **Why**: Prevents missing-role failures in auth flows.

### 8.4 Optional test data initializer

- **What**: Project includes test data initializer configuration.
- **Why**: Speeds local/integration environment setup.

---

## 9) Observability and Operations

### 9.1 Actuator + Prometheus exposure

- **What**: Health, metrics, and Prometheus endpoints are exposed.
- **Why**: Production monitoring readiness.

### 9.2 OpenAPI documentation with cookie + bearer schemes

- **What**: Swagger/OpenAPI includes both bearer and cookie security schemes.
- **Why**: Easier testing and clearer consumer docs for mixed clients.

### 9.3 Dockerized backend runtime

- **What**: Repo includes Dockerfile and environment-driven configuration.
- **Why**: Reproducible deployments.

### 9.4 Environment-driven secrets/config

- **What**: `.env`-backed configuration for DB, JWT keys, Redis, AWS, throttling.
- **Why**: Safer config separation between code and runtime values.

---

## 10) Frontend Capabilities

### 10.1 Protected route guard

- **What**: Protected pages require auth context state.
- **Why**: Prevents accidental UI access without authentication.

### 10.2 Auth context with persistence

- **What**: Auth state and user snapshot are persisted in local storage.
- **Why**: Better UX across refreshes.

### 10.3 Automatic refresh on 401

- **What**: API service retries once after refresh; if refresh fails, clears auth state and redirects.
- **Why**: Smoother token-expiry handling.

### 10.4 Refresh de-duplication

- **What**: Concurrent refresh attempts are collapsed using `isRefreshing` + shared promise.
- **Why**: Avoids refresh storms.

### 10.5 Device identity generation

- **What**: Frontend generates/stores stable device UUID and sends it to login/refresh/logout/revoke-others.
- **Why**: Enables session-level control by device.

### 10.6 Session management in dashboard

- **What**: Dashboard loads sessions, shows counts, revokes single session, and revokes others.
- **Why**: Gives users visible and actionable account security controls.

### 10.7 Live rate-limit UX component

- **What**: Frontend listens for rate-limit headers and shows remaining quota/block countdown.
- **Why**: Improves user behavior and reduces repeated 429s.

---

## 11) Integrations

### 11.1 AWS SES/SNS integration points

- **What**: SES/SNS configuration and sender services are implemented for OTP dispatch.
- **Why**: Supports real delivery infrastructure beyond local mocks.

### 11.2 Redis integration

- **What**: Redis is used for distributed rate-limiter state (not in-memory limiter state).
- **Why**: Works correctly in multi-instance deployments.

---

## 12) Testing and Quality Coverage

### 12.1 Layered tests across app

- **What**: Repository, service, controller, config, exception, and resolver tests exist.
- **Why**: Broad regression safety net.

### 12.2 Integration tests with Testcontainers support

- **What**: Integration test base and Docker-availability condition are present; OTP and session flows are covered.
- **Why**: Validates realistic app behavior beyond unit mocking.

### 12.3 Security and rate-limit specific tests

- **What**: Dedicated tests for JWT services/filter and rate-limit components.
- **Why**: Protects critical auth/abuse-prevention paths.

---

## Quick “Why these choices matter” summary

- **JWT stateless + refresh rotation + server-side session checks** gives both scalability and revocation control.
- **Redis-backed distributed rate limiting** is production-friendly compared to in-memory-only throttling.
- **Device-aware session APIs** improve real-world account security.
- **Purpose-aware OTP with strict verification statuses** prevents workflow confusion and reuse attacks.
- **Structured error responses + tests** improve reliability and frontend integration speed.
