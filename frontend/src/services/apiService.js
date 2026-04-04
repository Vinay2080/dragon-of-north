import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device';
import {mapErrorCodeToMessage} from '../utils/errorMapper';
import {exponentialBackoffDelay, shouldRetryRequest, wait} from '../utils/networkUtils';
import {ensureCsrfHeader} from '../utils/csrf';
import {clearAccessToken, extractAccessToken, getAccessToken, setAccessToken} from './tokenStore';
import {clearAuthClientState, extractUserStatus, isDeletedUserStatus} from './authSession';

const AUTO_REFRESH_EXCLUDED_ENDPOINTS = new Set([
    API_CONFIG.ENDPOINTS.LOGIN,
    API_CONFIG.ENDPOINTS.REFRESH_TOKEN,
    API_CONFIG.ENDPOINTS.LOGOUT,
    API_CONFIG.ENDPOINTS.PASSWORD_CHANGE,
    API_CONFIG.ENDPOINTS.PASSWORD_RESET_REQUEST,
    API_CONFIG.ENDPOINTS.PASSWORD_RESET_CONFIRM,
]);

class ApiService {
    constructor() {
        this.rateLimitInfo = {remaining: null, capacity: null, retryAfter: null};
        this.rateLimitListeners = [];
        this.isRefreshing = false;
        this.refreshPromise = null;
        this.authFailureListeners = [];
        this.authFailureInProgress = false;
    }

    onAuthFailure(callback) {
        this.authFailureListeners.push(callback);
        return () => {
            this.authFailureListeners = this.authFailureListeners.filter((listener) => listener !== callback);
        };
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

    isDeletedStatusPayload(payload) {
        return isDeletedUserStatus(extractUserStatus(payload));
    }

    notifyAuthFailure(reason, payload = null) {
        if (this.authFailureInProgress) {
            return;
        }

        this.authFailureInProgress = true;
        clearAuthClientState();

        if (this.authFailureListeners.length > 0) {
            this.authFailureListeners.forEach((listener) => {
                try {
                    listener({reason, payload});
                } catch (listenerError) {
                    console.error('Auth failure listener error:', listenerError);
                }
            });
        } else if (typeof window !== 'undefined') {
            window.location.assign('/signup');
        }

        setTimeout(() => {
            this.authFailureInProgress = false;
        }, 0);
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

        this.refreshPromise = (async () => {
            const requestOptions = await ensureCsrfHeader({
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({device_id: getDeviceId()}),
            });

            return fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.REFRESH_TOKEN}`, requestOptions);
        })().then(async (res) => {
            if (!res.ok) {
                clearAccessToken();
                throw new Error('Refresh failed');
            }

            const responseData = await this.parseBody(res);
            this.persistAccessToken(responseData);
            return responseData;
        }).finally(() => {
            this.isRefreshing = false;
        });

        return this.refreshPromise;
    }

    normalizeApiError(data, fallbackMessage) {
        // Backend failures are wrapped in ApiResponse { apiResponseStatus, data: { code, defaultMessage, ... } }
        const errorPayload = data?.data || data;
        const errorCode = errorPayload?.error_code || errorPayload?.errorCode || errorPayload?.code;
        const message =
            errorPayload?.message ||
            errorPayload?.defaultMessage ||
            data?.message ||
            data?.defaultMessage;
        const isExpiredJwtMessage = typeof message === 'string' && /expired\s+jwt|jwt\s+expired|token\s+expired/i.test(message);
        const fieldErrors =
            errorPayload?.validation_error_list ||
            errorPayload?.validationErrorList ||
            data?.validation_error_list ||
            data?.validationErrorList ||
            [];
        return {
            errorCode,
            message: isExpiredJwtMessage
                ? 'Your session has expired. Please log in again.'
                : mapErrorCodeToMessage(errorCode, errorPayload || data),
            backendMessage: message,
            fieldErrors,
            raw: errorPayload || data,
            fallbackMessage,
        };
    }

    persistAccessToken(payload) {
        const token = extractAccessToken(payload);
        if (token) {
            setAccessToken(token);
        }
    }

    async request(endpoint, options = {}, retry = true, attempt = 0) {
        if (typeof navigator !== 'undefined' && !navigator.onLine) {
            return {
                type: 'NETWORK_ERROR',
                message: 'You appear to be offline. Please check your connection and try again.',
            };
        }

        const {skipAuthRefresh = false, ...requestOptions} = options;
        const url = `${API_CONFIG.BASE_URL}${endpoint}`;

        let defaultOptions = {
            headers: {'Content-Type': 'application/json', ...requestOptions.headers},
            credentials: 'include',
            ...requestOptions,
        };

        const accessToken = getAccessToken();
        if (accessToken) {
            defaultOptions.headers.Authorization = `Bearer ${accessToken}`;
        }

        defaultOptions = await ensureCsrfHeader(defaultOptions);

        try {
            const response = await fetch(url, defaultOptions);
            this.extractRateLimitHeaders(response);

            let data = null;

            if (response.status === 401 && retry) {
                data = await this.parseBody(response);
                const shouldAttemptRefresh = !skipAuthRefresh && !AUTO_REFRESH_EXCLUDED_ENDPOINTS.has(endpoint);

                if (shouldAttemptRefresh) {
                    try {
                        await this.refreshToken();
                        return this.request(endpoint, options, false, attempt);
                    } catch (refreshError) {
                        clearAccessToken();
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

                if (response.status === 401) {
                    this.notifyAuthFailure('UNAUTHORIZED', data);
                }

                if (this.isDeletedStatusPayload(data)) {
                    this.notifyAuthFailure('DELETED', data);
                }

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
                    clearAccessToken();
                    const defaultMessage = endpoint === API_CONFIG.ENDPOINTS.LOGIN
                        ? 'Invalid email or password. Please check your credentials and try again.'
                        : 'Authentication failed. Please log in again.';
                    return {
                        type: 'API_ERROR',
                        status: response.status,
                        ...normalizedError,
                        message: normalizedError.message || normalizedError.backendMessage || defaultMessage,
                        data,
                    };
                }

                if (response.status === 403) {
                    return {
                        type: 'API_ERROR',
                        status: response.status,
                        ...normalizedError,
                        message: normalizedError.message || normalizedError.backendMessage || 'Access denied. Please log in again.',
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

            if (this.isDeletedStatusPayload(data)) {
                this.notifyAuthFailure('DELETED', data);
                return {
                    type: 'API_ERROR',
                    status: 401,
                    errorCode: 'ACCOUNT_DELETED',
                    message: 'Your account has been deleted. Please sign up again to continue.',
                    data,
                };
            }

            this.persistAccessToken(data);

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

    patch(endpoint, body, options = {}) {

        return this.request(endpoint, {...options, method: 'PATCH', body: JSON.stringify(body)});
    }

    delete(endpoint, options = {}) { return this.request(endpoint, {...options, method: 'DELETE'}); }

    getRateLimitInfo() { return {...this.rateLimitInfo}; }

    resetRateLimitInfo() {
        this.rateLimitInfo = {remaining: null, capacity: null, retryAfter: null};
        this.notifyRateLimitUpdate();
    }
}

export const apiService = new ApiService();
