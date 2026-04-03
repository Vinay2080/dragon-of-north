# Feature: Session Management

## Purpose
Expose active device sessions and allow revocation of one or many refresh sessions.

## Entry Points (API endpoints)
- `GET /api/v1/sessions/get/all`
- `DELETE /api/v1/sessions/delete/{session_id}`
- `POST /api/v1/sessions/revoke-others`

## Flow (step-by-step backend flow)
1. Controller extracts authenticated user id from security principal.
2. Service fetches or mutates `user_sessions` for that user.
3. Revoke operations mark session(s) revoked.
4. Results are mapped to summary DTOs/messages.

## Key Classes (controllers, services, repositories)
- Controller: `SessionController`
- Service: `SessionServiceImpl`
- Repository: `SessionRepository`

## Security considerations
- All endpoints require authenticated principal.
- Revoke-by-id path is user-scoped to prevent cross-user session tampering.
- Refresh token rotation relies on this module for replay containment.

## Edge cases
- Unknown session id returns not-found/business error.
- Revoking others can return zero when only current device exists.
- Device id mismatch during rotation/logout invalidates operation.

## Dependencies
- `AuthCommonServiceImpl` refresh/logout flows
- `TokenHasher`

## Notes for AI (important implementation assumptions)
- Session primary key is UUID (path variable), but revocation logic also keys on `device_id` in auth flows.
- This module stores security context data (IP/user-agent), useful for audits/UX.
