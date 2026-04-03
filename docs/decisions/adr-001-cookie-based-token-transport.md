# ADR-001: Cookie-Based Token Transport

## Why
Browser clients need safer token storage than JavaScript-accessible local storage.

## Decision
Use HttpOnly cookies for `access_token` and `refresh_token` with configurable `same-site` and `secure` attributes.

## Impact
- Better XSS resistance for token theft.
- Requires CSRF strategy for protected mutating requests.
- Backend must manage cookie lifecycle consistently.

## Tradeoffs
- Slightly higher complexity vs bearer token headers.
- Cross-site setup depends on correct cookie flags and frontend behavior.
