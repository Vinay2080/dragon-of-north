# ADR-003: Endpoint-Scoped Rate Limiting

## Why
Auth and OTP endpoints are prime abuse targets and need stronger protection than generic API routes.

## Decision
Apply token-bucket limits by endpoint pattern using Redis-backed bucket service in a request filter.

## Impact
- Reduces brute-force and OTP spam risk.
- Exposes actionable headers for clients (`remaining`, `capacity`, `retry-after`).
- Provides metrics for blocked/success rate-limit events.

## Tradeoffs
- Policy tuning required to avoid false positives for legitimate spikes.
- Redis availability directly affects rate-limit correctness.
