# Feature: OAuth (Google)

## Purpose
Authenticate or sign up users using Google ID tokens while preserving the same session/cookie model as local auth.

## Entry Points (API endpoints)
- `POST /api/v1/auth/oauth/google`
- `POST /api/v1/auth/oauth/google/signup`

## Flow (step-by-step backend flow)
1. Controller receives `id_token` (+ optional expected identifier/device id).
2. Service verifies token against configured Google client id.
3. Existing-account path: authenticate and issue cookies.
4. Signup path: create/link user + provider record, assign defaults, issue cookies.
5. Session entry is persisted for refresh-token lifecycle.

## Key Classes (controllers, services, repositories)
- Controller: `OAuthController`
- Service: `OAuthServiceImpl`
- Supporting service: `GoogleTokenVerifierService`
- Repository: `UserAuthProviderRepository`

## Security considerations
- Provider linkage prevents local password flow from bypassing provider constraints.
- Expected identifier can be checked to reduce token-account mismatch risk.
- Tokens are not trusted until verification service validates signature/audience.

## Edge cases
- Existing identifier with incompatible provider path is rejected.
- Invalid/expired Google token results in authentication failure.
- Duplicate provider mapping prevented by DB uniqueness.

## Dependencies
- Google OAuth config (`google.client-id`)
- Shared JWT/session infrastructure
- `UserAuthProvider` persistence

## Notes for AI (important implementation assumptions)
- OAuth endpoints are in CSRF bypass list due pre-auth cross-origin flow.
- OAuth still ends in same cookie/session model as local auth.
