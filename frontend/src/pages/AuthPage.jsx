import React, {useEffect, useMemo, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import {useToast} from '../hooks/useToast';
import {useAuthState} from '../hooks/authStateHook';
import {AuthErrorMessage, AuthLoadingOverlay} from '../components/auth/AuthStateComponents';
import ValidationError from '../components/Validation/ValidationError';
import GoogleLoginButton from '../components/auth/GoogleLoginButton';
import AuthCardLayout from '../components/auth/AuthCardLayout';
import AuthInput from '../components/auth/AuthInput';
import PasswordInput from '../components/auth/PasswordInput';
import AuthButton from '../components/auth/AuthButton';
import AuthDivider from '../components/auth/AuthDivider';
import {getDeviceId} from '../utils/device';
import {useAuth} from '../context/authUtils';
import {useDocumentTitle} from '../hooks/useDocumentTitle';

const AUTH_STEP = {
    EMAIL_ENTRY: 'EMAIL_ENTRY',
    PASSWORD_LOGIN: 'PASSWORD_LOGIN',
    GOOGLE_ONLY: 'GOOGLE_ONLY',
    LOCAL_AND_GOOGLE: 'LOCAL_AND_GOOGLE',
    SIGNUP_CREATE_PASSWORD: 'SIGNUP_CREATE_PASSWORD',
    GOOGLE_SIGNUP: 'GOOGLE_SIGNUP',
};

const OTP_FLOW = {
    SIGNUP: 'SIGNUP',
    LOGIN_UNVERIFIED: 'LOGIN_UNVERIFIED',
};

const AuthPage = () => {
    useDocumentTitle('Login');
    const navigate = useNavigate();
    const {toast} = useToast();
    const {login, isAuthenticated, isLoading} = useAuth();
    const authState = useAuthState();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [step, setStep] = useState(AUTH_STEP.EMAIL_ENTRY);
    const [passwordError, setPasswordError] = useState('');
    const [isGoogleRedirecting, setIsGoogleRedirecting] = useState(false);
    const [showAuthError, setShowAuthError] = useState(false);

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
        authState.setLoading('Checking email...');
        setPasswordError('');

        // Simulate backend latency (300-500ms)
        await new Promise((resolve) => setTimeout(resolve, 400));

        const result = await apiService.post(
            API_CONFIG.ENDPOINTS.IDENTIFIER_STATUS,
            {
                identifier: normalizedEmail,
                identifier_type: 'EMAIL',
            }
        );

        setLoading(false);
        authState.setIdle();

        if (
            apiService.isErrorResponse(result) ||
            result?.api_response_status !== 'success'
        ) {
            const errorMsg = result?.message || 'Unable to check this email.';
            toast.error(errorMsg);
            authState.setError(errorMsg);
            setShowAuthError(true);
            return;
        }

        const data = result.data || {};

        // Smooth transition to password step
        setTimeout(() => {
            moveToStepFromProviders({
                exists: Boolean(data.exists),
                providers: Array.isArray(data.providers)
                    ? data.providers
                    : [],
            });
        }, 150);
    };

    const handleUnverifiedEmailLogin = async () => {
        const unverifiedMessage = 'Email not verified. Redirecting to OTP verification...';
        authState.setError(unverifiedMessage);
        setShowAuthError(true);
        toast.error(unverifiedMessage);

        const otpResult = await apiService.post(
            API_CONFIG.ENDPOINTS.EMAIL_OTP_REQUEST,
            {
                email: normalizedEmail,
                otp_purpose: 'SIGNUP',
            }
        );

        if (apiService.isErrorResponse(otpResult)) {
            const otpErrorMessage = otpResult.message || 'Failed to send OTP. Please try again.';
            authState.setError(otpErrorMessage);
            setShowAuthError(true);
            toast.error(otpErrorMessage);
            return;
        }

        toast.success('OTP sent successfully. Please verify your email.');
        navigate('/otp', {
            state: {
                identifier: normalizedEmail,
                identifierType: 'EMAIL',
                flow: OTP_FLOW.LOGIN_UNVERIFIED,
            },
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
        authState.setLoading('Signing in...');

        // Simulate backend latency (300-800ms)
        await new Promise((resolve) => setTimeout(resolve, 500));

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

            if (result.errorCode === 'VAL_002') {
                await handleUnverifiedEmailLogin();
                return;
            }

            if (backendMessage.toLowerCase().includes('google')) {
                resetFlow();
                authState.setError('Please login with Google for this account.');
                setShowAuthError(true);
                return;
            }

            authState.setError(backendMessage);
            setShowAuthError(true);
            setPasswordError(backendMessage);
            return;
        }

        authState.setSuccess('Welcome back!');
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
        <>
            <AuthLoadingOverlay isVisible={authState.isLoading} message={authState.message}/>

            <AuthCardLayout
                title="Welcome back"
                subtitle="Use your email to continue."
            >
                {showAuthError && (
                    <div className="mb-4">
                        <AuthErrorMessage
                            message={authState.message}
                            onRetry={() => {
                                authState.setIdle();
                                setShowAuthError(false);
                            }}
                            onDismiss={() => {
                                authState.setIdle();
                                setShowAuthError(false);
                            }}
                        />
                    </div>
                )}

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
                        disabled={loading || authState.isLoading}
                    />
                    {step === AUTH_STEP.EMAIL_ENTRY && (
                        <AuthButton
                            type="submit"
                            disabled={loading || isGoogleRedirecting || authState.isLoading}
                            loading={loading}
                        >
                            Continue with Email
                        </AuthButton>
                    )}
                </form>

                {isPasswordStep && (
                    <form onSubmit={handleLocalLogin} className="auth-section">
                        <label className="auth-label block">Password</label>
                        <PasswordInput
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Enter your password"
                            hasError={Boolean(passwordError)}
                            required
                            disabled={loading || isGoogleRedirecting || authState.isLoading}
                        />
                        <ValidationError errors={passwordError ? [passwordError] : []}/>
                        <AuthButton
                            disabled={loading || !password || isGoogleRedirecting}
                            loading={loading}
                        >
                            Login with password
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
        </>
    );
};

export default AuthPage;
