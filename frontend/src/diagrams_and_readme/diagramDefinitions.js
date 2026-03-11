export const systemOverview = {
    nodes: [
        {id: 'browser', label: 'User Browser', position: {x: 40, y: 180}, responsibilities: ['Initiates auth requests', 'Stores short-lived tokens/cookies', 'Renders secure UI state']},
        {id: 'frontend', label: 'Frontend (Vercel)', position: {x: 280, y: 180}, responsibilities: ['Presents login flows', 'Calls backend API', 'Manages session UX']},
        {id: 'backend', label: 'Backend API (Spring Boot)', position: {x: 540, y: 180}, responsibilities: ['Validates credentials', 'Issues and rotates tokens', 'Applies RBAC and security filters']},
        {id: 'postgres', label: 'PostgreSQL', position: {x: 820, y: 90}, responsibilities: ['Stores users/sessions', 'Stores hashed refresh tokens', 'Persists audit events']},
        {id: 'redis', label: 'Redis', position: {x: 820, y: 220}, responsibilities: ['Rate limiting counters', 'Fast policy/risk checks', 'Short-lived auth metadata']},
        {id: 'oauth', label: 'OAuth Provider', position: {x: 540, y: 340}, responsibilities: ['External identity proofing', 'OAuth consent and token exchange', 'Provider token validation']},
    ],
    edges: [
        ['browser', 'frontend', 'HTTPS'],
        ['frontend', 'backend', 'API Calls'],
        ['backend', 'postgres', 'SQL'],
        ['backend', 'redis', 'Rate Limit & Cache'],
        ['backend', 'oauth', 'OAuth2'],
    ],
};

export const sequenceDefinitions = {
    actors: ['User', 'Browser', 'Frontend', 'Backend', 'Database', 'OAuth Provider'],
    flows: {
        'Password Login': {
            Success: ['User → Browser: Enter credentials', 'Browser → Frontend: Submit login form', 'Frontend → Backend: POST /auth/login', 'Backend → Database: Verify user + password hash', 'Database → Backend: User valid', 'Backend → Frontend: Access + Refresh tokens', 'Frontend → Browser: Persist secure cookie/session'],
            'Invalid Password': ['User → Browser: Enter credentials', 'Frontend → Backend: POST /auth/login', 'Backend → Database: Verify user hash', 'Database → Backend: Password mismatch', 'Backend → Frontend: 401 invalid credentials', 'Frontend → Browser: Show retry + delay'],
            'User Not Found': ['User → Browser: Enter credentials', 'Frontend → Backend: POST /auth/login', 'Backend → Database: Lookup identifier', 'Database → Backend: No record', 'Backend → Frontend: 404/401 account not found', 'Frontend → Browser: Suggest signup flow'],
            'Token Expired': ['Browser → Frontend: API call with expired access token', 'Frontend → Backend: Request protected resource', 'Backend → Frontend: 401 token expired', 'Frontend → Backend: POST /auth/refresh', 'Backend → Frontend: New access token'],
            'Replay Attack': ['Attacker → Backend: Reuse old refresh token', 'Backend → Database: Check token family', 'Database → Backend: Token already rotated', 'Backend → Frontend: Revoke session + deny request'],
        },
        'OAuth Login': {
            Success: ['User → Frontend: Select Google login', 'Frontend → OAuth Provider: Redirect auth request', 'OAuth Provider → Browser: Consent + callback code', 'Frontend → Backend: Exchange provider code', 'Backend → OAuth Provider: Verify token', 'Backend → Frontend: Local session tokens issued'],
            'Invalid Password': ['OAuth flow: Not applicable'],
            'User Not Found': ['User → OAuth Provider: Authenticate', 'Frontend → Backend: Exchange provider code', 'Backend → Database: Lookup linked account', 'Database → Backend: No linked account', 'Backend → Frontend: Trigger account linking/signup'],
            'Token Expired': ['Frontend → Backend: OAuth session API with expired access token', 'Backend → Frontend: Refresh required', 'Frontend → Backend: Rotate refresh token', 'Backend → Frontend: Continue session'],
            'Replay Attack': ['Attacker → Backend: Replay old OAuth-derived refresh token', 'Backend → Database: Detect rotated token', 'Backend → Frontend: Session revoked'],
        },
        'Token Refresh': {
            Success: ['Browser → Frontend: Access token expired', 'Frontend → Backend: POST /auth/refresh', 'Backend → Database: Validate refresh hash', 'Database → Backend: Valid and active', 'Backend → Database: Rotate refresh hash', 'Backend → Frontend: New access + refresh token'],
            'Invalid Password': ['Refresh flow: Not applicable'],
            'User Not Found': ['Frontend → Backend: POST /auth/refresh', 'Backend → Database: Session owner lookup', 'Database → Backend: User deleted', 'Backend → Frontend: Revoke session + require login'],
            'Token Expired': ['Frontend → Backend: POST /auth/refresh', 'Backend → Database: Validate expiry', 'Database → Backend: Refresh expired', 'Backend → Frontend: 401 re-login required'],
            'Replay Attack': ['Attacker → Backend: Reuse old refresh token', 'Backend → Database: Compare token family', 'Database → Backend: Rotation mismatch', 'Backend → Frontend: Revoke entire session chain'],
        },
    },
};

export const stateMachineDefinition = {
    states: ['Unauthenticated', 'Credentials Submitted', 'Password Verified', 'Token Issued', 'Session Active', 'Token Expired', 'Refresh Attempted', 'Session Revoked'],
    transitions: [
        {from: 'Unauthenticated', to: 'Credentials Submitted', label: 'Submit credentials'},
        {from: 'Credentials Submitted', to: 'Password Verified', label: 'Verify password'},
        {from: 'Password Verified', to: 'Token Issued', label: 'Generate JWT/refresh'},
        {from: 'Token Issued', to: 'Session Active', label: 'Store session metadata'},
        {from: 'Session Active', to: 'Token Expired', label: 'Access TTL reached'},
        {from: 'Token Expired', to: 'Refresh Attempted', label: 'Try refresh token'},
        {from: 'Refresh Attempted', to: 'Session Active', label: 'Refresh success'},
        {from: 'Refresh Attempted', to: 'Session Revoked', label: 'Replay detected / revoked'},
        {from: 'Session Revoked', to: 'Unauthenticated', label: 'Re-authenticate'},
    ],
};

export const securityPipeline = ['Incoming Request', 'Redis Rate Limiter', 'Spring Security Filters', 'JWT Verification', 'Role Authorization', 'Controller'];

export const erDefinition = {
    tables: {
        users: ['id (PK)', 'email', 'phone', 'password_hash', 'status', 'created_at'],
        sessions: ['id (PK)', 'user_id (FK)', 'device_id', 'ip', 'user_agent', 'revoked'],
        refresh_tokens: ['id (PK)', 'session_id (FK)', 'token_hash', 'expires_at', 'rotated_at'],
        roles: ['id (PK)', 'name', 'description'],
        audit_logs: ['id (PK)', 'user_id (FK)', 'event_type', 'ip', 'created_at'],
    },
    relations: [
        ['users', 'sessions', '1:N'],
        ['sessions', 'refresh_tokens', '1:N'],
        ['users', 'roles', 'N:M'],
        ['users', 'audit_logs', '1:N'],
    ],
};

export const deploymentTopology = {
    nodes: ['User', 'Vercel CDN', 'DuckDNS Domain', 'Spring Boot Backend', 'PostgreSQL', 'Redis'],
    path: ['User', 'Vercel CDN', 'DuckDNS Domain', 'Spring Boot Backend', 'PostgreSQL'],
    cachePath: ['Spring Boot Backend', 'Redis'],
};
