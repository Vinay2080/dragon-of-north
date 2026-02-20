import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device';

class ApiService {
    constructor() {
        this.rateLimitInfo = {
            remaining: null,
            capacity: null,
            retryAfter: null,
        };
        this.rateLimitListeners = [];

        this.isRefreshing = false;
        this.refreshPromise = null;
    }

    onRateLimitUpdate(callback) {
        this.rateLimitListeners.push(callback);
        return () => {
            this.rateLimitListeners = this.rateLimitListeners.filter(cb => cb !== callback);
        };
    }

    notifyRateLimitUpdate() {
        this.rateLimitListeners.forEach(callback => callback(this.rateLimitInfo));
    }

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

    isErrorResponse(result) {
        return !!result && ['RATE_LIMIT_EXCEEDED', 'API_ERROR', 'NETWORK_ERROR'].includes(result.type);
    }

    async parseBody(response) {
        const contentType = response.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) {
            return null;
        }
        return response.json();
    }

    async refreshToken() {
        if (this.isRefreshing) {
            return this.refreshPromise;
        }

        this.isRefreshing = true;

        this.refreshPromise = fetch(
            `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.REFRESH_TOKEN}`,
            {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                credentials: 'include',
                body: JSON.stringify({
                    device_id: getDeviceId(),
                }),
            }
        ).then(res => {
            if (!res.ok) {
                throw new Error('Refresh failed');
            }
            return this.parseBody(res);
        }).finally(() => {
            this.isRefreshing = false;
        });

        return this.refreshPromise;
    }

    async request(endpoint, options = {}, retry = true) {
        const url = `${API_CONFIG.BASE_URL}${endpoint}`;

        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers,
            },
            credentials: 'include',
            ...options,
        };

        try {
            const response = await fetch(url, defaultOptions);
            this.extractRateLimitHeaders(response);

            if (response.status === 401 && retry) {
                try {
                    await this.refreshToken();
                    return this.request(endpoint, options, false);
                } catch (refreshError) {
                    localStorage.removeItem('isAuthenticated');
                    localStorage.removeItem('user');
                    window.location.href = '/login';
                    throw refreshError;
                }
            }

            const data = await this.parseBody(response);

            if (!response.ok) {
                if (response.status === 429) {
                    return {
                        type: 'RATE_LIMIT_EXCEEDED',
                        message: data?.message || 'Too many requests',
                        retryAfter: this.rateLimitInfo.retryAfter,
                        data,
                    };
                }

                return {
                    type: 'API_ERROR',
                    message: data?.message || 'An error occurred',
                    status: response.status,
                    data,
                };
            }

            return data;
        } catch (error) {
            return {
                type: 'NETWORK_ERROR',
                message: 'Failed to connect to server',
                originalError: error,
            };
        }
    }

    async get(endpoint, options = {}) {
        return this.request(endpoint, {...options, method: 'GET'});
    }

    async post(endpoint, body, options = {}) {
        return this.request(endpoint, {
            ...options,
            method: 'POST',
            body: JSON.stringify(body),
        });
    }

    async put(endpoint, body, options = {}) {
        return this.request(endpoint, {
            ...options,
            method: 'PUT',
            body: JSON.stringify(body),
        });
    }

    async delete(endpoint, options = {}) {
        return this.request(endpoint, {...options, method: 'DELETE'});
    }

    getRateLimitInfo() {
        return {...this.rateLimitInfo};
    }

    resetRateLimitInfo() {
        this.rateLimitInfo = {
            remaining: null,
            capacity: null,
            retryAfter: null,
        };
        this.notifyRateLimitUpdate();
    }
}

export const apiService = new ApiService();
