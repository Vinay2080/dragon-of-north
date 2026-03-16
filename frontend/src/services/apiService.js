import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device';
import {mapErrorCodeToMessage} from '../utils/errorMapper';
import {exponentialBackoffDelay, shouldRetryRequest, wait} from '../utils/networkUtils';
import {CSRF_HEADER_NAME, ensureCsrfToken, isStateChangingMethod} from '../utils/csrf';

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

        this.refreshPromise = ensureCsrfToken().then(csrfToken => {
            return fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.REFRESH_TOKEN}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [CSRF_HEADER_NAME]: csrfToken,
                },
                credentials: 'include',
                body: JSON.stringify({device_id: getDeviceId()}),
            });
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

        const method = (defaultOptions.method || 'GET').toUpperCase();

        try {
            if (isStateChangingMethod(method)) {
                const csrfToken = await ensureCsrfToken();
                defaultOptions.headers = {
                    ...defaultOptions.headers,
                    [CSRF_HEADER_NAME]: csrfToken,
                };
            }

            const response = await fetch(url, defaultOptions);
            this.extractRateLimitHeaders(response);

            let data = null;

            if (response.status === 401 && retry) {
                data = await this.parseBody(response);
                const shouldAttemptRefresh = ![
                    API_CONFIG.ENDPOINTS.LOGIN,
                    API_CONFIG.ENDPOINTS.REFRESH_TOKEN,
                    API_CONFIG.ENDPOINTS.LOGOUT,
                    API_CONFIG.ENDPOINTS.CSRF,
                ].includes(endpoint);

                if (shouldAttemptRefresh) {
                    try {
                        await this.refreshToken();
                        return this.request(endpoint, options, false, attempt);
                    } catch (refreshError) {
                        console.error('Token refresh failed:', refreshError);
                        // Refresh failure should degrade to a normal 401 flow.
                        // Avoid mutating auth storage in the API layer; auth context owns that state.
                    }
                }
                // If it's not a token expiration, continue to normal error handling
            }

            if (data === null) {
                data = await this.parseBody(response);
            }

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
                    const defaultMessage = endpoint === API_CONFIG.ENDPOINTS.LOGIN
                        ? 'Invalid email or password. Please check your credentials and try again.'
                        : 'Authentication failed. Please log in again.';
                    return {
                        type: 'API_ERROR',
                        status: response.status,
                        ...normalizedError,
                        message: data?.message || defaultMessage,
                        data,
                    };
                }

                if (response.status === 403) {
                    return {
                        type: 'API_ERROR',
                        status: response.status,
                        ...normalizedError,
                        message: data?.message || 'Too many failed attempts. Please wait a few minutes before trying again.',
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
