import {clearAuthClientState} from './authSession';
import {clearAccessToken, getAccessToken, setAccessToken} from './tokenStore';

const LOCAL_TOKEN_KEY = 'authToken';

export const setToken = (token) => {
    if (!token || typeof token !== 'string') {
        return null;
    }

    const normalized = token.trim();
    if (!normalized) {
        return null;
    }

    localStorage.setItem(LOCAL_TOKEN_KEY, normalized);
    setAccessToken(normalized);
    return normalized;
};

export const getToken = () => {
    return getAccessToken() || localStorage.getItem(LOCAL_TOKEN_KEY);
};

export const removeToken = () => {
    localStorage.removeItem(LOCAL_TOKEN_KEY);
    clearAccessToken();
};

export const logout = () => {
    removeToken();
    clearAuthClientState();
};

