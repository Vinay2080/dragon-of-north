import apiClient from '../api/client';
import {API_CONFIG} from '../config';
import {extractAccessToken} from './tokenStore';
import {getToken, removeToken, setToken} from './auth';
import {extractUserStatus, isDeletedUserStatus} from './authSession';

const AUTH_EVENT = 'auth-token-changed';

const notifyAuthChange = () => {
    window.dispatchEvent(new Event(AUTH_EVENT));
};

const extractToken = (payload: any) => {
    return extractAccessToken(payload);
};

const readCurrentUserStatus = async (): Promise<string | null> => {
    try {
        const meResponse: any = await apiClient.post(API_CONFIG.ENDPOINTS.PROFILE, undefined, {method: 'GET'} as any);
        return extractUserStatus(meResponse?.data || meResponse) || null;
    } catch {
        return null;
    }
};

export const login = async (email: string, password: string) => {
    let response: any;

    try {
        response = await apiClient.post('/api/auth/login', {
            email,
            password,
        });
    } catch {
        response = await apiClient.post('/api/v1/auth/identifier/login', {
            identifier: email,
            password,
        });
    }

    const token = extractToken(response?.data);

    if (token) {
        setToken(token);
    }

    const status = await readCurrentUserStatus();
    if (isDeletedUserStatus(status)) {
        removeToken();
        notifyAuthChange();
        throw new Error('Your account was deleted. Please sign up again.');
    }

    notifyAuthChange();
    return response?.data;
};

export const signup = async (email: string, password: string) => {
    const response: any = await apiClient.post('/api/v1/auth/identifier/sign-up', {
        email,
        password,
    });

    return response?.data;
};

export const logout = async () => {
    try {
        await apiClient.post('/api/v1/auth/identifier/logout');
    } finally {
        removeToken();
        notifyAuthChange();
    }
};

export const authEvents = {
    AUTH_EVENT,
};

export const tokenHelpers = {
    setToken,
    getToken,
    removeToken,
};
