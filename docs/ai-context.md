# AI Context (Compressed)

## Stack
- Spring Boot (Java 21), Spring Security, JPA/Hibernate
- PostgreSQL (Flyway), Redis (rate limiting), Micrometer/Prometheus
- AWS SES/SNS for OTP delivery

## Architecture
- Layered backend: controllers -> services -> repositories -> PostgreSQL
- Security chain: RateLimitFilter -> CSRF/JWT auth chain -> controller
- JWT auth with cookie transport + DB-backed refresh sessions

## Key Flows
- **Login**: identifier/password -> authenticate -> JWT mint -> store session hash -> set cookies
- **Refresh**: refresh cookie + device id -> validate session -> rotate token -> set new cookies
- **Logout**: revoke session by refresh/device -> clear cookies
- **OTP**: create hashed OTP with purpose -> send -> verify and consume
- **Profile**: authenticated fetch/update + provider lookup
- **Sessions**: list and revoke session records per user

## Where Logic Lives
- Security config/filter: `security/config`, `security/filter`
- Auth orchestration: `modules/auth/service/impl/AuthCommonServiceImpl`
- OAuth: `modules/auth/controller/OAuthController`, `modules/auth/service/impl/OAuthServiceImpl`
- OTP: `modules/otp/service/impl/OtpServiceImpl`
- Sessions: `modules/session/service/impl/SessionServiceImpl`
- Profile: `modules/profile/service/impl/ProfileServiceImpl`
- Rate limiting: `ratelimit/*`
- Error envelope/handler: `shared/dto/api`, `shared/exception`

## Fast Navigation
1. Read `docs/system/system-overview.md`
2. Read `docs/system/security.md`
3. Open feature file in `docs/features/`
4. Confirm request/response in `docs/contracts/api.md`
5. Review rationale in `docs/decisions/`
