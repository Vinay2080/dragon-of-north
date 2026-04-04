import {clearAccessToken} from './tokenStore';

const AUTH_STORAGE_KEYS = [
    'isAuthenticated',
    'user',
    'auth_identifier_hint',
    'authToken',
    'access_token',
    'accessToken',
    'token',
    'jwt',
];

const expireClientCookies = () => {
    if (typeof document === 'undefined' || !document.cookie) {
        return;
    }

    const cookiePairs = document.cookie.split(';').map((entry) => entry.trim()).filter(Boolean);
    cookiePairs.forEach((cookiePair) => {
        const [cookieName] = cookiePair.split('=');
        if (!cookieName) {
            return;
        }

        document.cookie = `${cookieName}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
    });
};

export const clearAuthClientState = () => {
    clearAccessToken();

    AUTH_STORAGE_KEYS.forEach((key) => {
        localStorage.removeItem(key);
        sessionStorage.removeItem(key);
    });

    expireClientCookies();
};

export const isDeletedUserStatus = (status) => {
    return typeof status === 'string' && status.toUpperCase() === 'DELETED';
};

export const extractUserStatus = (payload) => {
    if (!payload || typeof payload !== 'object') {
        return null;
    }

    if (typeof payload.status === 'string') {
        return payload.status;
    }

    const nested = payload.data;
    if (nested && typeof nested === 'object' && typeof nested.status === 'string') {
        return nested.status;
    }

    return null;
};

