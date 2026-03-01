import React, {useEffect, useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import {useToast} from '../hooks/useToast';
import ValidationError from '../components/Validation/ValidationError';
import GoogleLoginButton from '../components/auth/GoogleLoginButton';
import {getDeviceId} from '../utils/device';
import {useAuth} from '../context/authUtils';

const AUTH_STEP = {
    EMAIL_ENTRY: 'EMAIL_ENTRY',
    PASSWORD_LOGIN: 'PASSWORD_LOGIN',
    GOOGLE_ONLY: 'GOOGLE_ONLY',
    LOCAL_AND_GOOGLE: 'LOCAL_AND_GOOGLE',
    SIGNUP_CREATE_PASSWORD: 'SIGNUP_CREATE_PASSWORD',
    GOOGLE_SIGNUP: 'GOOGLE_SIGNUP',
};

const isGoogleEmail = (value) => {
    const emailValue = value.trim().toLowerCase();
    return emailValue.endsWith('@gmail.com') || emailValue.endsWith('@googlemail.com');
};

const AuthPage = () => {
    const navigate = useNavigate();
    const {toast} = useToast();
    const {login, isAuthenticated, isLoading} = useAuth();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [step, setStep] = useState(AUTH_STEP.EMAIL_ENTRY);
    const [passwordError, setPasswordError] = useState('');

    useEffect(() => {
        if (!isLoading && isAuthenticated) {
            navigate('/dashboard', {replace: true});
        }
    }, [isAuthenticated, isLoading, navigate]);

    const normalizedEmail = useMemo(() => email.trim().toLowerCase(), [email]);

    const resetFlow = () => {
        setStep(AUTH_STEP.EMAIL_ENTRY);
        setPassword('');
        setPasswordError('');
    };

    const moveToStepFromProviders = ({exists, providers = []}) => {
        const hasLocal = providers.includes('LOCAL');
        const hasGoogle = providers.includes('GOOGLE');

        if (!exists) {
            setStep(isGoogleEmail(normalizedEmail) ? AUTH_STEP.GOOGLE_SIGNUP : AUTH_STEP.SIGNUP_CREATE_PASSWORD);
            return;
        }

        if (hasLocal && hasGoogle) {
            setStep(AUTH_STEP.LOCAL_AND_GOOGLE);
            return;
        }

        if (hasGoogle) {
            setStep(AUTH_STEP.GOOGLE_ONLY);
            return;
        }

        setStep(AUTH_STEP.PASSWORD_LOGIN);
    };

    const checkEmail = async (event) => {
        event.preventDefault();
        if (!normalizedEmail) {
            toast.error('Please enter your email.');
            return;
        }

        setLoading(true);
        setPasswordError('');

        const result = await apiService.post(API_CONFIG.ENDPOINTS.IDENTIFIER_STATUS, {
            identifier: normalizedEmail,
            identifier_type: 'EMAIL',
        });

        setLoading(false);

        if (apiService.isErrorResponse(result) || result?.api_response_status !== 'success') {
            toast.error(result?.message || 'Unable to check this email.');
            return;
        }

        const data = result.data || {};
        moveToStepFromProviders({
            exists: Boolean(data.exists),
            providers: Array.isArray(data.providers) ? data.providers : [],
        });
    };

    const handleLocalLogin = async (event) => {
        event.preventDefault();
        if (!password) return;

        setPasswordError('');
        setLoading(true);

        const result = await apiService.post(API_CONFIG.ENDPOINTS.LOGIN, {
            identifier: normalizedEmail,
            password,
            device_id: getDeviceId(),
        });

        setLoading(false);

        if (apiService.isErrorResponse(result)) {
            const backendMessage = result.backendMessage || result.message || 'Login failed.';
            if (backendMessage.toLowerCase().includes('google')) {
                resetFlow();
                toast.error('Please login with Google for this account.');
                return;
            }
            setPasswordError(backendMessage);
            return;
        }

        login({identifier: normalizedEmail});
        navigate('/dashboard');
    };

    const handleGoogleSuccess = () => {
        login({identifier: normalizedEmail});
        navigate('/dashboard');
    };

    const handleGoogleError = (message) => {
        const resolvedMessage = message || 'Google authentication failed.';
        const normalizedMessage = resolvedMessage.toLowerCase();

        if (normalizedMessage.includes('does not match entered email')) {
            resetFlow();
            toast.error('Please login with Google using the same email you entered.');
            return;
        }

        if (normalizedMessage.includes('registered. please sign up first')) {
            resetFlow();
            toast.error('Please login with Google using the same email you entered.');
            return;
        }

        toast.error(resolvedMessage);
    };

    const isPasswordStep = step === AUTH_STEP.PASSWORD_LOGIN || step === AUTH_STEP.LOCAL_AND_GOOGLE;
    const showGoogle = step === AUTH_STEP.EMAIL_ENTRY
        || step === AUTH_STEP.GOOGLE_ONLY
        || step === AUTH_STEP.LOCAL_AND_GOOGLE
        || step === AUTH_STEP.PASSWORD_LOGIN;

    return (
        <div className="auth-shell">
            <div className="auth-card">
                <h1 className="auth-title">Auth</h1>
                <p className="auth-subtitle">Use your email to continue.</p>

                <form onSubmit={checkEmail} className="auth-section">
                    <label className="auth-helper">Email</label>
                    <input
                        className="auth-input"
                        type="email"
                        value={email}
                        onChange={(e) => {
                            setEmail(e.target.value);
                            if (step !== AUTH_STEP.EMAIL_ENTRY) {
                                resetFlow();
                            }
                        }}
                        placeholder="you@example.com"
                    />
                    {step === AUTH_STEP.EMAIL_ENTRY && (
                        <button
                            className="btn-primary"
                            type="submit"
                            disabled={loading}
                        >
                            {loading ? 'Checking...' : 'Continue with Email'}
                        </button>
                    )}
                </form>

                {step === AUTH_STEP.SIGNUP_CREATE_PASSWORD && (
                    <div className="auth-section">
                        <p className="auth-helper">No account found. Continue with email signup.</p>
                        <button
                            className="btn-primary"
                            onClick={() => navigate('/signup', {state: {identifier: normalizedEmail, identifierType: 'EMAIL'}})}
                        >
                            Continue to create account
                        </button>
                    </div>
                )}

                {step === AUTH_STEP.GOOGLE_SIGNUP && (
                    <div className="auth-section">
                        <p className="auth-helper">No account found. Choose how you want to sign up.</p>
                        <button
                            className="btn-primary"
                            onClick={() => navigate('/signup', {state: {identifier: normalizedEmail, identifierType: 'EMAIL'}})}
                        >
                            Create password account
                        </button>
                        <GoogleLoginButton
                            mode="signup"
                            onSuccess={handleGoogleSuccess}
                            onError={handleGoogleError}
                            disabled={loading}
                            expectedIdentifier={normalizedEmail}
                        />
                    </div>
                )}

                {isPasswordStep && (
                    <form onSubmit={handleLocalLogin} className="auth-section">
                        <label className="auth-helper block">Password</label>
                        <input
                            className="auth-input"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Enter your password"
                            required
                        />
                        <ValidationError errors={passwordError ? [passwordError] : []}/>
                        <button className="btn-primary" disabled={loading || !password}>
                            {loading ? 'Logging in...' : 'Login with password'}
                        </button>
                        <button
                            type="button"
                            className="btn-ghost"
                            onClick={() => navigate('/forgot-password')}
                        >
                            Forgot password?
                        </button>
                    </form>
                )}

                {showGoogle && (
                    <div className="auth-section">
                        {step === AUTH_STEP.EMAIL_ENTRY && <p className="auth-helper">Continue with Google</p>}
                        {step === AUTH_STEP.GOOGLE_ONLY && <p className="auth-helper">This account uses Google sign-in.</p>}
                        {step === AUTH_STEP.PASSWORD_LOGIN && <p className="auth-helper">Or continue with Google instead of resetting your password.</p>}
                        <GoogleLoginButton
                            onSuccess={handleGoogleSuccess}
                            onError={handleGoogleError}
                            disabled={loading}
                            autoPrompt={step === AUTH_STEP.GOOGLE_ONLY}
                            expectedIdentifier={normalizedEmail}
                        />
                    </div>
                )}
            </div>
        </div>
    );
};

export default AuthPage;
