import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {apiService} from '../services/apiService';
import {AuthContext} from './authContext';
import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device.js';
import {clearAuthClientState, isDeletedUserStatus} from '../services/authSession';
import MfaChallengeModal from '../components/auth/MfaChallengeModal';

const IDENTIFIER_HINT_KEY = 'auth_identifier_hint';

const extractResponseData = (result) => {
    if (result?.data && typeof result.data === 'object' && !Array.isArray(result.data)) {
        return result.data;
    }

    if (result && typeof result === 'object' && !Array.isArray(result) && !result.type) {
        return result;
    }

    return null;
};

const normalizeUserPayload = (payload = {}) => {
    if (!payload) {
        return null;
    }

    return {
        ...payload,
        username: payload?.username || payload?.user_name || '',
        displayName: payload?.displayName || payload?.display_name || '',
        bio: payload?.bio || '',
        avatarUrl: payload?.avatarUrl || payload?.avatar_url || '',
        authProvider: payload?.authProvider || payload?.auth_provider || null,
        status: payload?.status || payload?.user_status || payload?.userStatus || null,
        mfaEnabled: payload?.mfaEnabled || payload?.mfa_enabled || false,
    };
};

export const AuthProvider = ({children}) => {
    const navigate = useNavigate();
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [user, setUser] = useState(null);

    const [stepUpMfaChallenge, setStepUpMfaChallenge] = useState(null);
    const [stepUpError, setStepUpError] = useState('');
    const [isStepUpSubmitting, setIsStepUpSubmitting] = useState(false);
    const stepUpPendingRef = useRef(null);

    useEffect(() => {
        const handler = (challenge) => {
            if (stepUpPendingRef.current) {
                return stepUpPendingRef.current.promise;
            }

            let resolvePromise, rejectPromise;
            const promise = new Promise((resolve, reject) => {
                resolvePromise = resolve;
                rejectPromise = reject;
            });

            stepUpPendingRef.current = { promise, resolve: resolvePromise, reject: rejectPromise };
            
            setStepUpMfaChallenge({
                challenge_id: challenge.challenge_id,
                available_methods: Array.isArray(challenge.available_methods) ? challenge.available_methods : [],
            });

            return promise;
        };

        apiService.registerStepUpMfaHandler(handler);
        return () => apiService.registerStepUpMfaHandler(null);
    }, []);

    const handleStepUpMfaSubmit = useCallback(async ({providerType, code}) => {
        if (!stepUpMfaChallenge?.challenge_id || isStepUpSubmitting) return;

        setIsStepUpSubmitting(true);
        setStepUpError('');

        try {
            const result = await apiService.post(
                API_CONFIG.ENDPOINTS.STEP_UP_MFA_VERIFY,
                {
                    challenge_id: stepUpMfaChallenge.challenge_id,
                    provider_type: providerType,
                    code,
                    device_id: getDeviceId(),
                },
                {skipAuthRefresh: true}
            );

            if (apiService.isErrorResponse(result) || result?.api_response_status !== 'success') {
                setStepUpError(result?.backendMessage || result?.message || 'Unable to verify this code. Please try again.');
                return;
            }

            setStepUpMfaChallenge(null);
            setStepUpError('');
            
            if (stepUpPendingRef.current) {
                stepUpPendingRef.current.resolve();
                stepUpPendingRef.current = null;
            }
        } finally {
            setIsStepUpSubmitting(false);
        }
    }, [stepUpMfaChallenge, isStepUpSubmitting]);

    const handleStepUpMfaCancel = useCallback(() => {
        if (isStepUpSubmitting) return;
        
        setStepUpMfaChallenge(null);
        setStepUpError('');

        if (stepUpPendingRef.current) {
            stepUpPendingRef.current.reject(new Error('MFA verification cancelled.'));
            stepUpPendingRef.current = null;
        }
    }, [isStepUpSubmitting]);

    const clearLocalAuthState = useCallback(() => {
        setIsAuthenticated(false);
        setUser(null);
        clearAuthClientState();
        apiService.resetRateLimitInfo();
    }, []);

    const forceLogout = useCallback(({redirectTo = '/signup'} = {}) => {
        clearLocalAuthState();
        if (redirectTo) {
            navigate(redirectTo, {replace: true});
        }
    }, [clearLocalAuthState, navigate]);

    const persistUserState = useCallback((nextUser) => {
        if (!nextUser) {
            localStorage.removeItem('user');
            return null;
        }

        const normalizedUser = normalizeUserPayload(nextUser);
        setUser(normalizedUser);
        localStorage.setItem('user', JSON.stringify(normalizedUser));
        if (normalizedUser?.identifier) {
            localStorage.setItem(IDENTIFIER_HINT_KEY, normalizedUser.identifier);
        }

        return normalizedUser;
    }, []);

    const syncUserProfile = useCallback(async (baseUser = null) => {
        const profileResult = await apiService.get(API_CONFIG.ENDPOINTS.PROFILE);

        if (apiService.isErrorResponse(profileResult)) {
            return baseUser;
        }

        const profilePayload = extractResponseData(profileResult);
        if (!profilePayload) {
            return baseUser;
        }

        const mergedUser = normalizeUserPayload({
            ...(baseUser || {}),
            ...profilePayload,
        });

        if (isDeletedUserStatus(mergedUser?.status)) {
            forceLogout({redirectTo: '/signup'});
            return null;
        }

        return persistUserState(mergedUser);
    }, [forceLogout, persistUserState]);

    const checkAuthStatus = useCallback(async () => {
        try {
            const storedUserRaw = localStorage.getItem('user');
            const identifierHint = localStorage.getItem(IDENTIFIER_HINT_KEY);

            let hydratedUser = null;

            if (storedUserRaw) {
                hydratedUser = JSON.parse(storedUserRaw);
            } else if (identifierHint) {
                hydratedUser = {identifier: identifierHint};
                localStorage.setItem('user', JSON.stringify(hydratedUser));
            }

            if (hydratedUser) {
                setUser(normalizeUserPayload(hydratedUser));
                if (isDeletedUserStatus(hydratedUser?.status)) {
                    forceLogout({redirectTo: '/signup'});
                    return;
                }
            }

            let refreshSucceeded = false;
            try {
                await apiService.refreshToken();
                refreshSucceeded = true;
            } catch (refreshError) {
                console.warn('Session refresh on startup failed:', refreshError);
            }

            if (!refreshSucceeded) {
                setIsAuthenticated(false);
                setUser(null);
                localStorage.removeItem('isAuthenticated');
                localStorage.removeItem('user');
                return;
            }

            const sessionResult = await apiService.get(API_CONFIG.ENDPOINTS.SESSIONS_ALL);
            if (!apiService.isErrorResponse(sessionResult) && Array.isArray(sessionResult?.data)) {
                setIsAuthenticated(true);
                localStorage.setItem('isAuthenticated', 'true');
                const syncedUser = await syncUserProfile(hydratedUser);
                if (!syncedUser) {
                    return;
                }
                return;
            }

            clearLocalAuthState();
        } catch (error) {
            console.error('Auth check failed:', error);
            clearLocalAuthState();
        } finally {
            setIsLoading(false);
        }
    }, [clearLocalAuthState, forceLogout, syncUserProfile]);

    useEffect(() => {
        return apiService.onAuthFailure(() => {
            forceLogout({redirectTo: '/signup'});
        });
    }, [forceLogout]);

    useEffect(() => {
        void checkAuthStatus();
    }, [checkAuthStatus]);

    const login = useCallback((userData = null) => {
        const storedUserRaw = localStorage.getItem('user');
        const storedUser = storedUserRaw ? JSON.parse(storedUserRaw) : null;
        const resolvedUser = normalizeUserPayload(userData || storedUser || null);

        setIsAuthenticated(true);
        setUser(resolvedUser || null);
        localStorage.setItem('isAuthenticated', 'true');

        if (resolvedUser) {
            localStorage.setItem('user', JSON.stringify(resolvedUser));
            if (resolvedUser.identifier) {
                localStorage.setItem(IDENTIFIER_HINT_KEY, resolvedUser.identifier);
            }
        }

        void syncUserProfile(resolvedUser);
    }, [syncUserProfile]);

    const logout = useCallback(async () => {
        try {
            await apiService.post(API_CONFIG.ENDPOINTS.LOGOUT, {
                device_id: getDeviceId(),
            });
        } catch (error) {
            console.error('Logout API failed:', error);
        } finally {
            clearLocalAuthState();
        }
    }, [clearLocalAuthState]);

    const patchUser = useCallback((nextFields = {}) => {
        setUser((previousUser) => {
            const mergedUser = normalizeUserPayload({
                ...(previousUser || {}),
                ...nextFields,
            });

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
        forceLogout,
        checkAuthStatus,
        patchUser,
        syncUserProfile,
        persistUserState,
    }), [isAuthenticated, isLoading, user, login, logout, forceLogout, checkAuthStatus, patchUser, syncUserProfile, persistUserState]);

    return (
        <AuthContext.Provider value={value}>
            {children}
            <MfaChallengeModal
                key={stepUpMfaChallenge?.challenge_id || 'step-up-mfa'}
                open={Boolean(stepUpMfaChallenge)}
                mode="step-up"
                availableMethods={stepUpMfaChallenge?.available_methods || []}
                error={stepUpError}
                isSubmitting={isStepUpSubmitting}
                onCancel={handleStepUpMfaCancel}
                onSubmit={handleStepUpMfaSubmit}
            />
        </AuthContext.Provider>
    );
};
