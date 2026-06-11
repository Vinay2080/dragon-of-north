<!-- ═══════════════════════════════════════════════════════════════ -->
<!--                   DRAGON OF NORTH — README                      -->
<!-- ═══════════════════════════════════════════════════════════════ -->

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0D0D12,50:1a1040,100:6B57FF&height=200&section=header&text=Dragon%20of%20North&fontSize=48&fontColor=ffffff&fontAlignY=52&desc=Production%20authentication%20infrastructure.%20Not%20a%20tutorial%20project.&descAlignY=70&descSize=14&descColor=9d8fff" width="100%"/>

<br>

<p align="center">
  <sub>
    Dragon of North is an IAM-focused backend platform for identity onboarding, session security, MFA orchestration,
    token lifecycle control, abuse resistance, and security telemetry.
    It is designed around the real failure modes of authentication systems: replay, refresh races, device takeover,
    stale sessions, weak recovery paths, and distributed brute-force pressure.
  </sub>
</p>

<!-- ─── IMPACT STRIP ─────────────────────────────────────────────── -->

<div align="center">
  <b>JWT + RSA-2048 &nbsp;·&nbsp; Device-Bound Sessions &nbsp;·&nbsp; Refresh Rotation &nbsp;·&nbsp; MFA + Step-Up &nbsp;·&nbsp; Redis Abuse Controls</b>
</div>

<br>

<!-- ─── TYPING ANIMATION ──────────────────────────────────────────── -->

<p align="center">
  <img src="https://readme-typing-svg.herokuapp.com?font=JetBrains+Mono&weight=500&size=16&duration=2500&pause=900&color=6B57FF&center=true&vCenter=true&width=760&lines=Identity+infrastructure%2C+not+just+login+APIs.;MFA+orchestration+%C2%B7+Step-up+auth+%C2%B7+Session+liveness;Refresh+rotation+%C2%B7+CSRF+%C2%B7+Redis+rate+limits;89+Java+test+classes+%C2%B7+13+k6+scenario+scripts;This+is+what+production-minded+auth+looks+like." />
</p>

<br>

<p align="center">
  <a href="https://app.verloren.dev">
    <img src="https://img.shields.io/badge/%F0%9F%8C%90_Live_App-app.verloren.dev-6B57FF?style=for-the-badge">
  </a>
  &nbsp;&nbsp;
  <a href="https://api.verloren.dev/swagger-ui/index.html">
    <img src="https://img.shields.io/badge/%F0%9F%93%96_API_Docs-Swagger_UI-85EA2D?style=for-the-badge">
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white">
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white">
  <img src="https://img.shields.io/badge/PostgreSQL_16-336791?style=flat-square&logo=postgresql&logoColor=white">
  <img src="https://img.shields.io/badge/Redis_7-DC382D?style=flat-square&logo=redis&logoColor=white">
  <img src="https://img.shields.io/badge/AWS_SES%2FSNS-FF9900?style=flat-square&logo=amazonaws&logoColor=white">
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white">
  <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white">
  <img src="https://img.shields.io/badge/License-MIT-22c55e?style=flat-square">
</p>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:6B57FF,100:0D0D12&height=3" width="100%"/>

<br>

# Project Overview

Dragon of North is a backend Identity and Access Management platform implemented in Java 21 and Spring Boot 3. It
combines stateless access-token authorization with authoritative server-side session state, giving API clients the
scalability of JWTs without giving up revocation, device tracking, MFA freshness, or refresh-token replay control.

The platform supports local email and phone identity flows, Google OAuth, passwordless email-link login, TOTP-based MFA,
one-time recovery codes, sensitive-operation step-up verification, device-aware sessions, refresh-token rotation,
Redis-backed rate limiting, CSRF protection for browser clients, audit logging, Prometheus metrics, Docker
infrastructure, Flyway migrations, integration tests, and k6 load scenarios.

This is intentionally documented as an IAM system rather than a generic Spring Boot app: the important work is not only
whether a user can log in, but how identity proof, session continuity, factor verification, token transport, revocation,
and abuse control interact under production constraints.

# Identity Capabilities

```text
Authentication
├── Email + Password
├── Phone + Password
├── Google OAuth
├── Passwordless Email Link
├── Password Reset via OTP
└── Identifier Status + Signup Completion

Multi-Factor Authentication
├── TOTP Setup and Verification
├── One-Time Recovery Codes
├── Login-Time MFA Challenge
└── Step-Up Verification for Sensitive Operations

Session Security
├── Device Tracking
├── Persistent Session Records
├── Refresh Token Rotation
├── Current Session Revocation
├── Targeted Session Revocation
├── Revoke Other Devices
└── Configurable SID Liveness Enforcement

Security Controls
├── Redis + Bucket4j Rate Limiting
├── Cookie Auth + CSRF Token Synchronization
├── RSA-Signed Access and Refresh JWTs
├── BCrypt Password, OTP, and Recovery-Code Hashing
├── AES-GCM Encryption for MFA Secrets
├── Security Audit Events
└── Actuator / Prometheus Observability
```

# Feature Matrix

| Capability                             | Status |
|----------------------------------------|--------|
| Email + password authentication        | ✓      |
| Phone + password authentication        | ✓      |
| Google OAuth login and signup          | ✓      |
| Passwordless email-link login          | ✓      |
| JWT access tokens                      | ✓      |
| RSA JWT signing                        | ✓      |
| HttpOnly cookie token transport        | ✓      |
| Bearer access-token support            | ✓      |
| Refresh token rotation                 | ✓      |
| Hashed refresh-token storage           | ✓      |
| Device-aware persistent sessions       | ✓      |
| Current-session logout                 | ✓      |
| Targeted session revocation            | ✓      |
| Revoke all other sessions              | ✓      |
| Configurable SID liveness checks       | ✓      |
| MFA with TOTP                          | ✓      |
| MFA recovery codes                     | ✓      |
| Step-up MFA for sensitive operations   | ✓      |
| Redis-backed MFA challenge state       | ✓      |
| Redis-backed passwordless token state  | ✓      |
| Redis-backed distributed rate limiting | ✓      |
| CSRF protection for cookie clients     | ✓      |
| Audit logging                          | ✓      |
| Micrometer / Prometheus metrics        | ✓      |
| Testcontainers integration testing     | ✓      |
| k6 load-test scenarios                 | ✓      |
| Role and permission data model         | ✓      |

# Project Scale

| Measure                                       | Count |
|-----------------------------------------------|------:|
| Domain modules under `modules/`               |     5 |
| Concrete JPA entities                         |     9 |
| Shared audited base entity                    |     1 |
| Mapped controller methods                     |    31 |
| Primary authentication and continuation flows |     8 |
| Documented security mechanisms                |    14 |
| Java test classes                             |    89 |
| Testcontainers-backed integration tests       |     5 |
| k6 scenario scripts                           |    13 |
| Flyway migration files                        |    22 |

Domain modules are `auth`, `otp`, `profile`, `session`, and `user`. Infrastructure concerns such as `security`,
`ratelimit`, `infrastructure`, and `shared` are intentionally separated from business modules.

# Architecture Overview

The backend uses a layered architecture with thin controllers, service-level orchestration, Spring Data repositories,
and dedicated security filters/interceptors.

```text
Client
  |
  v
RateLimitFilter
  |
  v
Spring Security
  |
  +--> CSRF validation and CSRF cookie refresh
  +--> JwtFilter extracts Bearer/cookie access tokens
  +--> SidLivenessFilter optionally validates session liveness
  |
  v
Controller Layer
  |
  v
Service Layer
  |
  +--> Auth orchestration
  +--> MFA challenge orchestration
  +--> Session/token issuance
  +--> OTP/passwordless/OAuth services
  |
  v
Persistence and Infrastructure
  |
  +--> PostgreSQL for users, roles, sessions, OTPs, MFA settings
  +--> Redis for rate-limit buckets, MFA challenges, MFA setup state, passwordless links
  +--> AWS SES/SNS for email and phone delivery
  +--> Micrometer/Prometheus for telemetry
```

## Request Lifecycle

1. Requests enter the `RateLimitFilter`, which maps endpoint patterns to risk-specific token buckets.
2. Spring Security applies CORS, CSRF, exception handling, public-route rules, and stateless session policy.
3. `JwtFilter` accepts `Authorization: Bearer` or `access_token` cookies, validates RSA-signed access JWTs, rejects
   non-access token types, and builds a `SecurityPrincipal`.
4. `SidLivenessFilter` can verify that the access-token `sid` still maps to a live database session, depending on
   configured enforcement mode.
5. Sensitive endpoints annotated with `@SensitiveAccountOperation` / `@RequireRecentMfa` pass through
   `RecentMfaEnforcementInterceptor`.
6. Controllers delegate to services; services emit audit events, metrics, and persistence changes.

# Authentication Architecture

Dragon of North has multiple primary authentication entry points, but they converge into common session issuance and MFA
orchestration. This avoids a common security smell where OAuth, passwordless, and password login accidentally mint
different token shapes or bypass MFA policy.

## Local Identifier Authentication

Email and phone identifiers are routed through `AuthenticationServiceResolver`. Local login normalizes the identifier,
verifies account lifecycle state, ensures the account is registered with the local provider, authenticates through
Spring Security, checks identifier verification, and then delegates to `AuthCommonServiceImpl.issueLoginSession`.

Signup is split into lifecycle stages:

```text
Identifier Status
  |
  v
Sign Up
  |
  v
OTP Request + Verify
  |
  v
Sign Up Complete
  |
  v
Verified Account
```

This split lets the system represent accounts that are created but not yet verified, and lets cleanup jobs remove stale
unverified users.

## Login Flow

```text
Client                    Auth API                  Auth Services                 DB / Redis
  |                          |                            |                            |
  |-- identifier/login ----->|                            |                            |
  |   identifier,password,   |                            |                            |
  |   device_id              |                            |                            |
  |                          |-- normalize identifier --->|                            |
  |                          |-- validate user state ---->| users/auth_provider        |
  |                          |-- authenticate password -->| BCrypt + UserDetails       |
  |                          |-- check MFA policy ------->| user_mfa_settings          |
  |                          |                            |                            |
  |                          |<-- no MFA required --------|                            |
  |                          |-- create session --------->| user_sessions              |
  |                          |-- hash refresh token ----->| user_sessions              |
  |                          |-- mint access JWT -------->| RSA private key            |
  |<-- HttpOnly cookies -----|                            |                            |
  |    access_token          |                            |                            |
  |    refresh_token         |                            |                            |
```

If MFA is enabled, the flow branches before session creation. No application session is issued until the MFA challenge
succeeds.

# MFA Architecture

MFA is implemented as an orchestration system, not as a boolean check embedded inside login. The core components are:

| Component                         | Responsibility                                                          |
|-----------------------------------|-------------------------------------------------------------------------|
| `MfaOrchestrator`                 | Decides whether login needs a challenge after primary authentication.   |
| `MfaProviderRegistry`             | Resolves enabled providers for a user.                                  |
| `TotpMfaProvider`                 | Verifies encrypted TOTP secrets through `TotpService`.                  |
| `RecoveryCodeMfaProvider`         | Verifies and consumes one-time recovery codes.                          |
| `MfaChallengeServiceImpl`         | Creates, stores, locks, verifies, and consumes Redis-backed challenges. |
| `RecentMfaEnforcementInterceptor` | Enforces recent MFA for sensitive operations.                           |

## TOTP Flow

MFA setup is a two-step process. The system generates a TOTP secret, encrypts it, stores it temporarily in Redis for 5
minutes, and returns QR bootstrap material. The user confirms setup with an authenticator code. Only after a valid code
does the system persist encrypted TOTP settings, mark the user as MFA-enabled, and generate recovery codes.

```text
Authenticated User              MFA Service                      Redis / DB
      |                              |                                |
      |-- enable/mfa/request ------>|                                |
      |                              |-- generate TOTP secret --------|
      |                              |-- encrypt temporary secret ---->| Redis TTL 5m
      |<-- secret + QR code --------|                                |
      |                              |                                |
      |-- enable/mfa/confirm ------>|                                |
      |   authenticator code         |                                |
      |                              |-- get+delete setup secret ---->| Redis
      |                              |-- decrypt and verify TOTP -----|
      |                              |-- persist encrypted secret ---->| user_mfa_settings
      |                              |-- generate recovery codes ---->| user_mfa_recovery_codes
      |<-- one-time recovery codes --|                                |
```

## MFA Challenge Flow

```text
Primary Auth Succeeds             MFA Orchestrator              Redis Challenge Store
        |                                |                              |
        |-- user has MFA enabled ------>|                              |
        |                                |-- resolve providers -------->|
        |                                |   TOTP, RECOVERY_CODE        |
        |                                |-- create opaque challenge -->| state + TTL
        |<-- challenge_id + methods -----|                              |
        |                                                               |
Client submits provider + code                                          |
        |                                                               |
        |------------------------------->|-- claim challenge ---------->| short lock
        |                                |-- validate binding --------->| device/ip prefix/UA
        |                                |-- verify provider ---------->| TOTP or recovery
        |                                |-- consume challenge -------->| delete/consume
        |<-- session cookies ------------|-- create session ---------->| PostgreSQL
```

Challenge state is bound to request context: device ID, IP prefix, user-agent hash, primary AMR, allowed providers,
attempts, timestamps, and optional session ID for step-up challenges. Verification uses a short Redis claim lock to
reduce replay and concurrent-consume races.

## Recovery Code Flow

Recovery codes are generated during MFA setup and returned once. They are normalized, BCrypt-hashed, stored
individually, and consumed atomically with `consumeIfUnused`. Existing active codes are invalidated when a new set is
generated.

Recovery codes are allowed for login-time MFA but intentionally not allowed for step-up verification. This preserves
recovery as an account-access fallback while keeping high-risk authenticated operations on stronger live TOTP proof.

## Step-Up Authentication

Sensitive operations are protected by `@SensitiveAccountOperation`, which wraps `@RequireRecentMfa`. Implemented
sensitive routes include password change, account deletion, targeted session revocation, revoke-other-sessions, and the
example protected step-up action.

```text
Authenticated Client          Sensitive Endpoint           Recent MFA Interceptor          Step-Up API
       |                              |                              |                         |
       |-- password/change ---------->|                              |                         |
       |                              |-- check annotation ---------->|                         |
       |                              |-- load live session --------->|                         |
       |                              |-- MFA stale/missing ----------|                         |
       |<-- 403 + challenge payload --|<-- issue challenge -----------|                         |
       |                                                                                        |
       |-- step-up/mfa/verify --------------------------------------------------------------->|
       |   challenge_id, TOTP, device_id                                                       |
       |                                                                                        |
       |<-- refreshed access cookie ----------------------------------------------------------|
       |                                                                                        |
       |-- retry sensitive action --->|-- recent MFA satisfied ------>|                         |
       |<-- success ------------------|                              |                         |
```

On successful step-up, the system updates `mfa_verified_at` and `mfa_method_amr` on the existing session and mints a new
access token from the authoritative session row. It does not create a new session and does not issue a temporary
elevation token.

## MFA Threat Model

| Threat                              | Control                                                                                |
|-------------------------------------|----------------------------------------------------------------------------------------|
| Stolen primary credentials          | MFA challenge required before session issuance when MFA is enabled.                    |
| Stolen challenge token              | Challenge is bound to device ID, IP prefix, user-agent hash, and optional session ID.  |
| Challenge replay                    | Redis claim lock and consume-on-success semantics.                                     |
| Brute-forced MFA codes              | Per-endpoint Redis rate limits plus challenge attempt tracking and lockout.            |
| Database disclosure of MFA material | TOTP secret is encrypted; recovery codes are BCrypt-hashed.                            |
| Recovery-code reuse                 | Recovery codes are consumed atomically and marked used.                                |
| Weak step-up fallback               | Recovery codes are not allowed for step-up; TOTP is required for recent-MFA elevation. |

## MFA Trade-Offs

The system chooses Redis-backed challenge state instead of encoding all challenge state into a signed token. That adds
infrastructure dependency, but it enables server-side lockout, replay detection, context binding, and immediate
invalidation.

TOTP uses the standard 6-digit, 30-second authenticator-app model. This is broadly interoperable, but it still depends
on user device security and time synchronization. Recovery codes improve account continuity, but they are deliberately
single-use and excluded from step-up to reduce their value after disclosure.

# Session Management

Sessions are first-class security records, not incidental refresh-token storage. Each login creates a `user_sessions`
row containing user, refresh-token hash, device ID, IP address, user agent, expiry time, last-used timestamp, revoked
flag, MFA verification timestamp, primary AMR, and MFA method AMR.

## Session Lifecycle

```text
Primary Authenticated
        |
        v
MFA Required?
   | yes                              | no
   v                                  v
Redis MFA Challenge              Create Session
   |                                  |
   v                                  v
MFA Verified                     Hash Refresh Token
   |                                  |
   v                                  v
Create Session ---------------> Persist Session Row
        |
        v
Mint Access Token with sid + amr + mfa facts
        |
        v
Issue HttpOnly access_token and refresh_token cookies
        |
        v
Refresh / Access / Revoke / Expire / Cleanup
```

## Session Creation

`SessionTokenIssuer` generates a refresh JWT, asks `SessionServiceImpl` to persist the session, and then mints an access
token from the persisted session. This sequencing matters: the access token includes the `sid` claim, MFA facts, AMR
values, and roles derived from the authoritative session.

The system maintains one active session per user-device pair by deleting an existing session for the same
`AppUser + deviceId` before inserting the replacement. Different device IDs can have concurrent sessions.

## Refresh Token Rotation Flow

```text
Client                         Auth API                 Session Service                  Database
  |                               |                            |                              |
  |-- jwt/refresh -------------->|                            |                              |
  |   refresh cookie + device_id  |                            |                              |
  |                               |-- validate token --------->| RSA public key                |
  |                               |-- generate new refresh --->|                              |
  |                               |-- hash old/new tokens ---->|                              |
  |                               |-- atomic update ---------->| where user + device + oldhash |
  |                               |                            | and live + not revoked        |
  |                               |<-- updated session --------|                              |
  |                               |-- mint new access JWT ---->| includes same sid             |
  |<-- new HttpOnly cookies ------|                            |                              |
```

Raw refresh tokens are never stored. The database stores only SHA-256 hashes. Refresh rotation is constrained by user
ID, device ID, old token hash, non-revoked state, non-deleted state, and expiry. A stale or replayed refresh token
cannot rotate the session because its hash no longer matches the live row.

## Session Revocation

Implemented revocation paths:

| Operation         | Endpoint / Trigger                           | Behavior                                                                                                                        |
|-------------------|----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| Current logout    | `POST /api/v1/auth/identifier/logout`        | Revokes current session by refresh token hash + device ID and clears cookies.                                                   |
| Targeted revoke   | `DELETE /api/v1/sessions/delete/{sessionId}` | Revokes a session owned by the authenticated user. Requires recent MFA when MFA is enabled.                                     |
| Revoke others     | `POST /api/v1/sessions/revoke-others`        | Revokes all other active sessions for the user except the submitted current device ID. Requires recent MFA when MFA is enabled. |
| Account deletion  | `POST /api/v1/auth/account/delete`           | Marks account deleted, revokes all sessions, clears cookies. Requires recent MFA when MFA is enabled.                           |
| Scheduled cleanup | `CleanupTask`                                | Deletes expired sessions and old revoked sessions after retention.                                                              |

## Device Awareness

Every session is tied to a client-provided `deviceId`. That device ID participates in session creation, refresh
rotation, logout, revoke-others, MFA challenge binding, rate-limit keys, and audit events. The model is device-aware
rather than device-trusted: the ID is useful for continuity and revocation, but it is not treated as a strong
authenticator by itself.

# JWT Security

Access and refresh tokens are both JWTs signed with RSA keys loaded through `KeyUtils`.

| Token         | Transport                                        | Claims / Purpose                                                                                               |
|---------------|--------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| Access token  | `Authorization: Bearer` or `access_token` cookie | `token_type=access_token`, subject user ID, roles, MFA status, `mfa_verified_at`, AMR list, session ID (`sid`) |
| Refresh token | `refresh_token` cookie                           | `token_type=refresh_token`, subject user ID                                                                    |

`JwtFilter` only accepts tokens with `token_type=access_token`. Refresh endpoints validate and rotate refresh tokens
separately. Access tokens are short-lived; refresh tokens are linked to persistent server-side session state through the
stored hash.

# OAuth Implementation

Google OAuth is implemented through `OAuthController`, `OAuthServiceImpl`, and `GoogleTokenVerifierService`.

```text
Client                    OAuth API                  Google Verifier              Local IAM
  |                          |                              |                         |
  |-- google id_token ------>|                              |                         |
  |   expected_identifier,   |                              |                         |
  |   device_id              |                              |                         |
  |                          |-- verify token ------------>| signature/issuer/aud    |
  |                          |<-- verified claims ----------| email_verified required |
  |                          |-- resolve provider link ----------------------------->|
  |                          |-- create/link/reactivate user ----------------------->|
  |                          |-- sync Google profile/avatar ----------------------->|
  |                          |-- shared MFA/session issuance ---------------------->|
  |<-- success or MFA challenge ------------------------------ cookies or challenge |
```

OAuth validates Google issuer, audience, email verification, optional expected identifier matching, and provider
identity linkage. Existing local accounts are not silently overwritten in signup conflict paths; the service requires
appropriate linking semantics. OAuth-created sessions use the same `SessionTokenIssuer`, MFA orchestration, cookies, and
audit patterns as local login.

# Passwordless Implementation

Passwordless login uses one-time email links. The request phase is enumeration-resistant: unknown emails do not receive
a distinct failure response. For known eligible users, the service generates a 32-byte random token, stores only its
hash in Redis with a TTL, invalidates any previous active passwordless token for the same user, and sends a link through
email.

```text
Client                 Passwordless API                  Redis                     Email
  |                          |                              |                         |
  |-- request(email) ------->|                              |                         |
  |                          |-- find user if exists ------>|                         |
  |                          |-- random token + hash ------>| token_hash -> user_id   |
  |                          |-- latest token wins -------->| user_id -> token_hash   |
  |                          |------------------------------------------------------>|
  |<-- generic success ------|                              | login link              |
  |                                                                              |
  |-- verify(token,device) ->|                              |                         |
  |                          |-- hash token lookup -------->|                         |
  |                          |-- delete token keys -------->| single use              |
  |                          |-- shared login completion -->| session/MFA             |
  |<-- success or MFA challenge ----------------------------|                         |
```

Passwordless verification delegates into `AuthCommonServices.completeLogin`, so MFA-enabled accounts still branch into
MFA challenge orchestration before session issuance.

# Authorization Model

The active request authorization model is:

1. Public authentication, OTP, OAuth, passwordless, Swagger, and selected actuator endpoints are explicitly listed in
   `SecurityConfig.public_urls`.
2. All other endpoints require an authenticated access token.
3. `SecurityPrincipal` carries user ID, authorities, MFA facts, session ID, and AMR values.
4. Roles and permissions are modeled with `Role`, `Permission`, and `role_permissions`; startup seeding creates `USER`
   and `ADMIN` roles.
5. `AppUserDetails` emits `ROLE_*` and `PERM_*` authorities.

Spring method security is enabled, but the current controller source does not use endpoint-specific `@PreAuthorize` role
gates. Authorization is therefore currently authentication-first, with role/permission infrastructure present for future
endpoint-level policy expansion.

# Security Controls

## CSRF Protection

The system supports browser cookie auth through `HttpOnly` `access_token` and `refresh_token` cookies. CSRF protection
uses `CookieCsrfTokenRepository` with an `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header. Pre-auth bootstrap and stateless
auth endpoints are explicitly bypassed, while protected state-changing requests remain under Spring Security CSRF
enforcement.

## Rate Limiting

`RateLimitFilter` maps endpoint patterns to `RateLimitType` policies and stores Bucket4j state in Redis through Lettuce.
Keys vary by risk profile:

| Type                                | Key Strategy                                     |
|-------------------------------------|--------------------------------------------------|
| Signup / Login / OTP / Passwordless | IP + device ID                                   |
| MFA verify                          | IP + challenge ID + device ID                    |
| Step-up request / verify            | user ID + session ID + IP                        |
| Refresh                             | IP + device ID + authenticated user if available |

The configured policies include login, signup, OTP, passwordless, MFA verify, step-up request, step-up verify, and
refresh. Failed rate-limit checks emit `X-RateLimit-*` headers, `Retry-After`, Micrometer counters, and security audit
events. The current implementation fails closed on rate-limit infrastructure errors by returning a blocked result.

## Redis Usage

Redis is used in four distinct identity paths:

| Usage                     | Implementation                                                                |
|---------------------------|-------------------------------------------------------------------------------|
| Distributed rate limiting | Bucket4j proxy manager backed by Redis.                                       |
| MFA setup                 | Encrypted temporary TOTP setup secret with 5-minute TTL.                      |
| MFA challenges            | Opaque challenge state, claim locks, attempt tracking, and consume semantics. |
| Passwordless login        | Token-hash-to-user mapping and latest-token-per-user mapping with TTL.        |

## Audit Logging

The codebase uses two audit mechanisms:

| Mechanism             | Purpose                                                                                                                       |
|-----------------------|-------------------------------------------------------------------------------------------------------------------------------|
| Spring Data auditing  | `BaseEntity` tracks created/updated timestamps, deleted flag, versioning, `createdBy`, and `updatedBy`.                       |
| Security audit events | `AuditEventLogger` records authentication, MFA, session, OTP, OAuth, passwordless, rate-limit, and suspicious-session events. |

Security events include contextual fields such as user ID, device ID, IP address, request ID, session ID where relevant,
outcome, reason, and provider/method information.

## Monitoring and Observability

Actuator exposes health, metrics, and Prometheus endpoints. The codebase emits Micrometer counters for login
success/failure, refresh success/failure, logout success/failure, OTP request/verify outcomes, MFA setup and step-up
outcomes, session revocation, and per-rate-limit success/blocked counts.

# Architecture Diagrams

## Refresh Token Rotation

```text
Old refresh token
       |
       v
Extract user ID from RSA-verified refresh JWT
       |
       v
Generate new refresh JWT
       |
       v
Hash old token + hash new token
       |
       v
UPDATE user_sessions
  SET refresh_token_hash = new_hash,
      last_used_at = now
WHERE user_id = token_subject
  AND device_id = request_device
  AND refresh_token_hash = old_hash
  AND revoked = false
  AND deleted = false
  AND expiry_date > now
       |
       +--> 1 row updated: mint access token + set cookies
       |
       +--> 0 rows updated: reject as invalid/replayed/stale
```

## Session Lifecycle

```text
CREATED
  |
  | login or MFA completion
  v
ACTIVE
  |
  +--> refresh rotation updates refresh hash + last_used_at
  |
  +--> step-up MFA updates mfa_verified_at + mfa_method_amr
  |
  +--> logout / revoke by id / revoke others / account delete
  v
REVOKED
  |
  | retained for configured forensic window
  v
CLEANED UP

ACTIVE
  |
  | expiry_date passes
  v
EXPIRED
  |
  v
CLEANED UP
```

## MFA Flow

```text
Primary factor verified
        |
        v
MFA enabled?
   | no
   v
Create session + issue tokens

   | yes
   v
Resolve enabled MFA providers
        |
        v
Create Redis challenge
        |
        v
Client submits challenge_id + provider + code
        |
        v
Claim challenge lock
        |
        v
Validate device/IP/user-agent/session binding
        |
        v
Verify provider
   | TOTP: decrypt secret + validate authenticator code
   | Recovery: BCrypt match + consume unused code
        |
        v
Consume challenge
        |
        v
Create session + issue tokens
```

## Passwordless Flow

```text
Request passwordless login
        |
        v
Find email if registered
        |
        +--> not found: return generic success
        |
        v
Generate one-time token
        |
        v
Store token hash in Redis with TTL
        |
        v
Send login link
        |
        v
Verify token
        |
        v
Delete Redis token keys
        |
        v
Shared login completion
        |
        +--> MFA challenge if enabled
        |
        +--> session + tokens if no MFA required
```

## OAuth Flow

```text
Google ID token
        |
        v
GoogleIdTokenVerifier
        |
        +--> issuer validation
        +--> audience validation
        +--> email_verified required
        |
        v
Provider link resolution
        |
        +--> existing provider link
        +--> existing email account linking rules
        +--> new Google-backed user
        |
        v
Profile synchronization
        |
        v
Shared MFA/session issuance
```

# Notable Engineering Decisions

## JWT + Persistent Sessions

**Problem:** Pure stateless JWT systems are difficult to revoke before expiry, while fully stateful session systems can
add database pressure to every request.

**Rejected alternatives:** A purely stateless refresh-token model was rejected because stolen refresh tokens remain
useful until expiry. A purely opaque session-token model was rejected because every request would need a session lookup
even when access-token authorization is sufficient.

**Chosen solution:** Access JWTs are stateless and short-lived; refresh tokens are linked to persistent `user_sessions`
rows. Access tokens carry `sid`, roles, AMR, and MFA facts. Refresh and sensitive operations return to server-side
session truth.

**Security implications:** Revocation is enforceable at refresh time and, when SID enforcement is enabled, at access
time. Stolen access tokens remain bounded by short TTL and optional session-liveness checks.

**Operational implications:** The system scales reads through JWT validation while preserving a server-side control
plane for logout, account deletion, device revocation, and step-up MFA freshness.

## RSA Signing

**Problem:** JWT verifiers need to validate tokens without becoming token issuers.

**Rejected alternative:** HMAC signing was rejected because every verifier that can validate the token also possesses
the signing secret.

**Chosen solution:** Access and refresh JWTs are signed with an RSA private key and verified with an RSA public key.

**Security implications:** Public-key verification reduces secret distribution and narrows issuer blast radius.

**Operational implications:** Key files can be mounted read-only in Docker, and future gateway/services can verify
tokens without access to the private key.

## Refresh Rotation

**Problem:** Long-lived refresh tokens create high-value bearer credentials.

**Rejected alternatives:** Static refresh tokens were rejected because replay remains valid until expiry. Storing raw
refresh tokens was rejected because database disclosure would immediately become account access.

**Chosen solution:** Each refresh request rotates the refresh token. The old token hash must match the live session row,
then the row is updated to the new hash.

**Security implications:** Reuse of an older refresh token fails after rotation. Raw token material is not persisted.

**Operational implications:** Rotation updates are constrained by user, device, token hash, revocation state, deleted
state, and expiry, keeping concurrency behavior explicit.

## MFA Orchestration

**Problem:** MFA is easy to bypass when implemented separately inside each login method.

**Rejected alternative:** Embedding MFA checks independently in local login, OAuth, and passwordless flows was rejected
because policy drift would be likely.

**Chosen solution:** Local, OAuth, and passwordless authentication converge through shared MFA orchestration and session
issuance.

**Security implications:** MFA-enabled users are challenged consistently regardless of primary authentication method.
Login-time challenges are isolated from session creation until verification succeeds.

**Operational implications:** New primary authentication methods can reuse the same challenge, session, token, and audit
pipeline.

## Redis for Ephemeral Security State

**Problem:** Rate-limit state, passwordless links, and MFA challenges need TTLs and cross-instance consistency.

**Rejected alternatives:** JVM-local caches were rejected because they fail under horizontal scaling. Database-only
ephemeral state was rejected for high-churn challenge and rate-limit paths.

**Chosen solution:** Redis backs Bucket4j rate limits, MFA challenge state, short-lived MFA setup secrets, and
passwordless token mappings.

**Security implications:** Centralized ephemeral state enables replay detection, challenge lockout, and
latest-token-wins semantics.

**Operational implications:** Redis becomes a critical dependency for abuse controls and challenge flows. Rate limiting
currently fails closed when Redis/Bucket4j operations fail.

## Device-Aware Sessions

**Problem:** Users need visibility and control over where they are logged in, and refresh rotation needs contextual
binding.

**Rejected alternative:** A single account-level refresh token set was rejected because it prevents targeted device
revocation and makes "log out other devices" imprecise.

**Chosen solution:** Each session stores a client-provided device ID plus IP and user-agent context. Refresh, logout,
revoke-others, MFA challenge binding, and rate-limit keys use device context.

**Security implications:** A stolen refresh token is harder to replay outside the expected device context because
rotation requires the matching device ID and live session hash.

**Operational implications:** The system can support account-security UI, device lists, targeted revocation, and future
trusted-device/risk scoring.

## Rate Limiting

**Problem:** Authentication endpoints are asymmetric: failed login, OTP, MFA verify, and passwordless attempts are cheap
for attackers and expensive or risky for the platform.

**Rejected alternative:** One global rate limit was rejected because it would either be too permissive for MFA/OTP or
too strict for normal browsing.

**Chosen solution:** Endpoint-scoped rate-limit policies use different key strategies and capacities for signup, login,
OTP, passwordless, refresh, MFA verify, and step-up flows.

**Security implications:** Brute force and challenge-flood attempts are throttled close to ingress and logged with
security event types.

**Operational implications:** `X-RateLimit-*` and `Retry-After` headers improve client behavior, while Prometheus
counters support alerting.

# Testing Strategy

The repository contains 89 Java test classes plus a k6 load-testing suite.

## Unit and Slice Testing

Unit tests cover security filters, JWT services, key loading, token hashing, identifier normalization, audit logging,
rate-limit resolution, rate-limit buckets, OAuth token handling, MFA orchestration, challenge Redis codecs,
recovery-code consumption, TOTP setup, passwordless login, OTP verification, session services, profile services,
repositories, and controller contracts.

Representative test classes include:

| Area               | Examples                                                                                                     |
|--------------------|--------------------------------------------------------------------------------------------------------------|
| JWT and filters    | `JwtServicesTest`, `JwtFilterTest`, `SidLivenessFilterTest`, `CsrfCookieFilterTest`                          |
| MFA                | `MfaServiceImplTest`, `MfaRecoveryCodeServiceImplTest`, `MfaOrchestratorTest`, `MfaChallengeServiceImplTest` |
| Step-up            | `RecentMfaEnforcementInterceptorTest`, `RecentMfaServiceImplTest`, `StepUpControllerTest`                    |
| Sessions           | `SessionServiceImplTest`, `SessionRepositoryTest`, `SessionControllerTest`                                   |
| Rate limiting      | `RateLimitFilterTest`, `RateLimitBucketServiceImplTest`, `RateLimitKeyResolverTest`                          |
| OAuth/passwordless | `OAuthServiceImplTest`, `OAuthControllerTest`, `PasswordlessLoginServiceImplTest`                            |
| OTP                | `OtpServiceTest`, `OtpControllerTest`, `OtpTokenRepositoryTest`                                              |

## Integration Testing

Integration tests use Testcontainers for PostgreSQL 16 and Redis 7. `BaseIntegrationTest` dynamically provisions
container properties, generates temporary RSA key pairs for test JWT signing, and replaces AWS SES/SNS senders with
mocks.

Implemented integration tests include session login/list behavior, OTP request/verify behavior, CSRF cookie behavior,
Redis connectivity, and full application context boot.

## Security Testing

Security-focused tests validate:

* JWT token type handling and claim extraction.
* SID liveness behavior for revoked or missing sessions.
* Sensitive-route validation for SID enforcement configuration.
* Recent-MFA enforcement and access-token refresh when session MFA is fresher than JWT claims.
* CSRF cookie generation and configured bypass behavior.
* MFA challenge context binding, provider mismatch handling, replay/consume failure paths, and Redis key codecs.
* Recovery code one-time consumption.
* Rate-limit headers, blocked behavior, and key derivation.

## Load Testing

The `load_tests/` directory contains 13 k6 scenario scripts plus a shared cookie helper. Scenarios cover health
stability, auth load, login load, refresh storms, session reads, logout, multi-device sessions, CSRF token behavior,
profile reads, protected endpoint concurrency, mixed traffic, and optional profile mutation idempotency. Historical
result captures and an audit summary are included in the repository.

# Infrastructure

The repository includes:

| Asset                 | Purpose                                                                                                                          |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `Dockerfile`          | Multi-stage Java 21 build and runtime image.                                                                                     |
| `docker-compose.yaml` | Local/packaged stack with PostgreSQL 16, Redis 7, and backend service.                                                           |
| Flyway migrations     | Versioned schema for users, providers, sessions, OTP, MFA, profile, and integrity indexes.                                       |
| `application.yaml`    | Externalized configuration for JWT TTLs, cookies, Redis, rate limits, OTP, passwordless, OAuth, AWS, Actuator, and cleanup jobs. |
| Testcontainers config | Production-like integration testing with PostgreSQL and Redis.                                                                   |
| Actuator/Prometheus   | Health and metric surfaces for runtime observability.                                                                            |

# Core API Surface

| Area                   | Endpoints                                                                                                                            |
|------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| Auth bootstrap         | `GET /api/v1/auth/csrf`, `POST /api/v1/auth/identifier/status`                                                                       |
| Signup                 | `POST /api/v1/auth/identifier/sign-up`, `POST /api/v1/auth/identifier/sign-up/complete`                                              |
| Login/logout           | `POST /api/v1/auth/identifier/login`, `POST /api/v1/auth/identifier/logout`                                                          |
| Tokens                 | `POST /api/v1/auth/jwt/refresh`                                                                                                      |
| Password recovery      | `POST /api/v1/auth/password/forgot/request`, `POST /api/v1/auth/password/forgot/reset`                                               |
| Password change/delete | `POST /api/v1/auth/password/change`, `POST /api/v1/auth/account/delete`                                                              |
| Passwordless           | `POST /api/v1/auth/passwordless/request`, `POST /api/v1/auth/passwordless/verify`                                                    |
| MFA                    | `POST /api/v1/auth/mfa/verify`, `POST /api/v1/auth/enable/mfa/request`, `POST /api/v1/auth/enable/mfa/confirm`                       |
| Step-up                | `POST /api/v1/auth/step-up/mfa/request`, `POST /api/v1/auth/step-up/mfa/verify`, `POST /api/v1/auth/step-up/protected-action`        |
| OAuth                  | `POST /api/v1/auth/oauth/google`, `POST /api/v1/auth/oauth/google/signup`                                                            |
| OTP                    | `POST /api/v1/otp/email/request`, `POST /api/v1/otp/email/verify`, `POST /api/v1/otp/phone/request`, `POST /api/v1/otp/phone/verify` |
| Sessions               | `GET /api/v1/sessions/get/all`, `DELETE /api/v1/sessions/delete/{sessionId}`, `POST /api/v1/sessions/revoke-others`                  |
| Profile                | `GET /api/v1/profile`, `PATCH /api/v1/profile`, `POST /api/v1/profile/image`                                                         |

# Security Design

| Risk                                 | Implemented Mitigation                                                                                 |
|--------------------------------------|--------------------------------------------------------------------------------------------------------|
| Credential stuffing                  | Redis-backed login/signup/OTP/passwordless rate limits, account lock state, audit events.              |
| Refresh token theft                  | Rotation, SHA-256 token hashing, device-bound rotation checks, revocation.                             |
| Access token replay after revocation | Short-lived access JWTs plus configurable SID liveness checks.                                         |
| MFA challenge replay                 | Redis challenge locks, context binding, consume-on-success, replay audit events.                       |
| OTP brute force                      | BCrypt-hashed OTPs, max attempts, resend cooldown, request windows, rate limits.                       |
| CSRF against cookie clients          | Spring CSRF with `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header.                                        |
| XSS token theft                      | Auth tokens are issued in HttpOnly cookies.                                                            |
| Database disclosure                  | Passwords, OTPs, and recovery codes are hashed; TOTP secrets are encrypted; refresh tokens are hashed. |
| OAuth account confusion              | Google issuer/audience/email verification plus expected identifier matching and provider-link rules.   |
| Sensitive operation abuse            | Recent-MFA step-up on password change, account deletion, and session revocation.                       |
| Stale unverified accounts            | Scheduled cleanup deletes unverified users after the grace window.                                     |

# Technical Highlights

* **Unified post-auth pipeline:** Local password, Google OAuth, and passwordless flows converge into shared MFA and
  session issuance.
* **Session-bound access tokens:** Access JWTs include `sid`, AMR, MFA facts, and role names derived from persisted
  session state.
* **Refresh-token rotation:** Database updates are constrained to live sessions with matching user, device, and old
  token hash.
* **Step-up MFA:** Sensitive operations are guarded by recent MFA, and successful step-up updates the existing session
  rather than creating a parallel elevation token.
* **Redis challenge orchestration:** MFA challenge state supports TTL, context binding, claim locks, attempt tracking,
  provider constraints, and replay detection.
* **Abuse-aware rate limiting:** Endpoint-specific distributed buckets protect login, signup, OTP, passwordless, MFA,
  step-up, and refresh paths.
* **Security telemetry:** Audit events and Micrometer counters make auth failures, rate limits, session events, and MFA
  outcomes observable.

# Tech Stack

## Backend

* Java 21
* Spring Boot 3
* Spring Security
* Spring Data JPA
* Spring Modulith naming boundaries
* Bucket4j

## Security and Cryptography

* JJWT
* RSA private/public key signing
* BCrypt
* SHA-256 token hashing
* AES-GCM encryption
* TOTP authenticator support

## Persistence and Infrastructure

* PostgreSQL 16
* Redis 7
* Flyway
* Docker / Docker Compose
* AWS SES / SNS
* Cloudinary profile image integration

## Testing and Observability

* JUnit 5
* Mockito
* MockMvc
* Testcontainers
* k6
* Micrometer
* Prometheus
* OpenAPI / Swagger UI

# Future Work

These items naturally extend the existing architecture without changing its core model:

* **Trusted devices:** Build on existing device IDs, session records, MFA timestamps, and revoke flows.
* **Risk-based authentication:** Use IP prefix, user-agent hash, device continuity, failed attempts, and rate-limit
  telemetry to require MFA dynamically.
* **OAuth MFA policy expansion:** Add provider-specific MFA enrollment and risk rules for federated identities.
* **Passwordless MFA hardening:** Add explicit step-up and recovery paths tailored for passwordless-first accounts.
* **Security dashboard:** Surface sessions, recent MFA events, suspicious context mismatches, rate-limit blocks, and
  OAuth link events.
* **Session analytics:** Track session age, refresh cadence, device spread, revocation frequency, and stale-session
  cleanup.
* **Permission enforcement:** Apply the existing role/permission model to administrative and user-management endpoints
  with `@PreAuthorize`.
* **Key rotation:** Add operational support for overlapping JWT verification keys and active signing key IDs.
* **WebAuthn/passkeys:** Extend the MFA provider registry with phishing-resistant authentication factors.

<br>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:6B57FF,50:1a1040,100:0a0a12&height=150&section=footer" width="100%"/>
