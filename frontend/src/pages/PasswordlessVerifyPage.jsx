import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {AlertCircle, Loader} from 'react-feather';
import AuthCardLayout from '../components/auth/AuthCardLayout';
import MfaChallengeModal from '../components/auth/MfaChallengeModal';
import {useAuth} from '../context/authUtils';
import {useDocumentTitle} from '../hooks/useDocumentTitle';
import {verifyPasswordlessLogin} from '../services/passwordlessAuthService';
import {apiService} from '../services/apiService';
import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device';
import {resolvePostLoginRedirectPath} from '../utils/postLoginRedirect';

const VERIFY_STATE = {
    LOADING: 'LOADING',
    MFA_REQUIRED: 'MFA_REQUIRED',
    FAILED: 'FAILED',
};

const PasswordlessVerifyPage = () => {
    useDocumentTitle('Verifying login');

    const location = useLocation();
    const navigate = useNavigate();
    const {login} = useAuth();
    const handledRef = useRef(false);

    const token = useMemo(
        () => new URLSearchParams(location.search).get('token')?.trim(),
        [location.search]
    );

    const [state, setState] = useState(() => (
        token ? VERIFY_STATE.LOADING : VERIFY_STATE.FAILED
    ));
    const [mfaChallenge, setMfaChallenge] = useState(null);
    const [mfaError, setMfaError] = useState('');
    const [mfaSubmitting, setMfaSubmitting] = useState(false);

    const completeLogin = useCallback(() => {
        login();
        const redirectPath = resolvePostLoginRedirectPath({
            location,
            defaultPath: '/',
        });
        navigate(redirectPath, {replace: true});
    }, [location, login, navigate]);

    useEffect(() => {
        if (handledRef.current) return;
        handledRef.current = true;

        if (!token) return;

        window.history.replaceState(null, '', '/passwordless/verify');

        const completeVerification = async () => {
            const result = await verifyPasswordlessLogin(token);

            if (apiService.isErrorResponse(result) || result?.api_response_status !== 'success') {
                console.warn('Passwordless login verification failed:', result);
                setState(VERIFY_STATE.FAILED);
                return;
            }

            const data = result?.data;
            if (data?.mfa_required === true && data?.challenge_id) {
                setMfaChallenge({
                    challenge_id: data.challenge_id,
                    available_methods: Array.isArray(data.available_methods) ? data.available_methods : [],
                });
                setState(VERIFY_STATE.MFA_REQUIRED);
                return;
            }

            completeLogin();
        };

        void completeVerification();
    }, [completeLogin, token]);

    const handleMfaSubmit = useCallback(async ({providerType, code}) => {
        if (!mfaChallenge?.challenge_id || mfaSubmitting) return;

        setMfaSubmitting(true);
        setMfaError('');

        try {
            const result = await apiService.post(
                API_CONFIG.ENDPOINTS.MFA_VERIFY,
                {
                    challenge_id: mfaChallenge.challenge_id,
                    provider_type: providerType,
                    code,
                    device_id: getDeviceId(),
                },
                {skipAuthRefresh: true}
            );

            if (apiService.isErrorResponse(result) || result?.api_response_status !== 'success') {
                setMfaError(result?.backendMessage || result?.message || 'Unable to verify this code. Please try again.');
                return;
            }

            setMfaChallenge(null);
            setMfaError('');
            completeLogin();
        } finally {
            setMfaSubmitting(false);
        }
    }, [completeLogin, mfaChallenge, mfaSubmitting]);

    const handleMfaCancel = useCallback(() => {
        if (mfaSubmitting) return;
        setMfaChallenge(null);
        setMfaError('');
        setState(VERIFY_STATE.FAILED);
    }, [mfaSubmitting]);

    if (state === VERIFY_STATE.FAILED) {
        return (
            <AuthCardLayout
                title="Sign-in link expired or invalid"
                subtitle="Request a new sign-in link to continue."
            >
                <div className="auth-section flex flex-col items-center gap-4 text-center">
                    <div className="rounded-full bg-red-500/10 p-3">
                        <AlertCircle size={24} className="text-red-400"/>
                    </div>
                    <button
                        type="button"
                        className="auth-primary-btn"
                        onClick={() => navigate('/login', {replace: true})}
                    >
                        Request new sign-in link
                    </button>
                </div>
            </AuthCardLayout>
        );
    }

    return (
        <>
            <MfaChallengeModal
                key={mfaChallenge?.challenge_id || 'passwordless-mfa'}
                open={Boolean(mfaChallenge)}
                mode="login"
                availableMethods={mfaChallenge?.available_methods || []}
                error={mfaError}
                isSubmitting={mfaSubmitting}
                onCancel={handleMfaCancel}
                onSubmit={handleMfaSubmit}
            />
            <AuthCardLayout
                title="Completing sign-in"
                subtitle={state === VERIFY_STATE.MFA_REQUIRED ? 'Verify your identity to finish signing in.' : 'Verifying your sign-in link...'}
            >
                <div className="auth-section flex items-center gap-3">
                    <Loader size={20} className="animate-spin text-violet-400"/>
                    <p className="auth-callback-note text-sm">Please wait while we securely finish your login.</p>
                </div>
            </AuthCardLayout>
        </>
    );
};

export default PasswordlessVerifyPage;
