import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device';
import {mapErrorCodeToMessage} from '../utils/errorMapper';
import {exponentialBackoffDelay, shouldRetryRequest, wait} from '../utils/networkUtils';

class ApiService {
    constructor() {
        this.rateLimitInfo = {remaining: null, capacity: null, retryAfter: null};
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
        try {
            return await response.json();
        } catch {
            // If JSON parsing fails, return null instead of throwing
            return null;
        }
    }

    async refreshToken() {
        if (this.isRefreshing) {
            return this.refreshPromise;
        }

        this.isRefreshing = true;

        this.refreshPromise = fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.REFRESH_TOKEN}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            credentials: 'include',
            body: JSON.stringify({device_id: getDeviceId()}),
        }).then(res => {
            if (!res.ok) {
                throw new Error('Refresh failed');
            }
            return this.parseBody(res);
        }).finally(() => {
            this.isRefreshing = false;
        });

        return this.refreshPromise;
    }

    normalizeApiError(data, fallbackMessage) {
        const errorCode = data?.error_code || data?.errorCode || data?.code;
        const message = data?.message || data?.defaultMessage;
        const fieldErrors = data?.validation_error_list || data?.validationErrorList || [];
        return {
            errorCode,
            message: mapErrorCodeToMessage(errorCode, data),
            backendMessage: message,
            fieldErrors,
            raw: data,
            fallbackMessage,
        };
    }

    async request(endpoint, options = {}, retry = true, attempt = 0) {
        if (typeof navigator !== 'undefined' && !navigator.onLine) {
            return {
                type: 'NETWORK_ERROR',
                message: 'You appear to be offline. Please check your connection and try again.',
            };
        }

        const url = `${API_CONFIG.BASE_URL}${endpoint}`;
        const defaultOptions = {
            headers: {'Content-Type': 'application/json', ...options.headers},
            credentials: 'include',
            ...options,
        };

        try {
            const response = await fetch(url, defaultOptions);
            this.extractRateLimitHeaders(response);

            if (response.status === 401 && retry) {
                const data = await this.parseBody(response);

                // Only refresh token for actual token expiration, not for login authentication failures
                if (data?.error_code === 'TOK_001' && endpoint !== API_CONFIG.ENDPOINTS.LOGIN) {
                    try {
                        await this.refreshToken();
                        return this.request(endpoint, options, false, attempt);
                    } catch (refreshError) {
                        localStorage.removeItem('isAuthenticated');
                        localStorage.removeItem('user');
                        window.location.href = '/login';
                        throw refreshError;
                    }
                }
                // If it's not a token expiration, continue to normal error handling
            }

            const data = await this.parseBody(response);

            if (!response.ok) {
                const normalizedError = this.normalizeApiError(data, 'An error occurred');

                if (response.status === 429) {
                    return {
                        type: 'RATE_LIMIT_EXCEEDED',
                        status: response.status,
                        retryAfter: this.rateLimitInfo.retryAfter,
                        ...normalizedError,
                        data,
                    };
                }

                // Handle specific auth errors with better messages
                if (response.status === 401) {
                    return {
                        type: 'API_ERROR',
                        status: response.status,
                        message: data?.message || 'Invalid credentials. Please check your email and password.',
                        ...normalizedError,
                        data,
                    };
                }

                if (response.status === 403) {
                    return {
                        type: 'API_ERROR',
                        status: response.status,
                        message: data?.message || 'Access denied. You do not have permission to perform this action.',
                        ...normalizedError,
                        data,
                    };
                }

                if (attempt < 2 && shouldRetryRequest(response.status)) {
                    await wait(exponentialBackoffDelay(attempt));
                    return this.request(endpoint, options, retry, attempt + 1);
                }

                return {
                    type: 'API_ERROR',
                    status: response.status,
                    ...normalizedError,
                    data,
                };
            }

            return data;
        } catch (error) {
            if (attempt < 2) {
                await wait(exponentialBackoffDelay(attempt));
                return this.request(endpoint, options, retry, attempt + 1);
            }

            return {
                type: 'NETWORK_ERROR',
                message: 'Failed to connect to server. Please try again.',
                originalError: error,
            };
        }
    }

    get(endpoint, options = {}) { return this.request(endpoint, {...options, method: 'GET'}); }

    post(endpoint, body, options = {}) {
        return this.request(endpoint, {...options, method: 'POST', body: JSON.stringify(body)});
    }

    put(endpoint, body, options = {}) {
        return this.request(endpoint, {...options, method: 'PUT', body: JSON.stringify(body)});
    }

    delete(endpoint, options = {}) { return this.request(endpoint, {...options, method: 'DELETE'}); }

    getRateLimitInfo() { return {...this.rateLimitInfo}; }

    resetRateLimitInfo() {
        this.rateLimitInfo = {remaining: null, capacity: null, retryAfter: null};
        this.notifyRateLimitUpdate();
    }
}

export const apiService = new ApiService();
