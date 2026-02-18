// API Configuration
export const API_CONFIG = {
    // Base URL for all API requests
    BASE_URL: import.meta.env.VITE_API_BASE_URL,

    // API Endpoints
    ENDPOINTS: {
        // Authentication
        IDENTIFIER_STATUS: '/api/v1/auth/identifier/status',
        SIGNUP: '/api/v1/auth/identifier/sign-up',
        SIGNUP_COMPLETE: '/api/v1/auth/identifier/sign-up/complete',
        LOGIN: '/api/v1/auth/identifier/login',
        LOGOUT: '/api/v1/auth/identifier/logout',
        REFRESH_TOKEN: '/api/v1/auth/jwt/refresh',

        // OTP
        EMAIL_OTP_REQUEST: '/api/v1/otp/email/request',
        EMAIL_OTP_VERIFY: '/api/v1/otp/email/verify',
        PHONE_OTP_REQUEST: '/api/v1/otp/phone/request',
        PHONE_OTP_VERIFY: '/api/v1/otp/phone/verify',
    },

    // Timeout for API requests (in milliseconds)
    TIMEOUT: 10000,

    // Default headers
    HEADERS: {
        'Content-Type': 'application/json',
    }
};

// Helper function to get full API URL
export const getApiUrl = (endpoint) => {
    return `${API_CONFIG.BASE_URL}${endpoint}`;
};
