# System Overview

## Purpose
Provide a fast map of backend architecture so developers and AI agents can find logic without scanning all modules.

## Stack
- Java 21 + Spring Boot
- Spring Security (JWT + CSRF)
- PostgreSQL + Flyway migrations
- Redis + Bucket4j for rate limiting
- Micrometer + Prometheus metrics
- AWS SES/SNS for OTP delivery

## Module Map
| Module | Responsibility | Primary Packages |
|---|---|---|
| Auth | Identifier login/signup, password reset, token refresh, logout | `modules.auth` |
| OAuth | Google token validation and account linking/signup | `modules.auth.controller`, `modules.auth.service.impl.OAuthServiceImpl` |
| OTP | OTP issue/verify for signup/login/password reset | `modules.otp` |
| Sessions | Device-bound refresh sessions, revocation | `modules.session` |
| Profile | User profile read/update | `modules.profile` |
| Security | JWT parsing, filter chain, CSRF/cookie policy | `security` |
| Rate Limit | Endpoint-specific token bucket limits | `ratelimit` |
| Shared/Infra | Exceptions, API wrappers, configs, schedulers, audit | `shared`, `infrastructure` |

## Request Pipeline
1. `RateLimitFilter` applies endpoint policy and sets rate headers.
2. `SecurityFilterChain` enforces CSRF (except bypass list), CORS, and auth rules.
3. `JwtFilter` resolves `access_token` and populates Spring Security context.
4. Controller delegates to service layer.
5. Service layer updates DB entities and emits metrics/audit logs.

## Data Flow (Auth Happy Path)
1. Client calls `/api/v1/auth/identifier/login` with identifier + password + `device_id`.
2. Credentials are authenticated; user/provider constraints are checked.
3. Access JWT and refresh JWT are generated.
4. Refresh token hash is persisted in `user_sessions` with device/IP/user-agent.
5. Cookies are written (`access_token`, `refresh_token`) and used in subsequent requests.

## Where to Start Reading
- Security entry point: `security/config/SecurityConfig.java`
- Auth core logic: `modules/auth/service/impl/AuthCommonServiceImpl.java`
- Session lifecycle: `modules/session/service/impl/SessionServiceImpl.java`
- OTP lifecycle: `modules/otp/service/impl/OtpServiceImpl.java`
- Cross-cutting errors: `shared/exception/ApplicationExceptionHandler.java`
