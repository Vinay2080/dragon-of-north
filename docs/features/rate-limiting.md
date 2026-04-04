# Feature: Request Rate Limiting

## Purpose
Protect high-risk endpoints (signup, login, OTP) from bursts, brute force, and abuse traffic.

## Entry Points (API endpoints)
- Applied via patterns configured in `rate-limit.endpoints` (e.g., login/signup/otp paths)

## Flow (step-by-step backend flow)
1. `RateLimitFilter` matches request URI to configured endpoint pattern.
2. Key resolver computes rate-limit key (IP/user/context dependent).
3. Bucket service attempts token consumption.
4. Response headers are written (`X-RateLimit-Remaining`, `X-RateLimit-Capacity`).
5. On deny, `Retry-After` is set and business exception is thrown.

## Key Classes (controllers, services, repositories)
- Filter: `RateLimitFilter`
- Service: `RateLimitBucketServiceImpl`
- Resolver: `RateLimitKeyResolver`
- Config: `RateLimitProperties`, `RateLimitConfig`

## Security considerations
- Limits are endpoint-specific and centrally configurable.
- Blocking events are metered for abuse monitoring.
- Runs before auth filter chain to reduce attack surface impact.

## Edge cases
- Unmatched endpoint patterns skip rate limiting.
- Misconfigured `type` mapping can break policy resolution.
- Distributed behavior depends on backing Redis health.

## Dependencies
- Redis
- Bucket4j
- Micrometer counters

## Notes for AI (important implementation assumptions)
- This is infrastructure-level policy, not controller annotations.
- Rate limiting currently focuses on selected auth/otp routes; not global for all endpoints.
