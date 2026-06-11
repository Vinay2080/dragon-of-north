import os

readme_content = """<!-- ═══════════════════════════════════════════════════════════════ -->
<!--                   DRAGON OF NORTH — README                      -->
<!-- ═══════════════════════════════════════════════════════════════ -->

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0D0D12,50:1a1040,100:6B57FF&height=200&section=header&text=Dragon%20of%20North&fontSize=48&fontColor=ffffff&fontAlignY=52&desc=Production%20authentication%20infrastructure.%20Not%20a%20tutorial%20project.&descAlignY=70&descSize=14&descColor=9d8fff" width="100%"/>

<br>

<p align="center">
  <sub>
    Dragon of North is an enterprise-grade security and identity platform that handles complex authentication flows, device-aware sessions, and robust abuse prevention.
    It is designed to solve real engineering challenges: token lifecycle management, concurrent refresh races, and distributed rate limiting before they become incidents.
  </sub>
</p>

<!-- ─── IMPACT STRIP ─────────────────────────────────────────────── -->

<div align="center">
  <b>JWT + RSA-2048 &nbsp;·&nbsp; Cookie Auth + CSRF &nbsp;·&nbsp; Redis Rate Limiting &nbsp;·&nbsp; Device-Bound Sessions &nbsp;·&nbsp; OTP via AWS</b>
</div>

<br>

<!-- ─── TYPING ANIMATION ──────────────────────────────────────────── -->

<p align="center">
  <img src="https://readme-typing-svg.herokuapp.com?font=JetBrains+Mono&weight=500&size=16&duration=2500&pause=900&color=6B57FF&center=true&vCenter=true&width=720&lines=Not+just+login+APIs.+The+full+picture.;Token+lifecycle+%C2%B7+Session+revocation+%C2%B7+Abuse+controls;55%2B+test+classes+%C2%B7+12+k6+load+scenarios;Zero-downtime+CI%2FCD+on+every+push+to+main;This+is+what+production+auth+looks+like." />
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

Dragon of North is a highly technical identity and access management backend. It was built to solve the engineering challenge of providing stateless, scalable authentication without sacrificing the security and absolute control typically associated with stateful sessions.

This platform exists to provide a secure foundation for modern applications requiring multi-channel onboarding (email and phone), distributed rate limiting, explicit session revocation across multiple devices, and hardened token lifecycles. It is intended for production environments where security, auditability, and resilience against common web vulnerabilities are critical.

# Architecture Overview

The system follows a strict layered architecture (`Controller` -> `Service` -> `Repository`), backed by Spring Security for the core authentication filter chain.

### Request Lifecycle
1. **Ingress & Throttling:** Requests pass through a Redis-backed distributed rate limiter (`RateLimitFilter`) to preempt abuse.
2. **Authentication:** The `JwtFilter` intercepts requests, verifying the presence and validity of access tokens via `Authorization: Bearer` or `HttpOnly` secure cookies.
3. **Authorization:** Method-level security (`@PreAuthorize`) ensures callers have the requisite roles/permissions.
4. **Processing & Persistence:** Services orchestrate business logic, utilizing Spring Data JPA with optimistic locking and auditing to ensure safe concurrent mutations.

### Authentication & Session Flow
```text
Client                API Gateway / Filter Chain                 Auth Service                     Database / Redis
  |                              |                                    |                                  |
  |--- 1. Login (Creds/OTP) ---->|                                    |                                  |
  |                              |--- 2. Validate & Hash Password --->|                                  |
  |                              |                                    |--- 3. Create Session Record ---->| (Stores Device ID, IP)
  |                              |                                    |--- 4. Store Hash of Refresh ---->|
  |<-- 5. Return Tokens (JWT) ---|                                    |                                  |
  |    (Access + Refresh)        |                                    |                                  |
```

# Core Features

## Authentication
* **Purpose:** Establish user identity securely across multiple channels.
* **Technical Implementation:** Supports both `EMAIL` and `PHONE` identifiers via an `AuthenticationServiceResolver`. Signup logic is split into creation and completion (`/sign-up/complete`) to enforce lifecycle states (`NOT_EXIST` -> `CREATED` -> `VERIFIED`).
* **Security Considerations:** Enforces strict identifier mismatch validation. Uses `BCryptPasswordEncoder` for adaptive credential hashing.
* **Key Classes:** `AuthenticationServiceResolver`, `EmailAuthenticationServiceImpl`, `PhoneAuthenticationServiceImpl`.

## Session Management
* **Purpose:** Provide visibility and granular control over active logins across multiple devices.
* **Technical Implementation:** Sessions are persisted in PostgreSQL. Each login creates a `Session` record capturing `deviceId`, `ipAddress`, `userAgent`, and `expiryDate`.
* **Security Considerations:** A single active session is maintained per user-device pair. Supports targeted revocation (logout from one device) and "revoke all other sessions" for rapid account lockdown.
* **Key Classes:** `SessionServiceImpl`, `SessionRepository`.

## JWT Security
* **Purpose:** Enable stateless, horizontally scalable authorization.
* **Technical Implementation:** Issues separate access and refresh tokens. JWTs are signed asymmetrically using RSA private/public keys loaded via `KeyUtils`. Access tokens carry roles to avoid DB hits on every request.
* **Security Considerations:** Enforces token typing (`token_type` claim) to prevent refresh tokens from being misused as access tokens.
* **Key Classes:** `JwtServicesImpl`, `JwtFilter`.

## Refresh Token Rotation
* **Purpose:** Maintain long-lived sessions safely without permanent exposure.
* **Technical Implementation:** During token refresh, the client submits the old refresh token. The system validates it against the persisted `Session` hash, issues a new refresh token, and updates the hash in the database using optimistic locking (`@Version`).
* **Security Considerations:** Raw refresh tokens are never stored—only their SHA-256 hashes. Rotation drastically limits the replay window if a refresh token is compromised.
* **Key Classes:** `AuthCommonServiceImpl`, `TokenHasher`.

## MFA
* **Purpose:** Provide step-up security for high-value accounts or suspicious activities.
* **Technical Implementation:** Evaluated via an orchestration layer (`MfaOrchestrator`) that seamlessly integrates a secondary challenge into the login flow.
* **Security Considerations:** MFA flows are isolated from the standard login state machine.
* **Key Classes:** `MfaChallengeService`, `MfaOrchestrationResult`.

## Recovery Codes
* **Purpose:** Ensure account accessibility if primary MFA methods are lost or unavailable.
* **Technical Implementation:** Securely stores hashed backup codes generated during MFA setup.
* **Security Considerations:** Codes are consumed strictly upon use (`verifyAndConsume`), preventing any form of replay.
* **Key Classes:** `AuthCommonServiceImpl`.

## OAuth
* **Purpose:** Support federated identity providers alongside local credential flows.
* **Technical Implementation:** Implements Google OAuth using `GoogleIdTokenVerifier` to validate downstream JWTs, persisting linked profiles in a `provider_link` table.
* **Security Considerations:** Validates external issuer signatures, audience boundaries, and explicit identity linking rules.
* **Key Classes:** `GoogleOAuthConfig`, `OAuthServiceImpl`.

## Passwordless Authentication
* **Purpose:** Reduce reliance on passwords by utilizing secure, one-time authentication links and OTPs.
* **Technical Implementation:** Generates purpose-scoped (`LOGIN`) OTPs delivered asynchronously via AWS SES/SNS.
* **Security Considerations:** Protected by strict attempt tracking, cooldown windows, and rate limits to prevent brute-forcing.
* **Key Classes:** `PasswordlessLoginServiceImpl`.

## Device Management
* **Purpose:** Bind sessions to specific client devices to assist with anomaly detection.
* **Technical Implementation:** Clients generate a stable `deviceId` UUID. This ID is passed during auth flows and bound to the persistent `Session` entity.
* **Security Considerations:** Refresh token rotation enforces device-bound validation (`token hash + deviceId + userId`), ensuring a stolen token cannot be easily replayed on a different machine.

## Role-Based Authorization
* **Purpose:** Restrict access to sensitive endpoints based on user privileges.
* **Technical Implementation:** `Role` and `Permission` entities define many-to-many relationships. Missing system roles are auto-initialized on startup (`RolesInitializer`).
* **Security Considerations:** Security principal exports both role-prefixed and permission-prefixed authorities, evaluated safely by `@PreAuthorize`.
* **Key Classes:** `RolesInitializer`, `SecurityConfig`.

## Audit Logging
* **Purpose:** Maintain strict attribution for all domain mutations.
* **Technical Implementation:** Uses Spring Data JPA `@EntityListeners(AuditingEntityListener.class)` combined with a custom `AuditorAwareImpl`.
* **Security Considerations:** Automatically captures `createdBy` and `updatedBy` fields using the authenticated security principal.
* **Key Classes:** `AuditorAwareImpl`, `BaseEntity`.

## Redis Infrastructure
* **Purpose:** Provide highly available, distributed state for transient data mechanisms.
* **Technical Implementation:** Redis serves as the backend for Bucket4j via Lettuce, scaling limiters beyond local memory bounds.
* **Security Considerations:** Designed to "fail-open"—if Redis becomes unreachable, requests fall back to passing through to prioritize auth service availability over strict limiting.

## Security Monitoring
* **Purpose:** Provide SRE visibility into abuse and traffic patterns.
* **Technical Implementation:** Integrated with Actuator and Prometheus, exposing endpoints like `/actuator/prometheus`.
* **Security Considerations:** Dedicated Micrometer counters track `rate_limit.blocked` vs `rate_limit.success` per limit type, enabling precise alerting on abuse trends.

## CSRF Protection
* **Purpose:** Defend against Cross-Site Request Forgery for browser-based, cookie-authenticated clients.
* **Technical Implementation:** Implemented via Spring Security's `CookieCsrfTokenRepository` and a custom `CsrfCookieFilter`.
* **Security Considerations:** Synchronizes CSRF tokens into `SameSite=None`, `Secure` response cookies, demanding validation on state-changing requests while explicitly bypassing stateless / bootstrap endpoints.
* **Key Classes:** `SecurityConfig`, `CsrfCookieFilter`.

## Rate Limiting
* **Purpose:** Defend against brute-force attacks and resource exhaustion.
* **Technical Implementation:** Distributed token buckets map to endpoint risk profiles (e.g., `signup`, `login`, `otp`). Keys resolve dynamically based on authenticated user ID or client IP.
* **Security Considerations:** Ensures fairness while clamping down on unauthenticated floods. Returns `X-RateLimit-*` headers for client observability.
* **Key Classes:** `RateLimitConfig`, `RateLimitBucketServiceImpl`, `RateLimitFilter`.

# Security Design

This architecture was built with specific, mitigated threat vectors in mind:

* **Session Hijacking:** Mitigated by tying sessions to a `deviceId` and utilizing short-lived access tokens.
* **Token Theft:** Addressed by refresh token rotation. If an attacker steals a refresh token, its subsequent use will invalidate the session and force re-authentication.
* **Database Compromise:** Passwords and OTPs are hashed using BCrypt. Refresh tokens are stored strictly as SHA-256 hashes. Even full database exposure does not yield usable raw tokens.
* **Brute Force & Credential Stuffing:** Defeated by a distributed, Redis-backed rate limiter that discriminates between high-risk (login/OTP) and low-risk endpoints.
* **Replay Attacks:** Addressed through strict single-use OTP rules, rotating refresh tokens, and optimistic locking (`@Version`) to prevent concurrent refresh races.
* **Cross-Site Scripting (XSS):** Mitigated by delivering auth material via `HttpOnly`, `Secure` cookies.
* **Recovery / MFA Compromise:** Recovery codes are aggressively consumed on first use, and MFA challenges are handled via a separated orchestration loop.
* **Device Trust Model:** Device IDs are persisted and explicitly tracked. Users have clear UI flows to "revoke all other devices".

# Notable Engineering Decisions

* **Why JWT + Persistent Sessions?**
  Pure stateless JWTs cannot be forcefully revoked before expiration. By persisting sessions and hashing the refresh token, the system achieves the horizontal scalability of stateless API authorization (access tokens) while retaining absolute server-side control over the session lifecycle (revocation on refresh).
* **Why RSA for JWT Signing?**
  Symmetric keys (HMAC) share the exact same secret for signing and verifying. Using RSA asymmetric keys means the API gateway or downstream microservices can verify tokens using *only* the public key, drastically reducing the blast radius of a compromised microservice.
* **Why Redis is used for Rate Limiting?**
  In-memory rate limiters fail in multi-instance deployments. Using Redis ensures that an attacker cannot bypass limits simply by being routed to a different backend pod via a load balancer.
* **Why Refresh Token Rotation Exists?**
  It prevents a stolen refresh token from being useful indefinitely. When combined with optimistic locking, concurrent replay attempts result in an immediate `409 Conflict`, blocking the attacker.
* **Why Separate OTP Purposes?**
  An OTP generated for `SIGNUP` must never be valid for a `PASSWORD_RESET` flow. Scoping OTPs to specific enums ensures strict contextual boundaries at compile-time, closing an entire class of OTP reuse attacks.

# Technical Highlights

* **Distributed Rate Limiting:** Redis-backed token bucket algorithm with dynamic key resolution (User ID vs. IP).
* **Cryptographic Token Lifecycles:** RSA-signed JWTs, BCrypt-hashed OTPs, and SHA-256 hashed refresh tokens.
* **Advanced Session Management:** Device-aware tracking, optimistic locking against refresh races, and fine-grained revocation APIs ("revoke all others").
* **Multi-Channel Delivery:** Asynchronous OTP delivery via AWS SES (Email) and SNS (Phone).
* **Observability:** Granular Actuator/Prometheus metrics for rate-limiting blocked vs. successful requests.
* **Standardized API Contracts:** Global exception handlers and structured `ApiResponse` envelopes bridging filter-layer and controller-layer failures.

# Tech Stack

### Backend
* **Java 21** / **Spring Boot 3**
* **Spring Security** / **Spring Data JPA**
* **Bucket4j** (Distributed Rate Limiting)

### Security & Cryptography
* **JJWT** (JSON Web Tokens)
* **BCrypt** (Password & OTP Hashing)
* **SHA-256** (Token Hashing)

### Persistence & Infrastructure
* **PostgreSQL 16** (Relational Data & Optimistic Locking)
* **Redis 7 / Lettuce** (Distributed Throttling State)
* **AWS SES / SNS** (Messaging Channels)

### Tooling & Testing
* **Testcontainers** (Environment-Faithful Integration Testing)
* **Micrometer / Prometheus** (Observability)
* **OpenAPI / Swagger** (Contract Documentation)

<br>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:6B57FF,50:1a1040,100:0a0a12&height=150&section=footer" width="100%"/>
"""

with open("README.MD", "w") as f:
    f.write(readme_content)
