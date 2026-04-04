# ADR-002: Refresh Token Rotation with Session Store

## Why
Static refresh tokens are vulnerable to replay if leaked.

## Decision
Persist hashed refresh-token sessions per device and rotate refresh token on refresh flow.

## Impact
- Replay containment and device-scoped revocation.
- Enables session management endpoints (list/revoke).
- Requires DB writes on login/refresh/logout flows.

## Tradeoffs
- Loses pure stateless simplicity.
- Adds operational dependence on session persistence and cleanup.
