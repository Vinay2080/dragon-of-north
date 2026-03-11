# JWT-Based Authentication Flow

## Problem Statement

**Server-side memory sessions break across replicas and load balancers.**

Traditional session-based authentication requires shared session state across all application instances. This creates
scalability issues and single points of failure in distributed systems.

## Technical Solution

**Stateless tokens allow any API node to authenticate without shared session memory.**

JWT (JSON Web Tokens) contain all necessary authentication information within the token itself, eliminating the need for
server-side session storage.

## State Chart Diagram

```mermaid
stateDiagram-v2
    [*] --> Unauthenticated
    Unauthenticated --> LoginAttempt: User initiates login
    LoginAttempt --> ValidatingCredentials: Submit credentials
    ValidatingCredentials --> Authenticated: Credentials valid
    ValidatingCredentials --> AuthenticationFailed: Credentials invalid
    AuthenticationFailed --> Unauthenticated: Try again
    Authenticated --> TokenIssued: Generate JWT
    TokenIssued --> ActiveSession: Token sent to client
    ActiveSession --> TokenValidation: API request with token
    TokenValidation --> RequestAuthorized: Token valid
    TokenValidation --> TokenExpired: Token expired
    TokenValidation --> TokenInvalid: Token malformed/tampered
    RequestAuthorized --> ActiveSession: Continue processing
    TokenInvalid --> Unauthenticated: Force re-authentication
    TokenExpired --> RefreshTokenAttempt: Use refresh token
    RefreshTokenAttempt --> TokenRefreshed: Refresh valid
    RefreshTokenAttempt --> Unauthenticated: Refresh invalid/expired
    TokenRefreshed --> ActiveSession: New tokens issued
    ActiveSession --> SessionTerminated: User logout/revocation
    SessionTerminated --> Unauthenticated: Clear tokens
    note right of Authenticated: User identity verified
    note right of TokenIssued: JWT contains: user ID, roles, expiration
    note right of TokenValidation: Verify signature + expiration
    note right of TokenRefreshed: Rotate refresh token family
```

## Detailed Flow

### 1. Authentication Initiation

```http
POST /api/auth/login
Content-Type: application/json

{
  "identifier": "user@example.com",
  "password": "securePassword123"
}
```

### 2. Token Issuance

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "rt_abc123def456...",
  "expiresIn": 900,
  "tokenType": "Bearer"
}
```

### 3. API Request with Token

```http
GET /api/protected/resource
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 4. Token Refresh

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "rt_abc123def456..."
}
```

## Security Benefits

1. **Scalability**: No shared session state required
2. **Performance**: Eliminates database lookups for authentication
3. **Resilience**: System continues working even if some nodes fail
4. **Cross-Origin**: Tokens work across different domains/services

## Implementation Details

### JWT Structure

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-uuid",
    "email": "user@example.com",
    "roles": [
      "USER",
      "ADMIN"
    ],
    "iat": 1640995200,
    "exp": 1640996100,
    "iss": "dragon-of-north",
    "aud": "api-gateway"
  }
}
```

### Verification Process

1. **Signature Verification**: Validate RSA signature using public key
2. **Token Expiration**: Check `exp` claim against current time
3. **Issuer Validation**: Verify `iss` claim matches expected issuer
4. **Audience Check**: Ensure `aud` claim includes current service

## Failure Mode Mitigation

| Threat             | Mitigation                    |
|--------------------|-------------------------------|
| Token interception | Short expiration (15 minutes) |
| Token replay       | Unique JWT ID (`jti`) claim   |
| Token tampering    | RSA signature verification    |
| Long-term access   | Refresh token rotation        |

## Monitoring Metrics

- Token issuance rate
- Token validation success/failure rate
- Refresh token frequency
- Average token lifetime
- Failed authentication attempts by source

---

*Related
Features: [RSA Token Signing](rsa-signing-flow.md), [Refresh Token Rotation](refresh-token-rotation.md), [Access Token Expiration](./token-expiration.md)*
