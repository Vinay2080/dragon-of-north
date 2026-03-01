import {useCallback, useEffect, useRef, useState} from 'react';
import {API_CONFIG} from '../../config';
import {apiService} from '../../services/apiService';
import {getDeviceId} from '../../utils/device';

const GOOGLE_IDENTITY_SCRIPT_SRC = 'https://accounts.google.com/gsi/client';

const GoogleLoginButton = ({onSuccess, onError, disabled = false, autoPrompt = false, mode = 'login', expectedIdentifier = ''}) => {
    const buttonRef = useRef(null);
    const hasClientId = Boolean(API_CONFIG.GOOGLE_CLIENT_ID);
    const [isInitializing, setIsInitializing] = useState(hasClientId);
    const [isReady, setIsReady] = useState(false);

    const handleGoogleCredential = useCallback(async ({credential}) => {
        if (!credential) {
            onError?.('Google did not return an ID token.');
            return;
        }

        const endpoint = mode === 'signup' ? API_CONFIG.ENDPOINTS.OAUTH_GOOGLE_SIGNUP : API_CONFIG.ENDPOINTS.OAUTH_GOOGLE;
        const payload = {
            id_token: credential,
            device_id: getDeviceId(),
        };

        if (expectedIdentifier) {
            payload.expected_identifier = expectedIdentifier.trim().toLowerCase();
        }

        const result = await apiService.post(endpoint, payload);

        if (apiService.isErrorResponse(result) || result?.api_response_status !== 'success') {
            const fallbackMessage = mode === 'signup'
                ? 'Google signup failed. Please try again.'
                : 'Google sign-in failed. Please try again.';
            onError?.(result?.message || fallbackMessage);
            return;
        }

        onSuccess?.(result?.data);
    }, [expectedIdentifier, mode, onError, onSuccess]);

    useEffect(() => {
        const clientId = API_CONFIG.GOOGLE_CLIENT_ID;
        if (!clientId) {
            onError?.('Google login is not configured. Missing client ID.');
            return;
        }

        const initializeGoogle = () => {
            if (!window.google?.accounts?.id || !buttonRef.current) {
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
            setIsInitializing(false);
            setIsReady(true);
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
            setIsInitializing(false);
            setIsReady(false);
            onError?.('Unable to load Google Identity Services. Please try again later.');
        });

        return () => {
            script.removeEventListener('load', initializeGoogle);
        };
    }, [handleGoogleCredential, onError]);

    useEffect(() => {
        if (!autoPrompt || !isReady || !window.google?.accounts?.id) return;
        window.google.accounts.id.prompt();
    }, [autoPrompt, isReady]);

    return (
        <div className="flex flex-col items-center gap-2" aria-busy={isInitializing}>
            <div ref={buttonRef} className={disabled ? 'pointer-events-none opacity-60' : ''}/>
            {isInitializing && <p className="text-xs text-slate-400">Loading Google sign-in...</p>}
        </div>
    );
};

export default GoogleLoginButton;
