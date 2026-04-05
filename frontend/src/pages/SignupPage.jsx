import React, {useEffect, useMemo, useState} from 'react';
import {Link, useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import {useAuthState} from '../hooks/authStateHook';
import {AuthLoadingOverlay} from '../components/auth/AuthStateComponents';
import RateLimitInfo from '../components/RateLimitInfo';
import {useToast} from '../hooks/useToast';
import ValidationError from '../components/Validation/ValidationError';
import PasswordValidationChecklist from '../components/auth/PasswordValidationChecklist';
import AuthFlowProgress from '../components/AuthFlowProgress';
import GoogleLoginButton from '../components/auth/GoogleLoginButton';
import AuthCardLayout from '../components/auth/AuthCardLayout';
import AuthInput from '../components/auth/AuthInput';
import AuthButton from '../components/auth/AuthButton';
import AuthDivider from '../components/auth/AuthDivider';
import {validatePassword} from '../utils/validation';
import {useAuth} from '../context/authUtils';
import AlertBanner from '../components/AlertBanner.jsx';

const OTP_SESSION_KEYS = {
    IDENTIFIER: 'otpIdentifier',
    IDENTIFIER_TYPE: 'otpIdentifierType',
    FLOW: 'otpFlow',
};

const SignupPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const {toast} = useToast();
    const {login, isAuthenticated, isLoading} = useAuth();
    const authState = useAuthState();
    const {identifier, identifierType} = location.state || {};

    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [acceptTerms, setAcceptTerms] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});
    const [loading, setLoading] = useState(false);

    const reason = useMemo(() => {
        const stateReason = location.state?.reason;
        const queryReason = new URLSearchParams(location.search).get('reason');
        return stateReason || queryReason;
    }, [location.search, location.state]);

    const banner = useMemo(() => {
        if (reason === 'deleted') {
            return {type: 'error', message: 'User was deleted. Please sign up again to reactivate.'};
        }

        if (reason === 'inactive') {
            return {type: 'info', message: 'User not found. Please sign up.'};
        }

        return null;
    }, [reason]);

    useEffect(() => {
        if (!reason) {
            return;
        }

        const params = new URLSearchParams(location.search);
        params.delete('reason');
        const nextState = {...(location.state || {})};
        delete nextState.reason;

        navigate({
            pathname: location.pathname,
            search: params.toString() ? `?${params.toString()}` : '',
        }, {
            replace: true,
            state: Object.keys(nextState).length ? nextState : null,
        });
    }, [location.pathname, location.search, location.state, navigate, reason]);

    useEffect(() => {
        if (!isLoading && isAuthenticated) {
            navigate('/sessions', {replace: true});
        }
    }, [isAuthenticated, isLoading, navigate]);

    const isEmailIdentifier = identifierType === 'EMAIL';

    // Compute password validity state
    const passwordErrors = useMemo(() => validatePassword(password), [password]);
    const isPasswordValid = password && passwordErrors.length === 0;

    // Confirm password only validates if password is valid
    const confirmPasswordErrors = useMemo(() => {
        if (!isPasswordValid) return [];
        return confirmPassword && confirmPassword !== password ? ['Passwords do not match'] : [];
    }, [isPasswordValid, confirmPassword, password]);

    // Button is enabled only when all conditions met
    const isFormValid = useMemo(() => {
        return isPasswordValid &&
            confirmPassword === password &&
            acceptTerms &&
            !loading &&
            !authState.isLoading;
    }, [isPasswordValid, confirmPassword, password, acceptTerms, loading, authState.isLoading]);

    const handlePasswordChange = (value) => {
        setPassword(value);
        // Only set errors if user has typed something and password is invalid
        setFieldErrors(prev => ({
            ...prev,
            password: value ? validatePassword(value) : [],
            // Clear confirm password error if password becomes invalid
            confirmPassword: []
        }));
    };

    const handleConfirmPasswordChange = (value) => {
        setConfirmPassword(value);
        // Only validate confirm password if primary password is valid
        if (!isPasswordValid) {
            setFieldErrors(prev => ({...prev, confirmPassword: []}));
            return;
        }
        setFieldErrors(prev => ({
            ...prev,
            confirmPassword: value && value !== password ? ['Passwords do not match'] : []
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
        authState.setLoading('Creating account...');

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
            const errorMsg = signupResult.message || 'Signup failed.';
            toast.error(errorMsg);
            authState.setError(errorMsg);
            setLoading(false);
            return;
        }

        if (signupResult?.api_response_status !== 'success') {
            const errorMsg = signupResult?.message || 'Something went wrong';
            toast.error(errorMsg);
            authState.setError(errorMsg);
            setLoading(false);
            return;
        }

        // Step 2: request OTP for email verification.
        authState.setLoading('Sending verification code...');
        const otpEndpoint = identifierType === 'EMAIL' ? API_CONFIG.ENDPOINTS.EMAIL_OTP_REQUEST : API_CONFIG.ENDPOINTS.PHONE_OTP_REQUEST;
        const otpPayload = identifierType === 'EMAIL' ? {email: identifier, otp_purpose: 'SIGNUP'} : {phone: identifier, otp_purpose: 'SIGNUP'};
        const otpResult = await apiService.post(otpEndpoint, otpPayload);

        if (apiService.isErrorResponse(otpResult)) {
            const errorMsg = otpResult.message || 'Failed to send verification code.';
            toast.error(errorMsg);
            authState.setError(errorMsg);
            setLoading(false);
            return;
        }

        setLoading(false);
        authState.setIdle();

        // Step 3: navigate to OTP page for verification.
        sessionStorage.setItem(OTP_SESSION_KEYS.IDENTIFIER, identifier);
        sessionStorage.setItem(OTP_SESSION_KEYS.IDENTIFIER_TYPE, identifierType);
        sessionStorage.setItem(OTP_SESSION_KEYS.FLOW, 'SIGNUP');
        navigate('/otp', {
            state: {
                identifier,
                identifierType,
                flow: 'SIGNUP',
            },
        });
    };

    if (!identifier) {
        navigate('/');
        return null;
    }

    return (
        <>
            <AuthLoadingOverlay isVisible={authState.isLoading} message={authState.message}/>

            <AuthCardLayout
                title="Create account"
                subtitle={<span>Setting up account for <span
                    className="font-medium text-cyan-300">{identifier}</span></span>}
            >
                <AlertBanner type={banner?.type} message={banner?.message}/>
                <AuthFlowProgress currentStep="signup"/>

                <form onSubmit={handleGetOtp} noValidate>
                    <div className="space-y-4">
                            {/* Password Input */}
                            <div className="relative">
                                <AuthInput
                                    type={showPassword ? 'text' : 'password'}
                                    value={password}
                                    onChange={(e) => handlePasswordChange(e.target.value)}
                                    placeholder="Enter password"
                                    className="pr-12"
                                    hasError={password && passwordErrors.length > 0}
                                    aria-describedby="password-errors"
                                    disabled={loading || authState.isLoading}
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="auth-toggle-visibility"
                                    disabled={loading || authState.isLoading || !password}
                                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                                >
                                    {showPassword ? 'Hide' : 'Show'}
                                </button>
                            </div>

                            {/* Password Validation Checklist - shows only when user types */}
                            <PasswordValidationChecklist password={password}/>

                            {/* Password Errors (inline) */}
                            <ValidationError id="password-errors"
                                             errors={password && passwordErrors.length > 0 ? ['Password does not meet requirements'] : []}/>

                            {/* Confirm Password Input - only active after password is valid */}
                            <div className="relative">
                                <AuthInput
                                    type={showConfirmPassword ? 'text' : 'password'}
                                    value={confirmPassword}
                                    onChange={(e) => handleConfirmPasswordChange(e.target.value)}
                                    placeholder="Confirm password"
                                    className="pr-12"
                                    hasError={confirmPasswordErrors.length > 0}
                                    aria-describedby="confirm-password-errors"
                                    disabled={!isPasswordValid || loading || authState.isLoading}
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    className="auth-toggle-visibility"
                                    disabled={!isPasswordValid || loading || authState.isLoading || !confirmPassword}
                                    aria-label={showConfirmPassword ? 'Hide confirm password' : 'Show confirm password'}
                                >
                                    {showConfirmPassword ? 'Hide' : 'Show'}
                                </button>
                            </div>
                            <ValidationError id="confirm-password-errors" errors={confirmPasswordErrors}/>

                            {/* Terms & Privacy */}
                            <label className="auth-helper flex items-start gap-2">
                                <input
                                    type="checkbox"
                                    checked={acceptTerms}
                                    onChange={(e) => setAcceptTerms(e.target.checked)}
                                    className="mt-1 h-4 w-4"
                                    disabled={loading || authState.isLoading}
                                    aria-label="Accept terms of service and privacy policy"
                                />
                                <span>I agree to the <Link to="/terms"
                                                           className="auth-link underline">Terms of Service</Link> and <Link
                                    to="/privacy" className="auth-link underline">Privacy Policy</Link>.</span>
                            </label>
                            <ValidationError id="terms-errors" errors={fieldErrors.terms || []}/>

                            {/* Primary CTA Button - only enabled when all conditions met */}
                            <AuthButton
                                type="submit"
                                disabled={!isFormValid}
                                aria-label={isFormValid ? 'Get OTP' : 'Complete all fields to continue'}
                            >
                                {loading ? 'Sending OTP...' : 'Get OTP'}
                            </AuthButton>
                    </div>
                    <RateLimitInfo/>
                </form>

                {/* Google button moved below main CTA */}
                {isEmailIdentifier && (
                    <div className="mt-6">
                        <AuthDivider label="OR continue with"/>
                        <div className="auth-section">
                            <GoogleLoginButton
                                mode="signup"
                                onSuccess={handleGoogleSignup}
                                onError={(message) => {
                                    authState.setError(message || 'Google signup failed.');
                                    toast.error(message || 'Google signup failed.');
                                }}
                                disabled={loading || authState.isLoading}
                                expectedIdentifier={identifier}
                            />
                        </div>
                    </div>
                )}
            </AuthCardLayout>
        </>
    );
};

export default SignupPage;
