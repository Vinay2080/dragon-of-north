# Auth Phase 5.5 — Final Authorization Consistency & Production Readiness

## 1) Sensitive endpoint audit

| Endpoint | Risk | Auth required | Recent MFA required | Current status |
|---|---|---:|---:|---|
| `POST /api/v1/auth/password/change` | High | Yes | Yes | Protected via `@SensitiveAccountOperation` |
| `POST /api/v1/auth/account/delete` | High | Yes | Yes | Protected via `@SensitiveAccountOperation` |
| `POST /api/v1/auth/enable/mfa/request` | High | Yes | Yes | Protected via `@SensitiveAccountOperation` |
| `POST /api/v1/auth/enable/mfa/confirm` | High | Yes | Yes | Protected via `@SensitiveAccountOperation` |
| `DELETE /api/v1/sessions/delete/{sessionId}` | High | Yes | Yes | Protected via `@SensitiveAccountOperation` |
| `POST /api/v1/sessions/revoke-others` | High | Yes | Yes | Protected via `@SensitiveAccountOperation` |
| `POST /api/v1/auth/step-up/protected-action` | High | Yes | Yes | Protected via `@SensitiveAccountOperation` |
| `POST /api/v1/auth/step-up/mfa/request` | Medium | Yes | No (this is the step-up bootstrap) | Auth + session binding checks |
| `POST /api/v1/auth/step-up/mfa/verify` | High | Yes | No (this grants recent MFA) | Challenge + session binding + replay/rate-limit protection |
| `POST /api/v1/auth/jwt/refresh` | High | Refresh token | No | Rotation race-safe + rate-limited |
| `POST /api/v1/auth/mfa/verify` | High | Challenge-bound pre-auth | No | Challenge-bound + replay/rate-limit protection |
| `POST /api/v1/auth/identifier/login` | High | No | No | Password auth + challenge/rate limits |
| `POST /api/v1/auth/identifier/logout` | Medium | Refresh token | No | Session-bound logout |
| `POST /api/v1/auth/password/forgot/request` | Medium | No | No | Intentionally public; anti-enumeration messaging |
| `POST /api/v1/auth/password/forgot/reset` | High | OTP/reset secret | No | Intentionally recovery path |

### Ambiguous / watchlist
- Profile mutation endpoints are authenticated but currently not recent-MFA-gated; this is acceptable product policy today but should be revisited if profile fields become security-critical.

## 2) Policy-centralization approach

Implemented `@SensitiveAccountOperation` meta-annotation over `@RequireRecentMfa`.

- Existing sensitive endpoints now use `@SensitiveAccountOperation`.
- Future high-risk endpoints should default to this annotation to reduce "forgot annotation" drift.
- Existing interceptor (`RecentMfaEnforcementInterceptor`) remains single enforcement point.

## 3) JWT/session drift semantics (explicit)

### Immediate guarantees
- Session revocation truth is immediate in DB/Redis state.
- SID liveness check blocks revoked/missing session usage when enforcement applies.
- Step-up success updates session `mfa_verified_at` and next access token projection.

### Eventual/stale windows (intentional)
- Old access JWT may remain usable until expiry on non-sensitive endpoints.
- Recent-MFA freshness in JWT is bounded by token refresh cadence.

### Compensating controls
- SID liveness filter for revoked/missing sessions.
- `@SensitiveAccountOperation`/recent-MFA interceptor for high-risk actions.
- Refresh-rotation CAS/race handling and replay-aware observability.

## 4) Practical threat-model findings

- **Login MFA**: protected by credential validation, challenge issuance, challenge consumption atomicity, and rate limits.
- **Step-up MFA**: protected by authenticated session requirement, session ownership/liveness checks, context binding, replay-safe consume.
- **Refresh rotation**: single-winner rotation semantics; losers rejected.
- **Replay attempts**: challenge consume-race fails; suspicious telemetry emitted.
- **Session theft/stale JWT replay**: constrained by SID liveness + endpoint-level recent-MFA checks.
- **Challenge flooding**: rate limits on request/verify surfaces + abuse telemetry.
- **Rate-limit bypass attempts**: key strategy now includes IP/device/challenge/user/session dimensions.

## 5) Architecture lifecycle (maintainer view)

`login -> (optional) challenge issued -> challenge verify -> session issuance -> JWT projection -> refresh rotation -> step-up -> sensitive operation`

## 6) Sequence flows

### Login MFA
1. Primary auth validates credentials.
2. If MFA required, challenge created in Redis.
3. Verify consumes challenge atomically.
4. Session row issued and cookies minted.
5. JWT projected from session truth.

### Step-up MFA
1. Authenticated request blocked by recent-MFA policy.
2. Client requests step-up challenge.
3. Verify consumes challenge with session binding.
4. Session `mfa_verified_at` refreshed.
5. New JWT projected; sensitive endpoint allowed on retry.

### Refresh rotation
1. Refresh token submitted.
2. Rotation query updates old hash -> new hash atomically.
3. Winner gets new tokens; losers fail.

### Replay rejection
1. Duplicate challenge verify races.
2. One consume succeeds, others fail with replay/consume-race semantics.
3. Suspicious/replay telemetry emitted.

## 7) Final cleanup audit

- Removed stale config TODO from `application.yaml`.
- No legacy MFA claim fallback behavior retained.
- Sensitive policy annotation now centralized by meta-annotation.
