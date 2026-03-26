import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {apiService} from '../services/apiService';
import {AuthContext} from './authContext';
import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device.js';

const IDENTIFIER_HINT_KEY = 'auth_identifier_hint';

export const AuthProvider = ({children}) => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [user, setUser] = useState(null);

    const checkAuthStatus = useCallback(async () => {
        try {
            const hasAuthSessionFlag = localStorage.getItem('isAuthenticated') === 'true';
            const storedUserRaw = localStorage.getItem('user');
            const identifierHint = localStorage.getItem(IDENTIFIER_HINT_KEY);
            const hasBootstrapAuthSignal = hasAuthSessionFlag || Boolean(storedUserRaw) || Boolean(identifierHint);

            if (!hasBootstrapAuthSignal) {
                setIsAuthenticated(false);
                setUser(null);
                return;
            }

            let hydratedUser = null;

            if (storedUserRaw) {
                hydratedUser = JSON.parse(storedUserRaw);
            } else if (identifierHint) {
                hydratedUser = {identifier: identifierHint};
                localStorage.setItem('user', JSON.stringify(hydratedUser));
            }

            if (hydratedUser) {
                setUser(hydratedUser);
            }

            const sessionResult = await apiService.get(API_CONFIG.ENDPOINTS.SESSIONS_ALL, {skipAuthRefresh: true});
            if (!apiService.isErrorResponse(sessionResult) && Array.isArray(sessionResult?.data)) {
                setIsAuthenticated(true);
                localStorage.setItem('isAuthenticated', 'true');
                return;
            }

            setIsAuthenticated(false);
            setUser(null);
            localStorage.removeItem('isAuthenticated');
            localStorage.removeItem('user');
        } catch (error) {
            console.error('Auth check failed:', error);
            setIsAuthenticated(false);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        void checkAuthStatus();
    }, [checkAuthStatus]);

    const login = useCallback((userData = null) => {
        const storedUserRaw = localStorage.getItem('user');
        const storedUser = storedUserRaw ? JSON.parse(storedUserRaw) : null;
        const resolvedUser = userData || storedUser;

        setIsAuthenticated(true);
        setUser(resolvedUser || null);
        localStorage.setItem('isAuthenticated', 'true');

        if (resolvedUser) {
            localStorage.setItem('user', JSON.stringify(resolvedUser));
            if (resolvedUser.identifier) {
                localStorage.setItem(IDENTIFIER_HINT_KEY, resolvedUser.identifier);
            }
        }
    }, []);

    const logout = useCallback(async () => {
        try {
            await apiService.post(API_CONFIG.ENDPOINTS.LOGOUT, {
                device_id: getDeviceId(),
            });
        } catch (error) {
            console.error('Logout API failed:', error);
        } finally {
            setIsAuthenticated(false);
            setUser(null);
            localStorage.removeItem('isAuthenticated');
            localStorage.removeItem('user');
            localStorage.removeItem(IDENTIFIER_HINT_KEY);
            apiService.resetRateLimitInfo();
        }
    }, []);

    const patchUser = useCallback((nextFields = {}) => {
        setUser((previousUser) => {
            const mergedUser = {
                ...(previousUser || {}),
                ...nextFields,
            };

            localStorage.setItem('user', JSON.stringify(mergedUser));
            if (mergedUser.identifier) {
                localStorage.setItem(IDENTIFIER_HINT_KEY, mergedUser.identifier);
            }

            return mergedUser;
        });
    }, []);

    const value = useMemo(() => ({
        isAuthenticated,
        isLoading,
        user,
        login,
        logout,
        checkAuthStatus,
        patchUser,
    }), [isAuthenticated, isLoading, user, login, logout, checkAuthStatus, patchUser]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
