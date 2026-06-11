import os

readme_content = """# Dragon of North

An enterprise-grade, backend-focused security and identity platform designed to handle complex authentication flows, device-aware session management, and robust abuse prevention.

## Project Overview

**Dragon of North** is a highly technical identity and access management backend. It was built to solve the engineering challenge of providing stateless, scalable authentication without sacrificing the security and control typically associated with stateful sessions.

This platform exists to provide a secure foundation for modern applications that require multi-channel onboarding (email and phone), distributed rate limiting, explicit session revocation across multiple devices, and hardened token lifecycles. It is designed to be deployed in production environments where security, auditability, and resilience against common web vulnerabilities are critical.

## Architecture Overview

The system follows a strict layered architecture (`Controller` -> `Service` -> `Repository`), backed by Spring Security for the core authentication filter chain. 

### Request Lifecycle
1. **Ingress & Throttling:** Requests pass through a Redis-backed distributed rate limiter (`RateLimitFilter`) to preempt abuse.
2. **Authentication:** The `JwtFilter` intercepts requests, verifying the presence and validity of access tokens (via ****** or secure cookies).
3. **Authorization:** Method-level security (`@PreAuthorize`) ensures callers have the requisite roles/permissions for the invoked endpoint.
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

## Core Features

### Authentication
* **Purpose:** Establish user identity securely across multiple channels.
* **Technical Implementation:** Supports both `EMAIL` and `PHONE` identifiers via an `AuthenticationServiceResolver`. Uses `AuthenticationManager` with `UsernamePasswordAuthenticationToken` for credential login.
* **Security Considerations:** Enforces strict identifier mismatch validation. Uses `BCryptPasswordEncoder` for adaptive credential hashing.
* **Key Classes:** `AuthenticationServiceResolver`, `EmailAuthenticationServiceImpl`, `PhoneAuthenticationServiceImpl`.

### Session Management
* **Purpose:** Provide visibility and granular control over active logins across multiple devices.
* **Technical Implementation:** Sessions are persisted in PostgreSQL. Each login creates a `Session` record capturing `deviceId`, `ipAddress`, `userAgent`, and `expiryDate`.
* **Security Considerations:** A single active session is maintained per user-device pair. Supports targeted revocation (logout from one device) and "revoke all other sessions" for rapid account lockdown.
* **Key Classes:** `SessionServiceImpl`, `SessionRepository`.

### JWT Security
* **Purpose:** Enable stateless, horizontally scalable authorization.
* **Technical Implementation:** Issues separate access and refresh tokens. JWTs are signed asymmetrically using RSA private/public keys loaded via `KeyUtils`.
* **Security Considerations:** Enforces token typing (`token_type` claim) to prevent refresh tokens from being misused as access tokens. Access tokens embed roles to prevent database lookups on every request.
* **Key Classes:** `JwtServicesImpl`, `JwtFilter`.

### Refresh Token Rotation
* **Purpose:** Maintain long-lived sessions safely.
* **Technical Implementation:** When an access token expires, the client submits the refresh token. The system validates the token against the persisted `Session` hash, issues a new refresh token, and updates the hash in the database.
* **Security Considerations:** Raw refresh tokens are never stored—only their SHA-256 hashes. Rotation limits the replay window if a refresh token is compromised.
* **Key Classes:** `AuthCommonServiceImpl`, `TokenHasher`.

### Multi-Factor Authentication (MFA)
* **Purpose:** Provide step-up security for high-value accounts.
* **Technical Implementation:** Implemented via an orchestration layer (`MfaOrchestrator`) that evaluates whether an account requires a secondary challenge.
* **Security Considerations:** MFA flows are strongly separated from standard login.
* **Key Classes:** `MfaChallengeService`, `MfaOrchestrationResult`.

### Recovery Codes
* **Purpose:** Ensure account accessibility if primary MFA methods are lost.
* **Technical Implementation:** Generates and securely stores backup codes that can fulfill an MFA challenge.
* **Security Considerations:** Codes are consumed upon use (`verifyAndConsume`), preventing replay.
* **Key Classes:** `AuthCommonServiceImpl`.

### OAuth Integration
* **Purpose:** Support federated identity providers.
* **Technical Implementation:** Implements Google OAuth using `GoogleIdTokenVerifier` to validate downstream JWTs.
* **Security Considerations:** Validates external issuer signatures and maps federated identities to internal `AppUser` records securely.
* **Key Classes:** `GoogleOAuthConfig`, `OAuthServiceImpl`.

### Passwordless Authentication
* **Purpose:** Reduce reliance on passwords by sending secure, one-time authentication links/OTPs.
* **Technical Implementation:** Generates purpose-scoped (`LOGIN`) OTPs delivered via AWS SES/SNS.
* **Security Considerations:** Limits brute-forcing through attempt tracking and rate-limiting.
* **Key Classes:** `PasswordlessLoginServiceImpl`.

### Role-Based Authorization
* **Purpose:** Restrict access to sensitive endpoints based on user privileges.
* **Technical Implementation:** `Role` and `Permission` entities define many-to-many relationships. Missing system roles are auto-initialized on startup.
* **Security Considerations:** Security principal exports role-prefixed authorities, evaluated by `@PreAuthorize` annotations.
* **Key Classes:** `RolesInitializer`, `SecurityConfig`.

### Rate Limiting
* **Purpose:** Defend against brute-force attacks and resource exhaustion.
* **Technical Implementation:** Uses Bucket4j with Redis (via Lettuce) to manage distributed token buckets. 
* **Security Considerations:** Applies different limits based on endpoint risk (`signup`, `login`, `otp`). Resolves keys based on authenticated user ID or client IP to ensure fairness.
* **Key Classes:** `RateLimitConfig`, `RateLimitBucketServiceImpl`, `RateLimitFilter`.

### Device Management
* **Purpose:** Bind sessions to specific client devices for anomaly detection.
* **Technical Implementation:** Clients generate a stable `deviceId` UUID. This ID is passed during auth flows and bound to the persistent `Session` entity.
* **Security Considerations:** Refresh token rotation enforces device-bound validation, ensuring a stolen token cannot be easily replayed on a new device.

### CSRF Protection
* **Purpose:** Defend against Cross-Site Request Forgery for browser-based clients.
* **Technical Implementation:** Configures Spring Security with a `CookieCsrfTokenRepository` and a custom `CsrfCookieFilter`.
* **Security Considerations:** Synchronizes CSRF tokens into response cookies (`SameSite=None`, `Secure`) and validates them on state-changing requests, bypassing specific APIs (like MFA verification) where appropriate.
* **Key Classes:** `SecurityConfig`, `CsrfCookieFilter`.

### Audit Logging
* **Purpose:** Maintain attribution for all domain mutations.
* **Technical Implementation:** Uses Spring Data JPA `@EntityListeners(AuditingEntityListener.class)` with a custom `AuditorAwareImpl`.
* **Security Considerations:** Automatically captures the `createdBy` and `updatedBy` fields using the currently authenticated security principal.
* **Key Classes:** `AuditorAwareImpl`, `BaseEntity`.

### OTP Engine
* **Purpose:** Handle delivery and verification of temporary codes.
* **Technical Implementation:** Generates BCrypt-hashed OTPs with strict lifecycle statuses (`SUCCESS`, `EXPIRED_OTP`, `MAX_ATTEMPT_EXCEEDED`).
* **Security Considerations:** OTPs are scoped by `OtpPurpose` (`SIGNUP`, `LOGIN`, `PASSWORD_RESET`). Enforces cooldowns and max-request windows.
* **Key Classes:** `OtpServiceImpl`, `OtpToken`.

## Security Design

The architecture was built to mitigate specific threat vectors:

* **Session Hijacking:** Mitigated by tying sessions to a `deviceId` and utilizing short-lived access tokens.
* **Token Theft:** Addressed by refresh token rotation. If an attacker steals a refresh token, its subsequent use will invalidate the session.
* **Database Compromise:** Passwords and OTPs are hashed using BCrypt. Refresh tokens are stored as SHA-256 hashes. Even full database exposure does not yield usable raw tokens.
* **Brute Force & Credential Stuffing:** Defeated by a distributed, Redis-backed rate limiter that discriminates between high-risk (login/OTP) and low-risk endpoints.
* **Cross-Site Scripting (XSS):** Mitigated by delivering tokens via `HttpOnly`, `Secure` cookies.
* **Cross-Site Request Forgery (CSRF):** Prevented via strict token synchronization using `CookieCsrfTokenRepository`.

## Notable Engineering Decisions

* **Why JWT + Persistent Sessions?**
  Pure stateless JWTs cannot be forcefully revoked before expiration. By persisting sessions and hashing the refresh token, the system achieves the scalability of stateless API authorization (access tokens) while retaining absolute server-side control over the session lifecycle (revocation on refresh).
* **Why RSA for JWT Signing?**
  Symmetric keys (HMAC) share the same secret for signing and verifying. Using RSA asymmetric keys means the API gateway or downstream microservices can verify tokens using only the public key, drastically reducing the blast radius of a compromised service.
* **Why Redis for Rate Limiting?**
  In-memory rate limiters fail in multi-instance deployments. Using Redis ensures that an attacker cannot bypass limits simply by being routed to a different backend pod via a load balancer.
* **Why Separate OTP Purposes?**
  An OTP generated for `SIGNUP` must never be valid for a `PASSWORD_RESET` flow. Scoping OTPs to specific enums ensures strict contextual boundaries.

## Technical Highlights

* **Distributed Rate Limiting:** Redis-backed token bucket algorithm with dynamic key resolution (User ID vs. IP).
* **Cryptographic Token Lifecycles:** RSA-signed JWTs, BCrypt-hashed OTPs, and SHA-256 hashed refresh tokens.
* **Advanced Session Management:** Device-aware tracking, rotation, and fine-grained revocation APIs ("revoke all others").
* **Multi-Channel Delivery:** Asynchronous OTP delivery via AWS SES (Email) and SNS (Phone).
* **Observability:** Granular Actuator/Prometheus metrics for rate-limiting blocked vs. successful requests.
* **Standardized API Contracts:** Global exception handlers and structured `ApiResponse` envelopes.

## Tech Stack

### Backend
* **Java 21** / **Spring Boot 3**
* **Spring Security** / **Spring Data JPA**
* **Bucket4j** (Rate Limiting)

### Security & Cryptography
* **JJWT** (JSON Web Tokens)
* **BCrypt** (Password & OTP Hashing)
* **SHA-256** (Token Hashing)

### Persistence & Infrastructure
* **PostgreSQL** (Relational Data)
* **Redis / Lettuce** (Distributed State)
* **AWS SES / SNS** (Messaging)

### Tooling & Testing
* **Testcontainers** (Integration Testing)
* **Micrometer / Prometheus** (Metrics)
* **OpenAPI / Swagger** (Documentation)
"""

with open("README.MD", "w") as f:
    f.write(readme_content)
