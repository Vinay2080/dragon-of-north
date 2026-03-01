import React, {useEffect, useMemo, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import RateLimitInfo from '../components/RateLimitInfo';
import {useToast} from '../hooks/useToast';
import ValidationError from '../components/Validation/ValidationError';
import AuthFlowProgress from '../components/AuthFlowProgress';
import GoogleLoginButton from '../components/auth/GoogleLoginButton';
import {validatePassword} from '../utils/validation';
import {useAuth} from '../context/authUtils';

const SignupPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const {toast} = useToast();
    const {login, isAuthenticated, isLoading} = useAuth();
    const {identifier, identifierType} = location.state || {};

    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [acceptTerms, setAcceptTerms] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!isLoading && isAuthenticated) {
            navigate('/dashboard', {replace: true});
        }
    }, [isAuthenticated, isLoading, navigate]);

    const isEmailIdentifier = identifierType === 'EMAIL';

    const passwordStrengthHint = useMemo(() => {
        if (!password) return 'Use at least 8 characters with uppercase, lowercase, number and symbol.';
        const errors = validatePassword(password);
        if (errors.length === 0) return 'Strong password ✅';
        return `Needs: ${errors.join(' ')}`;
    }, [password]);

    const handlePasswordChange = (value) => {
        setPassword(value);
        const errors = validatePassword(value);
        const confirmPasswordErrors = confirmPassword && value !== confirmPassword ? ['Passwords do not match.'] : [];
        setFieldErrors(prev => ({...prev, password: value ? errors : [], confirmPassword: confirmPasswordErrors}));
    };

    const handleConfirmPasswordChange = (value) => {
        setConfirmPassword(value);
        setFieldErrors(prev => ({
            ...prev,
            confirmPassword: value && value !== password ? ['Passwords do not match.'] : [],
        }));
    };

    const handleGoogleSignup = () => {
        login({identifier});
        navigate('/dashboard');
    };

    const handleGetOtp = async (e) => {
        e.preventDefault();
        setFieldErrors({});

        const passwordErrors = validatePassword(password);
        if (passwordErrors.length) {
            setFieldErrors(prev => ({...prev, password: passwordErrors}));
            return;
        }

        if (password !== confirmPassword) {
            setFieldErrors(prev => ({...prev, confirmPassword: ['Passwords do not match.']}));
            return;
        }

        if (!acceptTerms) {
            setFieldErrors(prev => ({...prev, terms: ['Please accept the terms to continue.']}));
            return;
        }

        setLoading(true);

        const signupResult = await apiService.post(API_CONFIG.ENDPOINTS.SIGNUP, {
            identifier,
            identifier_type: identifierType,
            password,
        });

        if (apiService.isErrorResponse(signupResult)) {
            const errors = signupResult?.fieldErrors?.reduce((acc, item) => {
                acc[item.field] = [...(acc[item.field] || []), item.message];
                return acc;
            }, {}) || {};
            setFieldErrors(errors);
            toast.error(signupResult.message || 'Signup failed.');
            setLoading(false);
            return;
        }

        if (signupResult?.api_response_status !== 'success') {
            toast.error(signupResult?.message || 'Something went wrong');
            setLoading(false);
            return;
        }

        const otpEndpoint = identifierType === 'EMAIL' ? API_CONFIG.ENDPOINTS.EMAIL_OTP_REQUEST : API_CONFIG.ENDPOINTS.PHONE_OTP_REQUEST;
        const otpPayload = identifierType === 'EMAIL' ? {email: identifier, otp_purpose: 'SIGNUP'} : {phone: identifier, otp_purpose: 'SIGNUP'};
        const otpResult = await apiService.post(otpEndpoint, otpPayload);

        if (apiService.isErrorResponse(otpResult)) {
            toast.error(otpResult.message || 'Failed to send OTP. Please try again.');
            setLoading(false);
            return;
        }

        toast.success('OTP sent. Complete verification to finish signup.');
        navigate('/otp', {state: {identifier, identifierType}});
        setLoading(false);
    };

    if (!identifier) {
        navigate('/');
        return null;
    }

    return (
        <div className="auth-shell">
            <div className="auth-card">
                <h2 className="auth-title">Create Account</h2>
                <p className="auth-subtitle mb-6">Setting up account for <span className="text-blue-400 font-medium">{identifier}</span></p>
                <AuthFlowProgress currentStep="signup"/>

                {isEmailIdentifier && (
                    <div className="auth-section mb-6">
                        <GoogleLoginButton
                            mode="signup"
                            onSuccess={handleGoogleSignup}
                            onError={(message) => toast.error(message || 'Google signup failed.')}
                            disabled={loading}
                            expectedIdentifier={identifier}
                        />
                        <p className="auth-helper text-center">or create a password account below</p>
                    </div>
                )}

                <form onSubmit={handleGetOtp} noValidate>
                    <div className="space-y-4">
                        <div className="relative">
                            <input type={showPassword ? 'text' : 'password'} value={password} onChange={(e) => handlePasswordChange(e.target.value)} placeholder="Enter password" className="auth-input pr-12 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none" aria-describedby="password-hint password-errors" required/>
                            <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white transition duration-200">{showPassword ? '🙈' : '👁️'}</button>
                        </div>
                        <p id="password-hint" className="auth-helper">{passwordStrengthHint}</p>
                        <ValidationError id="password-errors" errors={fieldErrors.password || []}/>

                        <div className="relative">
                            <input type={showConfirmPassword ? 'text' : 'password'} value={confirmPassword} onChange={(e) => handleConfirmPasswordChange(e.target.value)} placeholder="Confirm password" className="auth-input pr-12 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none" aria-describedby="confirm-password-errors" required/>
                            <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white transition duration-200">{showConfirmPassword ? '🙈' : '👁️'}</button>
                        </div>
                        <ValidationError id="confirm-password-errors" errors={fieldErrors.confirmPassword || []}/>

                        <label className="flex items-start gap-2 auth-helper">
                            <input
                                type="checkbox"
                                checked={acceptTerms}
                                onChange={(e) => setAcceptTerms(e.target.checked)}
                                className="mt-1 h-4 w-4"
                            />
                            <span>I agree to the Terms and Privacy Policy.</span>
                        </label>
                        <ValidationError id="terms-errors" errors={fieldErrors.terms || []}/>

                        <button type="submit" disabled={loading || !password || !confirmPassword} className="btn-primary text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50">{loading ? 'Processing...' : 'Get OTP'}</button>
                    </div>
                    <RateLimitInfo/>
                </form>
            </div>
        </div>
    );
};

export default SignupPage;
