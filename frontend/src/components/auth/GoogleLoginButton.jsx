import {useCallback, useEffect, useRef, useState} from 'react';
import {API_CONFIG} from '../../config';
import {apiService} from '../../services/apiService';
import {getDeviceId} from '../../utils/device';

const GOOGLE_IDENTITY_SCRIPT_SRC = 'https://accounts.google.com/gsi/client';

const GoogleLoginButton = ({onSuccess, onError, onStart, disabled = false, autoPrompt = false, mode = 'login', expectedIdentifier = '', isRedirecting = false}) => {
    const buttonRef = useRef(null);
    const initializedRef = useRef(false);
    const expectedIdentifierRef = useRef(expectedIdentifier);
    const hasClientId = Boolean(API_CONFIG.GOOGLE_CLIENT_ID);
    const [isInitializing, setIsInitializing] = useState(hasClientId);
    const [isReady, setIsReady] = useState(false);

    useEffect(() => {
        expectedIdentifierRef.current = expectedIdentifier;
    }, [expectedIdentifier]);

    useEffect(() => {
        const buttonNode = buttonRef.current;
        if (!buttonNode || !onStart) return;

        const handleClick = () => {
            if (!disabled) {
                onStart();
            }
        };

        buttonNode.addEventListener('click', handleClick);

        return () => {
            buttonNode.removeEventListener('click', handleClick);
        };
    }, [disabled, onStart]);

    const decodeJWT = (token) => {
        try {
            const parts = token.split('.');
            if (parts.length !== 3) return null;

            return JSON.parse(atob(parts[1]));
        } catch (error) {
            console.error('Failed to decode JWT:', error);
            return null;
        }
    };

    const handleGoogleCredential = useCallback(async ({credential}) => {
        if (!credential) {
            onError?.('Google did not return an ID token.');
            return;
        }

        // Extract email from ID token
        const decodedToken = decodeJWT(credential);
        const emailFromToken = decodedToken?.email;

        // Backend endpoint depends on flow mode:
        // signup -> POST /api/v1/auth/oauth/google/signup, login -> POST /api/v1/auth/oauth/google
        const endpoint = mode === 'signup' ? API_CONFIG.ENDPOINTS.OAUTH_GOOGLE_SIGNUP : API_CONFIG.ENDPOINTS.OAUTH_GOOGLE;
        const payload = {
            // Backend payload contract for OAuth exchange.
            id_token: credential,
            // idToken: credential,
            device_id: getDeviceId(),
        };

        const normalizedExpectedIdentifier = expectedIdentifierRef.current?.trim().toLowerCase();
        if (normalizedExpectedIdentifier) {
            payload.expected_identifier = normalizedExpectedIdentifier;
            // payload.expectedIdentifier = normalizedExpectedIdentifier;
        }

        const result = await apiService.post(endpoint, payload);

        // Frontend expects standard ApiResponse with api_response_status === 'success'.
        if (apiService.isErrorResponse(result) || result?.api_response_status !== 'success') {
            const fallbackMessage = mode === 'signup'
                ? 'Google signup failed. Please try again.'
                : 'Google sign-in failed. Please try again.';
            onError?.(result?.message || fallbackMessage);
            return;
        }

        // Pass both the backend response and the email from a token
        onSuccess?.({
            ...result?.data,
            email: emailFromToken,
            identifier: emailFromToken
        });
    }, [mode, onError, onSuccess]);

    useEffect(() => {
        const clientId = API_CONFIG.GOOGLE_CLIENT_ID;
        if (!clientId) {
            onError?.('Google login is not configured. Missing client ID.');
            setTimeout(() => {
                setIsInitializing(false);
                setIsReady(false);
            }, 0);
            return;
        }

        const initializeGoogle = () => {
            if (initializedRef.current || !window.google?.accounts?.id || !buttonRef.current) {
                return;
            }

            window.google.accounts.id.initialize({
                client_id: clientId,
                callback: handleGoogleCredential,
            });

            buttonRef.current.innerHTML = '';
            const measuredWidth = buttonRef.current.clientWidth || 320;
            const buttonWidth = Math.min(360, Math.max(220, Math.floor(measuredWidth)));
            window.google.accounts.id.renderButton(buttonRef.current, {
                type: 'standard',
                theme: 'outline',
                text: 'continue_with',
                shape: 'pill',
                size: 'large',
                width: buttonWidth,
            });

            initializedRef.current = true;
            setTimeout(() => {
                setIsInitializing(false);
                setIsReady(true);
            }, 0);
        };

        if (window.google?.accounts?.id) {
            initializeGoogle();
            return;
        }

        const existingScript = document.querySelector(`script[src="${GOOGLE_IDENTITY_SCRIPT_SRC}"]`);
        const script = existingScript || document.createElement('script');
        if (!existingScript) {
            script.src = GOOGLE_IDENTITY_SCRIPT_SRC;
            script.async = true;
            script.defer = true;
            document.head.appendChild(script);
        }

        script.addEventListener('load', initializeGoogle);
        script.addEventListener('error', () => {
            setTimeout(() => {
                setIsInitializing(false);
                setIsReady(false);
            }, 0);
            onError?.('Unable to load Google Identity Services. Please try again later.');
        });

        return () => {
            script.removeEventListener('load', initializeGoogle);
        };
    }, [handleGoogleCredential, onError]);

    useEffect(() => {
        if (!autoPrompt || !isReady || !window.google?.accounts?.id) return;
        window.google.accounts.id.prompt();

        return () => {
            window.google?.accounts?.id?.cancel();
        };
    }, [autoPrompt, isReady]);

    return (
        <div className="auth-oauth-wrap" aria-busy={isInitializing}>
            <div className={`auth-oauth-button-shell ${disabled ? 'pointer-events-none opacity-60' : ''}`.trim()}>
                <div ref={buttonRef} className="auth-oauth-button-target"/>
            </div>
            {isInitializing && <p className="auth-oauth-caption">Loading Google sign-in...</p>}
            {isRedirecting && <p className="auth-oauth-caption font-medium">Redirecting to Google...</p>}
        </div>
    );
};

export default GoogleLoginButton;
