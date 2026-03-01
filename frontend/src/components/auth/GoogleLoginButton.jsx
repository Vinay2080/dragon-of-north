import {useCallback, useEffect, useRef, useState} from 'react';
import {API_CONFIG} from '../../config';
import {apiService} from '../../services/apiService';
import {getDeviceId} from '../../utils/device';

const GOOGLE_IDENTITY_SCRIPT_SRC = 'https://accounts.google.com/gsi/client';

const GoogleLoginButton = ({onSuccess, onError, disabled = false, autoPrompt = false, mode = 'login', expectedIdentifier = ''}) => {
    const buttonRef = useRef(null);
    const initializedRef = useRef(false);
    const expectedIdentifierRef = useRef(expectedIdentifier);
    const hasClientId = Boolean(API_CONFIG.GOOGLE_CLIENT_ID);
    const [isInitializing, setIsInitializing] = useState(hasClientId);
    const [isReady, setIsReady] = useState(false);

    useEffect(() => {
        expectedIdentifierRef.current = expectedIdentifier;
    }, [expectedIdentifier]);

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

        const endpoint = mode === 'signup' ? API_CONFIG.ENDPOINTS.OAUTH_GOOGLE_SIGNUP : API_CONFIG.ENDPOINTS.OAUTH_GOOGLE;
        const payload = {
            // Send both payload keys for compatibility across backend naming strategies.
            id_token: credential,
            idToken: credential,
            device_id: getDeviceId(),
        };

        const normalizedExpectedIdentifier = expectedIdentifierRef.current?.trim().toLowerCase();
        if (normalizedExpectedIdentifier) {
            payload.expected_identifier = normalizedExpectedIdentifier;
            payload.expectedIdentifier = normalizedExpectedIdentifier;
        }

        const result = await apiService.post(endpoint, payload);

        if (apiService.isErrorResponse(result) || result?.api_response_status !== 'success') {
            const fallbackMessage = mode === 'signup'
                ? 'Google signup failed. Please try again.'
                : 'Google sign-in failed. Please try again.';
            onError?.(result?.message || fallbackMessage);
            return;
        }

        // Pass both the backend response and the email from token
        onSuccess?.({
            ...result?.data,
            email: emailFromToken,
            identifier: emailFromToken
        });
    }, [mode, onError, onSuccess]);

    useEffect(() => {
        const clientId = API_CONFIG.GOOGLE_CLIENT_ID;
        console.log("Google Client ID being used:", clientId);
        console.log("Current origin:", window.location.origin);
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
            window.google.accounts.id.renderButton(buttonRef.current, {
                type: 'standard',
                theme: 'outline',
                text: 'continue_with',
                shape: 'pill',
                size: 'large',
                width: 320,
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
        <div className="flex flex-col items-center gap-2" aria-busy={isInitializing}>
            <div ref={buttonRef} className={disabled ? 'pointer-events-none opacity-60' : ''}/>
            {isInitializing && <p className="text-xs text-slate-400">Loading Google sign-in...</p>}
        </div>
    );
};

export default GoogleLoginButton;
