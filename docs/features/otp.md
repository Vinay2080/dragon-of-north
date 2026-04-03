# Feature: OTP Verification

## Purpose
Provide purpose-scoped OTP issuance and verification for email/phone flows (signup, login, password reset).

## Entry Points (API endpoints)
- `POST /api/v1/otp/email/request`
- `POST /api/v1/otp/phone/request`
- `POST /api/v1/otp/email/verify`
- `POST /api/v1/otp/phone/verify`

## Flow (step-by-step backend flow)
1. Request endpoint validates identifier and otp purpose.
2. Service enforces resend cooldown / request window limits.
3. OTP is generated, hashed, persisted, and dispatched via sender abstraction.
4. Verify endpoint checks active token, expiry, attempts, and purpose.
5. Success marks token consumed; failure increments attempts.

## Key Classes (controllers, services, repositories)
- Controller: `OtpController`
- Service: `OtpServiceImpl`
- Senders: `EmailOtpSender`, `PhoneOtpSender`, `SesEmailService`
- Repository: `OtpTokenRepository`

## Security considerations
- OTP values are stored as hash, not plaintext.
- Tokens are purpose-scoped and single-consumption.
- Request throttling and max verify-attempts reduce brute-force risk.

## Edge cases
- Expired or consumed OTP is rejected.
- Purpose mismatch (e.g., SIGNUP code for PASSWORD_RESET) fails.
- Multiple rapid requests trigger cooldown/rate-limit errors.

## Dependencies
- AWS SES/SNS integrations
- OTP config in `application.yaml`
- `OtpToken` persistence and cleanup scheduler

## Notes for AI (important implementation assumptions)
- OTP verification returns domain status enum (`OtpVerificationStatus`), not only boolean.
- Request/verify for email and phone are parallel paths with shared core service semantics.
