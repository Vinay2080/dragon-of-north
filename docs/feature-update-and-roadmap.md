# Session/Auth Status Audit and Sequential Implementation Plan

## What Is Already Implemented (Code-Verified)

### Backend endpoints currently available

1. **Session listing (user)**
   - `GET /api/v1/sessions/get/all`
   - Returns session summaries for the authenticated user.

2. **Session revoke by session ID (user)**
   - `DELETE /api/v1/sessions/delete/{sessionId}`
   - Revokes one specific session owned by the authenticated user.

3. **Revoke all other sessions (user)**
   - `POST /api/v1/sessions/revoke-others`
   - Uses current `device_id` and revokes every other active session for that user.

4. **Auth endpoints already wired with device ID + session flow**
   - `POST /api/v1/auth/identifier/login` (creates session)
   - `POST /api/v1/auth/jwt/refresh` (validates + rotates session token)
   - `POST /api/v1/auth/identifier/logout` (revokes current session)

### Backend session capabilities already implemented

- Session creation stores: `deviceId`, `ipAddress`, `userAgent`, expiry and last-used timestamp.
- Refresh flow validates session by token-hash + device, checks revoked/expired, rotates refresh hash, updates last-used.
- Revoke operations exist for single session and all-other sessions.
- Session response DTO already includes metadata needed by UI:
  `sessionId`, `deviceId`, `ipAddress`, `userAgent`, `lastUsedAt`, `expiryDate`, `revoked`.

### What tests currently cover vs. missing

- Existing tests mainly cover auth identifier status/signup completion and selected auth service logic.
- Session-specific controller tests are **missing**.
- Authentication controller login/logout tests are marked TODO.
- Session cleanup test has TODO markers.

### Frontend status (current)

- Frontend already generates/stores a `deviceId` and sends it during login/refresh/logout.
- Auth context handles login/logout lifecycle and refresh retry behavior.
- There is **no session-management UI** yet (no “Manage Devices/Sessions” page, no revoke actions in UI).

## Gap Summary (Current Reality)

- **Session visibility APIs (user):** already implemented.
- **Session revocation APIs (user):** already implemented.
- **Session visibility/revocation tests:** largely missing.
- **Frontend session management page/actions:** missing.
- **Admin-level session visibility APIs:** not present yet.

## Sequential Future Plan (Based on Existing Implementation)

1. **Add API tests for existing session endpoints first (highest priority)**
   - Add controller tests for:
     - `GET /api/v1/sessions/get/all`
     - `DELETE /api/v1/sessions/delete/{sessionId}`
     - `POST /api/v1/sessions/revoke-others`
   - Cover auth required, success path, ownership checks, invalid input/device ID.

2. **Close auth endpoint test gaps**
   - Add `AuthenticationController` tests for login, refresh, logout (currently TODO/missing).
   - Add service-level tests for `AuthCommonServiceImpl` session interactions.

3. **Create frontend session API layer**
   - Add frontend API methods:
     - fetch sessions
     - revoke session by ID
     - revoke others (keep current device)
   - Reuse current device helper and existing auth cookies flow.

4. **Implement "Manage Sessions" frontend page**
   - List sessions with metadata and “current device” indicator.
   - Add actions: revoke single session, revoke all other sessions.
   - Show optimistic/loading/error states.

5. **Wire navigation + access control for session page**
   - Add route under protected area.
   - Link from dashboard/user menu.

6. **Add admin session visibility/revocation (new capability)**
   - Introduce admin-only endpoints for listing/revoking another user's sessions.
   - Add role-based tests to prevent privilege escalation.

7. **Harden security and observability**
   - Add audit events for login/refresh/logout/revoke with session identifiers.
   - Add metrics for revoked sessions, refresh failures, expired sessions.

8. **Rollout and validation**
   - Deploy in phases: backend tests pass → frontend page released behind flag → enable by default.
   - Monitor auth/session error rate and revoke endpoint usage after rollout.

## Suggested next delivery milestone

The most practical next milestone is:
- Session endpoint tests + auth controller test completion.
- First version of frontend "Manage Sessions" page with list + revoke actions.

This uses what is already built instead of re-implementing backend APIs.
