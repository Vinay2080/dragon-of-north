import React, {useEffect, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import {useAuth} from '../context/authUtils';
import RateLimitInfo from '../components/RateLimitInfo';
import {getDeviceId} from '../utils/device.js';
import {useToast} from '../hooks/useToast';
import ValidationError from '../components/Validation/ValidationError';
import AuthFlowProgress from '../components/AuthFlowProgress';
import GoogleLoginButton from '../components/auth/GoogleLoginButton';
import {validateIdentifier} from '../utils/validation';

const LoginPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const {login} = useAuth();
    const {toast} = useToast();
    const {identifier: initialIdentifier, password: initialPassword} = location.state || {};

    const [identifier, setIdentifier] = useState(initialIdentifier || '');
    const [password, setPassword] = useState(initialPassword || '');
    const [rememberMe, setRememberMe] = useState(localStorage.getItem('rememberMe') === 'true');
    const [showPassword, setShowPassword] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});
    const [loading, setLoading] = useState(false);
    const [rateLimitInfo, setRateLimitInfo] = useState(() => apiService.getRateLimitInfo());

    useEffect(() => {
        // Subscribe to rate limit updates
        const unsubscribe = apiService.onRateLimitUpdate((info) => {
            setRateLimitInfo(info);
        });

        // Countdown timer for rate limit
        const interval = setInterval(() => {
            setRateLimitInfo(prev => {
                if (prev.retryAfter > 0) {
                    return {...prev, retryAfter: prev.retryAfter - 1};
                }
                return prev;
            });
        }, 1000);

        return () => {
            unsubscribe();
            clearInterval(interval);
        };
    }, []);

    const isRateLimited = rateLimitInfo.retryAfter > 0;

    const handleIdentifierChange = (value) => {
        setIdentifier(value);
        const error = validateIdentifier(value);
        setFieldErrors(prev => ({...prev, identifier: error ? [error] : []}));
    };

    const handlePasswordChange = (value) => {
        setPassword(value);
        setFieldErrors(prev => ({...prev, password: value ? [] : ['Please enter your password.']}));
    };

    const handleGoogleSuccess = () => {
        if (rememberMe) {
            localStorage.setItem('rememberMe', 'true');
        } else {
            localStorage.removeItem('rememberMe');
        }
        login({identifier: 'google-user'});
        toast.success('Logged in with Google successfully.');
        navigate('/dashboard');
    };

    const handleGoogleError = (message) => {
        toast.error(message || 'Google sign-in failed.');
    };

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoading(true);
        setFieldErrors({});

        const identifierError = validateIdentifier(identifier);
        const passwordErrors = password ? [] : ['Please enter your password.'];
        if (identifierError || passwordErrors.length) {
            setFieldErrors({
                identifier: identifierError ? [identifierError] : [],
                password: passwordErrors,
            });
            setLoading(false);
            return;
        }

        const result = await apiService.post(API_CONFIG.ENDPOINTS.LOGIN, {
            identifier,
            password,
            device_id: getDeviceId(),
        });

        if (apiService.isErrorResponse(result)) {
            const errors = result?.fieldErrors?.reduce((acc, item) => {
                acc[item.field] = [...(acc[item.field] || []), item.message];
                return acc;
            }, {}) || {};
            setFieldErrors(errors);
            toast.error(result.message || 'Login failed. Please check your credentials.');
            setLoading(false);
            return;
        }

        if (result?.api_response_status === 'success') {
            if (rememberMe) {
                localStorage.setItem('rememberMe', 'true');
            } else {
                localStorage.removeItem('rememberMe');
            }
            login({identifier});
            toast.success('Logged in successfully.');
            navigate('/dashboard');
        } else {
            toast.error(result?.message || 'Login failed. Please check your credentials.');
        }

        setLoading(false);
    };

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">Login</h2>
                <p className="mt-1 mb-6 text-sm text-slate-400">Enter your credentials to access your account</p>
                <AuthFlowProgress currentStep="login"/>

                <RateLimitInfo/>

                <form onSubmit={handleLogin} noValidate>
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-1">Email or Phone</label>
                            <input
                                type="text"
                                value={identifier}
                                onChange={(e) => handleIdentifierChange(e.target.value)}
                                placeholder="Email or phone number"
                                className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                                aria-invalid={!!fieldErrors.identifier?.length}
                                aria-describedby="identifier-errors"
                                required
                            />
                            <ValidationError id="identifier-errors" errors={fieldErrors.identifier || []}/>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-1">Password</label>
                            <div className="relative">
                                <input
                                    type={showPassword ? 'text' : 'password'}
                                    value={password}
                                    onChange={(e) => handlePasswordChange(e.target.value)}
                                    placeholder="Enter password"
                                    className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 pr-12 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                                    aria-invalid={!!fieldErrors.password?.length}
                                    aria-describedby="password-errors"
                                    required
                                />
                                <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 transform -translate-y-1/2 text-slate-400 hover:text-white transition">
                                    {showPassword ? '🙈' : '👁️'}
                                </button>
                            </div>
                            <ValidationError id="password-errors" errors={fieldErrors.password || []}/>
                        </div>

                        <label className="flex items-center gap-2 text-sm text-slate-300">
                            <input type="checkbox" checked={rememberMe} onChange={(e) => setRememberMe(e.target.checked)}/>
                            Remember me on this device
                        </label>

                        <button type="submit" disabled={loading || !identifier || !password || isRateLimited || !!fieldErrors.identifier?.length}
                                className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50">
                            {isRateLimited ? `Rate limited. Try again in ${rateLimitInfo.retryAfter}s...` : loading ? 'Logging in...' : 'Login'}
                        </button>
                    </div>
                </form>

                <div className="my-6 flex items-center gap-3">
                    <div className="h-px flex-1 bg-slate-800"/>
                    <span className="text-xs uppercase tracking-wide text-slate-500">or</span>
                    <div className="h-px flex-1 bg-slate-800"/>
                </div>

                <GoogleLoginButton
                    disabled={loading || isRateLimited}
                    onSuccess={handleGoogleSuccess}
                    onError={handleGoogleError}
                />

                <div className="mt-4 text-center">
                    <button type="button" onClick={() => navigate('/forgot-password')} className="text-sm text-blue-400 hover:text-blue-300 transition">Forgot password?</button>
                </div>
                <div className="mt-4 text-center">
                    <button onClick={() => navigate('/')} className="text-xs text-slate-500 hover:text-white transition">Back to Welcome</button>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;
