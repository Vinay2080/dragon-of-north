import React, {useEffect, useState} from 'react';
import {apiService} from '../services/apiService';
import {AuthContext} from './authContext';

/**
 * Auth Provider Component
 * Manages authentication state across the application
 */
export const AuthProvider = ({children}) => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [user, setUser] = useState(null);

    useEffect(() => {
        // Check if a user is authenticated on the mount
        checkAuthStatus();
    }, []);

    /**
     * Check authentication status
     * For now, we'll check if there's a session by trying to access a protected endpoint
     * or by checking localStorage
     */
    const checkAuthStatus = async () => {
        try {
            // Check if user was previously authenticated (stored in localStorage)
            const storedAuth = localStorage.getItem('isAuthenticated');
            if (storedAuth === 'true') {
                setIsAuthenticated(true);
            }
        } catch (error) {
            console.error('Auth check failed:', error);
            setIsAuthenticated(false);
        } finally {
            setIsLoading(false);
        }
    };

    /**
     * Login function
     * Sets authentication state after successful login
     */
    const login = (userData = null) => {
        setIsAuthenticated(true);
        setUser(userData);
        localStorage.setItem('isAuthenticated', 'true');
        if (userData) {
            localStorage.setItem('user', JSON.stringify(userData));
        }
    };

    /**
     * Logout function
     * Calls logout API and clears authentication state
     */
    const logout = async () => {
        try {
            // Call logout API
            await apiService.post('/api/v1/auth/identifier/logout');
        } catch (error) {
            console.error('Logout API failed:', error);
            // Continue with local logout even if API fails
        } finally {
            // Clear local state
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
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
