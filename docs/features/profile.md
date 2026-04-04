# Feature: Profile Management

## Purpose
Allow authenticated users to read and update profile metadata and expose primary auth provider in profile response.

## Entry Points (API endpoints)
- `GET /api/v1/profile`
- `PATCH /api/v1/profile`

## Flow (step-by-step backend flow)
1. Authenticated request enters controller.
2. Current user id is resolved from security principal variants.
3. Update path writes profile fields via profile service.
4. Read path fetches profile entity and resolves provider (`LOCAL`/`GOOGLE`).
5. API response returns normalized profile payload.
6. Provisioning paths call `ensureProfileExists(...)` for idempotent profile creation.

## Key Classes (controllers, services, repositories)
- Controller: `ProfileController`
- Service: `ProfileServiceImpl`
- Repositories: `ProfileRepository`, `UserAuthProviderRepository`

## Security considerations
- Endpoints require authenticated context.
- Controller guards unsupported/anonymous principal types.
- Provider detection avoids exposing incorrect account linkage.

## Edge cases
- Principal can arrive as `AppUserDetails`, `AppUser`, UUID, or raw string UUID.
- Unsupported principal shape throws unauthorized business exception.
- Provider may be null if neither LOCAL nor GOOGLE mapping exists.
- Profile creation for auth/oauth flows is idempotent via `ensureProfileExists`.

## Dependencies
- Spring Security context
- Profile + provider repositories

## Notes for AI (important implementation assumptions)
- Profile endpoints are not public URLs; JWT cookie must be valid.
- Provider in response is derived from auth-provider table, not from profile table.
