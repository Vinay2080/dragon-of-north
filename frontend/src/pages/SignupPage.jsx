import React, {useEffect, useMemo, useState} from 'react';
import {Link, useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import RateLimitInfo from '../components/RateLimitInfo';
import {useToast} from '../hooks/useToast';
import ValidationError from '../components/Validation/ValidationError';
import AuthFlowProgress from '../components/AuthFlowProgress';
import GoogleLoginButton from '../components/auth/GoogleLoginButton';
import AuthCardLayout from '../components/auth/AuthCardLayout';
import AuthInput from '../components/auth/AuthInput';
import AuthButton from '../components/auth/AuthButton';
import AuthDivider from '../components/auth/AuthDivider';
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
            navigate('/sessions', {replace: true});
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
        navigate('/sessions');
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

        // Step 1: persist an account via POST /api/v1/auth/identifier/sign-up.
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

        // Step 2: only after sign-up success, request OTP for SIGNUP verification.
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
        <AuthCardLayout
            title="Create account"
            subtitle={<span>Setting up account for <span
                className="font-medium text-cyan-300">{identifier}</span></span>}
        >
            <AuthFlowProgress currentStep="signup"/>

            {isEmailIdentifier && (
                <>
                    <AuthDivider label="or sign up with"/>
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
                </>
            )}

            <form onSubmit={handleGetOtp} noValidate>
                <div className="space-y-4">
                    <div className="relative">
                        <AuthInput type={showPassword ? 'text' : 'password'} value={password}
                                   onChange={(e) => handlePasswordChange(e.target.value)} placeholder="Enter password"
                                   className="pr-12" hasError={Boolean(fieldErrors.password?.length)}
                                   aria-describedby="password-hint password-errors" required/>
                        <button type="button" onClick={() => setShowPassword(!showPassword)}
                                className="auth-toggle-visibility">{showPassword ? 'Hide' : 'Show'}</button>
                    </div>
                    <p id="password-hint" className="auth-helper">{passwordStrengthHint}</p>
                    <ValidationError id="password-errors" errors={fieldErrors.password || []}/>

                    <div className="relative">
                        <AuthInput type={showConfirmPassword ? 'text' : 'password'} value={confirmPassword}
                                   onChange={(e) => handleConfirmPasswordChange(e.target.value)}
                                   placeholder="Confirm password" className="pr-12"
                                   hasError={Boolean(fieldErrors.confirmPassword?.length)}
                                   aria-describedby="confirm-password-errors" required/>
                        <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                className="auth-toggle-visibility">{showConfirmPassword ? 'Hide' : 'Show'}</button>
                    </div>
                    <ValidationError id="confirm-password-errors" errors={fieldErrors.confirmPassword || []}/>

                    <label className="auth-helper flex items-start gap-2">
                        <input
                            type="checkbox"
                            checked={acceptTerms}
                            onChange={(e) => setAcceptTerms(e.target.checked)}
                            className="mt-1 h-4 w-4"
                        />
                        <span>I agree to the <Link to="/terms"
                                                   className="auth-link underline">Terms of Service</Link> and <Link
                            to="/privacy" className="auth-link underline">Privacy Policy</Link>.</span>
                    </label>
                    <ValidationError id="terms-errors" errors={fieldErrors.terms || []}/>

                    <AuthButton type="submit"
                                disabled={loading || !password || !confirmPassword}>{loading ? 'Processing...' : 'Get OTP'}</AuthButton>
                </div>
                <RateLimitInfo/>
            </form>
        </AuthCardLayout>
    );
};

export default SignupPage;
