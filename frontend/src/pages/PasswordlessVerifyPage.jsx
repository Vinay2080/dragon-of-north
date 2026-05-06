import {useEffect, useRef, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {AlertCircle, Loader} from 'react-feather';
import AuthCardLayout from '../components/auth/AuthCardLayout';
import {useAuth} from '../context/authUtils';
import {useDocumentTitle} from '../hooks/useDocumentTitle';
import {verifyPasswordlessLogin} from '../services/passwordlessAuthService';
import {apiService} from '../services/apiService';

const VERIFY_STATE = {
    LOADING: 'LOADING',
    FAILED: 'FAILED',
};

const PasswordlessVerifyPage = () => {
    useDocumentTitle('Verifying login');

    const location = useLocation();
    const navigate = useNavigate();
    const {login} = useAuth();
    const handledRef = useRef(false);
    const [state, setState] = useState(VERIFY_STATE.LOADING);

    useEffect(() => {
        if (handledRef.current) return;
        handledRef.current = true;

        const token = new URLSearchParams(location.search).get('token')?.trim();

        if (!token) {
            setState(VERIFY_STATE.FAILED);
            return;
        }

        window.history.replaceState(null, '', '/passwordless/verify');

        const completeVerification = async () => {
            const result = await verifyPasswordlessLogin(token);

            if (apiService.isErrorResponse(result) || result?.api_response_status !== 'success') {
                console.warn('Passwordless login verification failed:', result);
                setState(VERIFY_STATE.FAILED);
                return;
            }

            login();
            navigate('/home', {replace: true});
        };

        void completeVerification();
    }, [location.search, login, navigate]);

    if (state === VERIFY_STATE.FAILED) {
        return (
            <AuthCardLayout
                title="Magic link expired or invalid"
                subtitle="Request a new login link to continue."
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
                        Request new login link
                    </button>
                </div>
            </AuthCardLayout>
        );
    }

    return (
        <AuthCardLayout
            title="Completing sign-in"
            subtitle="Verifying your magic login link..."
        >
            <div className="auth-section flex items-center gap-3">
                <Loader size={20} className="animate-spin text-violet-400"/>
                <p className="auth-callback-note text-sm">Please wait while we securely finish your login.</p>
            </div>
        </AuthCardLayout>
    );
};

export default PasswordlessVerifyPage;
