import {useEffect, useMemo, useRef, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import {useAuth} from '../context/authUtils';
import AuthCardLayout from '../components/auth/AuthCardLayout';
import {resolvePostLoginRedirectPath} from '../utils/postLoginRedirect';

const STATUS_MESSAGES = [
    'Authenticating with Google...',
    'Verifying your identity...',
    'Creating secure session...',
    'Redirecting to your destination...',
];

const IDENTIFIER_HINT_KEY = 'auth_identifier_hint';

const OAuthCallbackPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const {login} = useAuth();
    const [stepIndex, setStepIndex] = useState(0);
    const handledRef = useRef(false);

    const activeMessage = useMemo(
        () => STATUS_MESSAGES[Math.min(stepIndex, STATUS_MESSAGES.length - 1)],
        [stepIndex]
    );

    useEffect(() => {
        if (handledRef.current) {
            return undefined;
        }

        handledRef.current = true;

        const progressInterval = window.setInterval(() => {
            setStepIndex((prev) => Math.min(prev + 1, STATUS_MESSAGES.length - 1));
        }, 850);

        const redirectToLogin = () => {
            localStorage.removeItem('isAuthenticated');
            localStorage.removeItem('user');
            navigate('/', {replace: true});
        };

        const getHydratedUser = () => {
            const storedUserRaw = localStorage.getItem('user');
            if (storedUserRaw) {
                try {
                    return JSON.parse(storedUserRaw);
                } catch {
                    // Ignore malformed storage and continue with identifier hint.
                }
            }

            const identifierHint = localStorage.getItem(IDENTIFIER_HINT_KEY);
            if (identifierHint) {
                return {identifier: identifierHint};
            }

            return null;
        };

        const verifySession = async () => {
            const verificationRequest = apiService.get(API_CONFIG.ENDPOINTS.SESSIONS_ALL);
            const timeoutPromise = new Promise((_, reject) => {
                window.setTimeout(() => reject(new Error('verification-timeout')), 5000);
            });

            try {
                const result = await Promise.race([verificationRequest, timeoutPromise]);
                if (!apiService.isErrorResponse(result) && Array.isArray(result?.data)) {
                    login(getHydratedUser());
                    const redirectPath = resolvePostLoginRedirectPath({
                        location,
                        defaultPath: '/',
                    });

                    navigate(redirectPath, {replace: true});
                    return;
                }
            } catch (error) {
                console.warn('OAuth callback session verification failed:', error);
            }

            redirectToLogin();
        };

        verifySession();

        return () => {
            window.clearInterval(progressInterval);
        };
    }, [location, login, navigate]);

    return (
        <AuthCardLayout
            title="Completing sign-in"
            subtitle={activeMessage}
        >
            <div className="auth-section flex items-center gap-3">
                <div className="db-spin auth-callback-spinner h-5 w-5 rounded-full border-2"/>
                <p className="auth-callback-note text-sm">Please wait while we securely finish your login.</p>
            </div>
        </AuthCardLayout>
    );
};

export default OAuthCallbackPage;
