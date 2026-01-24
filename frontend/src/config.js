// API Configuration
export const API_CONFIG = {
    // Base URL for all API requests
    BASE_URL: 'http://52.71.153.142:8080',

    // API Endpoints
    ENDPOINTS: {
        SEND_OTP: '/api/v1/auth/send-otp',
        VERIFY_OTP: '/api/v1/auth/verify-otp',
        SIGNUP: '/api/v1/auth/signup',
        // Add more endpoints here as needed
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
