import React, {useEffect, useState} from 'react';
import {apiService} from '../services/apiService';
import {AuthContext} from './authContext';
import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device.js';

export const AuthProvider = ({children}) => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [user, setUser] = useState(null);

    useEffect(() => {
        checkAuthStatus();
    }, []);

    const checkAuthStatus = async () => {
        try {
            const wasAuthenticated = localStorage.getItem('isAuthenticated') === 'true';
            const storedUser = localStorage.getItem('user');
            if (storedUser) {
                setUser(JSON.parse(storedUser));
            }

            if (!wasAuthenticated) {
                setIsAuthenticated(false);
                return;
            }

            const sessionResult = await apiService.get(API_CONFIG.ENDPOINTS.SESSIONS_ALL);
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
    };

    const login = (userData = null) => {
        const storedUserRaw = localStorage.getItem('user');
        const storedUser = storedUserRaw ? JSON.parse(storedUserRaw) : null;
        const resolvedUser = userData || storedUser;

        setIsAuthenticated(true);
        setUser(resolvedUser || null);
        localStorage.setItem('isAuthenticated', 'true');

        if (resolvedUser) {
            localStorage.setItem('user', JSON.stringify(resolvedUser));
        }
    };

    const logout = async () => {
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
            apiService.resetRateLimitInfo();
        }
    };

    const value = {
        isAuthenticated,
        isLoading,
        user,
        login,
        logout,
        checkAuthStatus,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
