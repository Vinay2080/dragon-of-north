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
            email_verified: data.email_verified,
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
            setPasswordError(result.backendMessage || result.message || 'Login failed.');
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
        toast.error(message || 'Google login failed.');
    };

    const isPasswordStep = step === AUTH_STEP.PASSWORD_LOGIN || step === AUTH_STEP.LOCAL_AND_GOOGLE;
    const showGoogle = step === AUTH_STEP.GOOGLE_ONLY || step === AUTH_STEP.LOCAL_AND_GOOGLE;

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl text-white">
                <h1 className="text-2xl font-bold">Auth</h1>
                <p className="mt-2 text-sm text-slate-400">Use your email to continue.</p>

                <form onSubmit={checkEmail} className="mt-6 space-y-3">
                    <label className="text-sm text-slate-300">Email</label>
                    <input
                        className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3"
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
                            className="w-full rounded-lg bg-blue-600 py-3 font-semibold disabled:opacity-60"
                            type="submit"
                            disabled={loading}
                        >
                            {loading ? 'Checking...' : 'Continue'}
                        </button>
                    )}
                </form>

                {step === AUTH_STEP.SIGNUP_CREATE_PASSWORD && (
                    <div className="mt-6 space-y-3">
                        <p className="text-sm text-slate-300">No account found. Continue with email signup.</p>
                        <button
                            className="w-full rounded-lg bg-blue-600 py-3 font-semibold"
                            onClick={() => navigate('/signup', {state: {identifier: normalizedEmail, identifierType: 'EMAIL'}})}
                        >
                            Continue to create account
                        </button>
                    </div>
                )}

                {step === AUTH_STEP.GOOGLE_SIGNUP && (
                    <div className="mt-6 space-y-3">
                        <p className="text-sm text-slate-300">No account found. Choose how you want to sign up.</p>
                        <button
                            className="w-full rounded-lg bg-blue-600 py-3 font-semibold"
                            onClick={() => navigate('/signup', {state: {identifier: normalizedEmail, identifierType: 'EMAIL'}})}
                        >
                            Create password account
                        </button>
                        <GoogleLoginButton
                            mode="signup"
                            onSuccess={handleGoogleSuccess}
                            onError={handleGoogleError}
                            disabled={loading}
                        />
                    </div>
                )}

                {isPasswordStep && (
                    <form onSubmit={handleLocalLogin} className="mt-6 space-y-3">
                        <label className="text-sm text-slate-300 block">Password</label>
                        <input
                            className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Enter your password"
                            required
                        />
                        <ValidationError errors={passwordError ? [passwordError] : []}/>
                        <button className="w-full rounded-lg bg-blue-600 py-3 font-semibold disabled:opacity-60" disabled={loading || !password}>
                            {loading ? 'Logging in...' : 'Login with password'}
                        </button>
                    </form>
                )}

                {showGoogle && (
                    <div className="mt-6 space-y-3">
                        {step === AUTH_STEP.GOOGLE_ONLY && <p className="text-sm text-slate-300">This account uses Google sign-in.</p>}
                        <GoogleLoginButton
                            onSuccess={handleGoogleSuccess}
                            onError={handleGoogleError}
                            disabled={loading}
                            autoPrompt={step === AUTH_STEP.GOOGLE_ONLY}
                        />
                    </div>
                )}
            </div>
        </div>
    );
};

export default AuthPage;
