// API Configuration
export const API_CONFIG = {
    // Base URL for all API requests
    BASE_URL: import.meta.env.VITE_API_BASE_URL,

    // API Endpoints
    ENDPOINTS: {
        // Authentication
        CSRF: '/api/v1/auth/csrf',                         // GET
        IDENTIFIER_STATUS: '/api/v1/auth/identifier/status', // GET
        SIGNUP: '/api/v1/auth/identifier/sign-up',         // POST
        SIGNUP_COMPLETE: '/api/v1/auth/identifier/sign-up/complete', // POST
        LOGIN: '/api/v1/auth/identifier/login',            // POST
        LOGOUT: '/api/v1/auth/identifier/logout',          // POST
        REFRESH_TOKEN: '/api/v1/auth/jwt/refresh',         // POST
        OAUTH_GOOGLE: '/api/v1/auth/oauth/google',         // POST
        OAUTH_GOOGLE_SIGNUP: '/api/v1/auth/oauth/google/signup', // POST
        PASSWORD_RESET_REQUEST: '/api/v1/auth/password/forgot/request', // POST
        PASSWORD_RESET_CONFIRM: '/api/v1/auth/password/forgot/reset',   // POST
        PASSWORD_CHANGE: '/api/v1/auth/password',          // PATCH
        ACCOUNT_DELETE: '/api/v1/auth/account',      // POST


        // Login MFA
        MFA_VERIFY: '/api/v1/auth/mfa/verify',             // POST

        // MFA Setup
        MFA_SETUP_REQUEST: '/api/v1/auth/mfa/setup',       // POST
        MFA_SETUP_CONFIRM: '/api/v1/auth/mfa/setup/confirm', // POST

        // Step-up MFA
        STEP_UP_MFA_REQUEST: '/api/v1/auth/mfa/request', // POST
        STEP_UP_MFA_VERIFY: '/api/v1/auth/step-up/mfa/verify',   // POST
        MFA_STATUS: '/api/v1/auth/mfa/status',             // GET
        MFA_DISABLE: '/api/v1/auth/mfa/disable',           // POST

        // Passwordless
        PASSWORDLESS_REQUEST: '/api/v1/auth/login/passwordless/request', // POST
        PASSWORDLESS_VERIFY: '/api/v1/auth/login/passwordless/verify',   // POST
        PASSWORDLESS_REQUEST_FALLBACK: '/api/v1/auth/passwordless/request', // POST
        PASSWORDLESS_VERIFY_FALLBACK: '/api/v1/auth/passwordless/verify',   // POST

        // Profile
        PROFILE: '/api/v1/profile',                        // GET / PATCH
        PROFILE_IMAGE_UPLOAD: '/api/v1/profile/image',    // POST

        // OTP
        EMAIL_OTP_REQUEST: '/api/v1/otp/email/request',   // POST
        EMAIL_OTP_VERIFY: '/api/v1/otp/email/verify',     // POST
        PHONE_OTP_REQUEST: '/api/v1/otp/phone/request',   // POST
        PHONE_OTP_VERIFY: '/api/v1/otp/phone/verify',     // POST

        // Sessions
        SESSIONS: '/api/v1/sessions',                      // GET
        SESSION_REVOKE: (sessionId) => `/api/v1/sessions/${sessionId}`, // DELETE
        SESSION_REVOKE_OTHERS: '/api/v1/sessions/revoke-others', // POST
    },

    // Google Identity Services Client ID
    GOOGLE_CLIENT_ID: import.meta.env.VITE_GOOGLE_CLIENT_ID,

    // Timeout for API requests (in milliseconds)
    TIMEOUT: 10000,

    // Default headers
    HEADERS: {
        'Content-Type': 'application/json',
    },

    // CSRF token settings used by frontend clients.
    CSRF_COOKIE_NAME: 'XSRF-TOKEN',
    CSRF_HEADER_NAME: 'X-XSRF-TOKEN',
};

// Helper function to get full API URL
export const getApiUrl = (endpoint) => {
    return `${API_CONFIG.BASE_URL}${endpoint}`;
};
