import {API_CONFIG} from '../config';

/**
 * API Service utility for making HTTP requests with rate limit handling
 */
class ApiService {
    constructor() {
        this.rateLimitInfo = {
            remaining: null,
            capacity: null,
            retryAfter: null,
        };
        this.rateLimitListeners = [];
    }

    /**
     * Subscribe to rate limit updates
     * @param {Function} callback - Function to call when rate limit info changes
     * @returns {Function} Unsubscribe function
     */
    onRateLimitUpdate(callback) {
        this.rateLimitListeners.push(callback);
        return () => {
            this.rateLimitListeners = this.rateLimitListeners.filter(cb => cb !== callback);
        };
    }

    /**
     * Notify all listeners of rate limit changes
     */
    notifyRateLimitUpdate() {
        this.rateLimitListeners.forEach(callback => callback(this.rateLimitInfo));
    }

    /**
     * Extract rate limit headers from response
     * @param {Response} response - Fetch API response
     */
    extractRateLimitHeaders(response) {
        const remaining = response.headers.get('X-RateLimit-Remaining');
        const capacity = response.headers.get('X-RateLimit-Capacity');
        const retryAfter = response.headers.get('Retry-After');

        this.rateLimitInfo = {
            remaining: remaining ? parseInt(remaining, 10) : null,
            capacity: capacity ? parseInt(capacity, 10) : null,
            retryAfter: retryAfter ? parseInt(retryAfter, 10) : null,
        };

        this.notifyRateLimitUpdate();
    }

    /**
     * Make an API request with rate limit handling
     * @param {string} endpoint - API endpoint
     * @param {Object} options - Fetch options
     * @returns {Promise<Object>} Response data
     */
    async request(endpoint, options = {}) {
        const url = `${API_CONFIG.BASE_URL}${endpoint}`;

        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers,
            },
            credentials: 'include', // Important for cookies
            ...options,
        };

        try {
            const response = await fetch(url, defaultOptions);

            // Extract rate limit headers
            this.extractRateLimitHeaders(response);

            const data = await response.json();

            if (!response.ok) {
                // Handle rate limit exceeded
                if (response.status === 429) {
                    return {
                        type: 'RATE_LIMIT_EXCEEDED',
                        message: data.message || 'Too many requests. Please try again later.',
                        retryAfter: this.rateLimitInfo.retryAfter,
                        data,
                    };
                }

                // Handle other errors
                return {
                    type: 'API_ERROR',
                    message: data.message || 'An error occurred',
                    status: response.status,
                    data,
                };
            }

            return data;
        } catch (error) {
            // Network or other errors
            return {
                type: 'NETWORK_ERROR',
                message: 'Failed to connect to the server. Please try again later.',
                originalError: error,
            };
        }
    }



    /**
     * POST request
     */
    async post(endpoint, options = {}) {
        return this.request(endpoint, {
            ...options,
            method: 'POST',
            body: options.body,
        });
    }


    /**
     * Get current rate limit info
     */
    getRateLimitInfo() {
        return {...this.rateLimitInfo};
    }

    /**
     * Reset rate limit info
     */
    resetRateLimitInfo() {
        this.rateLimitInfo = {
            remaining: null,
            capacity: null,
            retryAfter: null,
        };
        this.notifyRateLimitUpdate();
    }
}

// Export singleton instance
export const apiService = new ApiService();
