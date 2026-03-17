import React, {useEffect, useMemo, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import {useToast} from '../hooks/useToast';
import ValidationError from '../components/Validation/ValidationError';
import GoogleLoginButton from '../components/auth/GoogleLoginButton';
import AuthCardLayout from '../components/auth/AuthCardLayout';
import AuthInput from '../components/auth/AuthInput';
import AuthButton from '../components/auth/AuthButton';
import AuthDivider from '../components/auth/AuthDivider';
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

const AuthPage = () => {
    const navigate = useNavigate();
    const {toast} = useToast();
    const {login, isAuthenticated, isLoading} = useAuth();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [step, setStep] = useState(AUTH_STEP.EMAIL_ENTRY);
    const [passwordError, setPasswordError] = useState('');
    const [isGoogleRedirecting, setIsGoogleRedirecting] = useState(false);

    const normalizedEmail = useMemo(
        () => email.trim().toLowerCase(),
        [email]
    );

    useEffect(() => {
        if (!isLoading && isAuthenticated) {
            navigate('/sessions', {replace: true});
        }
    }, [isAuthenticated, isLoading, navigate]);

    const resetFlow = () => {
        setStep(AUTH_STEP.EMAIL_ENTRY);
        setPassword('');
        setPasswordError('');
        setIsGoogleRedirecting(false);
    };

    const moveToStepFromProviders = ({exists, providers = []}) => {
        const hasLocal = providers.includes('LOCAL');
        const hasGoogle = providers.includes('GOOGLE');

        if (!exists) {
            setStep(AUTH_STEP.SIGNUP_CREATE_PASSWORD);
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

        const result = await apiService.post(
            API_CONFIG.ENDPOINTS.IDENTIFIER_STATUS,
            {
                identifier: normalizedEmail,
                identifier_type: 'EMAIL',
            }
        );

        setLoading(false);

        if (
            apiService.isErrorResponse(result) ||
            result?.api_response_status !== 'success'
        ) {
            toast.error(result?.message || 'Unable to check this email.');
            return;
        }

        const data = result.data || {};
        moveToStepFromProviders({
            exists: Boolean(data.exists),
            providers: Array.isArray(data.providers)
                ? data.providers
                : [],
        });
    };

    const handleLocalLogin = async (event) => {
        event.preventDefault();
        if (!password || isGoogleRedirecting) return;

        if (!normalizedEmail) {
            toast.error('Email is required');
            return;
        }

        setPasswordError('');
        setLoading(true);

        const result = await apiService.post(
            API_CONFIG.ENDPOINTS.LOGIN,
            {
                identifier: normalizedEmail,
                password,
                device_id: getDeviceId(),
            }
        );

        setLoading(false);

        if (apiService.isErrorResponse(result)) {
            const backendMessage =
                result.backendMessage ||
                result.message ||
                'Login failed.';

            if (backendMessage.toLowerCase().includes('google')) {
                resetFlow();
                toast.error(
                    'Please login with Google for this account.'
                );
                return;
            }

            setPasswordError(backendMessage);
            return;
        }

        login({identifier: normalizedEmail});
        navigate('/sessions');
    };

    //  CLEAN GOOGLE SUCCESS HANDLER
    const handleGoogleSuccess = (data) => {
        const backendIdentifier =
            data?.identifier || data?.email;

        const resolvedIdentifier =
            (backendIdentifier || normalizedEmail || '')
                .trim()
                .toLowerCase();

        if (!resolvedIdentifier) {
            toast.error(
                'Unable to determine authenticated user.'
            );
            return;
        }

        // Keep an identifier hint so the auth bootstrap can hydrate user info after browser reopened.
        localStorage.setItem('auth_identifier_hint', resolvedIdentifier);

        login({
            identifier: resolvedIdentifier,
        });

        navigate('/auth/callback');
    };

    const handleGoogleError = (message) => {
        const resolvedMessage =
            message || 'Google authentication failed.';
        const normalizedMessage =
            resolvedMessage.toLowerCase();

        if (
            normalizedMessage.includes(
                'does not match entered email'
            )
        ) {
            resetFlow();
            toast.error(
                'Please login with Google using the same email you entered.'
            );
            return;
        }

        if (
            normalizedMessage.includes(
                'registered. please sign up first'
            )
        ) {
            resetFlow();
            toast.error(
                'Please login with Google using the same email you entered.'
            );
            return;
        }

        setIsGoogleRedirecting(false);
        toast.error(resolvedMessage);
    };

    const handleGoogleStart = () => {
        setIsGoogleRedirecting(true);
    };

    const isPasswordStep =
        step === AUTH_STEP.PASSWORD_LOGIN ||
        step === AUTH_STEP.LOCAL_AND_GOOGLE;

    const isSignupStep =
        step === AUTH_STEP.SIGNUP_CREATE_PASSWORD ||
        step === AUTH_STEP.GOOGLE_SIGNUP;

    const showGoogle =
        step === AUTH_STEP.EMAIL_ENTRY ||
        step === AUTH_STEP.GOOGLE_ONLY ||
        step === AUTH_STEP.LOCAL_AND_GOOGLE ||
        step === AUTH_STEP.PASSWORD_LOGIN ||
        isSignupStep;

    return (
        <AuthCardLayout
            title="Welcome back"
            subtitle="Use your email to continue."
        >
            <form onSubmit={checkEmail} className="space-y-3">
                <label className="auth-label">Email</label>
                <AuthInput
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
                    <AuthButton
                        type="submit"
                        disabled={loading || isGoogleRedirecting}
                    >
                        {loading ? 'Checking...' : 'Continue with Email'}
                    </AuthButton>
                )}
            </form>

            {isPasswordStep && (
                <form onSubmit={handleLocalLogin} className="auth-section">
                    <label className="auth-label block">Password</label>
                    <AuthInput
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Enter your password"
                        hasError={Boolean(passwordError)}
                        required
                    />
                    <ValidationError errors={passwordError ? [passwordError] : []}/>
                    <AuthButton disabled={loading || !password || isGoogleRedirecting}>
                        {loading ? 'Logging in...' : 'Login with password'}
                    </AuthButton>
                    <p className="auth-helper text-right">
                        <Link to="/forgot-password" className="auth-link">Forgot password?</Link>
                    </p>
                </form>
            )}

            {showGoogle && (
                <>
                    <AuthDivider label="or continue with"/>
                    <div className="auth-section">
                        {isSignupStep && (
                            <p className="auth-helper">
                                No account found. Create one with password or continue with Google.
                            </p>
                        )}
                        {step === AUTH_STEP.SIGNUP_CREATE_PASSWORD && (
                            <AuthButton
                                type="button"
                                onClick={() => navigate('/signup', {state: {identifier: normalizedEmail, identifierType: 'EMAIL'}})}
                            >
                                Sign up with password
                            </AuthButton>
                        )}
                        <GoogleLoginButton
                            onSuccess={handleGoogleSuccess}
                            onError={handleGoogleError}
                            onStart={handleGoogleStart}
                            disabled={loading || isGoogleRedirecting}
                            isRedirecting={isGoogleRedirecting}
                            autoPrompt={step === AUTH_STEP.GOOGLE_ONLY}
                            mode={isSignupStep ? 'signup' : 'login'}
                            expectedIdentifier={normalizedEmail}
                        />
                    </div>
                </>
            )}
        </AuthCardLayout>
    );
};

export default AuthPage;
