import apiClient from '../api/client';

const TOKEN_KEY = 'auth_token';
const AUTH_EVENT = 'auth-token-changed';

const notifyAuthChange = () => {
    window.dispatchEvent(new Event(AUTH_EVENT));
};

const extractToken = (payload: any) => {
    return payload?.token || payload?.access_token || payload?.jwt || payload?.data?.token;
};

export const login = async (email: string, password: string) => {
    const response: any = await apiClient.post('/api/v1/auth/identifier/login', {
        email,
        password,
    });

    const token = extractToken(response?.data);

    if (token) {
        localStorage.setItem(TOKEN_KEY, token);
        notifyAuthChange();
    }

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
        localStorage.removeItem(TOKEN_KEY);
        notifyAuthChange();
    }
};

export const authEvents = {
    AUTH_EVENT,
};
