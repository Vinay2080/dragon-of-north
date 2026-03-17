import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {useAuth} from '../context/authUtils';
import {apiService} from '../services/apiService';
import {useToast} from '../hooks/useToast';
import ValidationError from '../components/Validation/ValidationError';
import AuthFlowProgress from '../components/AuthFlowProgress';
import GoogleLoginButton from '../components/auth/GoogleLoginButton';
import {getIdentifierType, normalizePhone, validateIdentifier} from '../utils/validation';

const TEST_USERS = [{label: 'shaking.121@gmail.com', identifier: 'shaking.121@gmail.com', password: 'Example@123'}];

const isGoogleEmail = (value) => {
    const emailValue = value.trim().toLowerCase();
    return emailValue.endsWith('@gmail.com') || emailValue.endsWith('@googlemail.com');
};

const AuthIdentifierPage = () => {
    const navigate = useNavigate();
    const {login, isAuthenticated, isLoading} = useAuth();
    const {toast} = useToast();
    const [identifier, setIdentifier] = useState('');
    const [loading, setLoading] = useState(false);
    const [blockedMessage, setBlockedMessage] = useState('');
    const [fieldErrors, setFieldErrors] = useState({});
    const [notExistChoice, setNotExistChoice] = useState(null);

    useEffect(() => {
        if (!isLoading && isAuthenticated) navigate('/sessions', {replace: true});
    }, [isAuthenticated, isLoading, navigate]);

    const handleQuickLogin = (user) => navigate('/login', {state: {identifier: user.identifier, password: user.password}});

    const handleIdentifierChange = (value) => {
        setIdentifier(value);
        setNotExistChoice(null);
        const error = validateIdentifier(value);
        setFieldErrors(prev => ({...prev, identifier: error ? [error] : []}));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setBlockedMessage('');
        setNotExistChoice(null);
        setFieldErrors({});

        const trimmed = identifier.trim();
        if (!trimmed) {
            setFieldErrors({identifier: ['Please enter your email or phone number.']});
            return;
        }

        const identifierError = validateIdentifier(trimmed);
        if (identifierError) {
            setFieldErrors({identifier: [identifierError]});
            return;
        }

        const identifierType = getIdentifierType(trimmed);
        const processedIdentifier = identifierType === 'PHONE' ? normalizePhone(trimmed) : trimmed;
        setLoading(true);

        // Backend lookup: POST /api/v1/auth/identifier/status
        const result = await apiService.post(API_CONFIG.ENDPOINTS.IDENTIFIER_STATUS, {
            identifier: processedIdentifier,
            identifier_type: identifierType,
        });

        if (apiService.isErrorResponse(result)) {
            const errors = result?.fieldErrors?.reduce((acc, item) => {
                acc[item.field] = [...(acc[item.field] || []), item.message];
                return acc;
            }, {}) || {};
            setFieldErrors(errors);
            toast.error(result.message || 'Could not verify identifier status.');
            setLoading(false);
            return;
        }

        if (result?.api_response_status !== 'success') {
            toast.error(result?.message || 'Something went wrong.');
            setLoading(false);
            return;
        }

        // Response contract used here: data.exists, data.app_user_status, data.email_verified
        const data = result?.data || {};
        const status = data.app_user_status;
        const exists = Boolean(data.exists);
        const emailVerified = Boolean(data.email_verified);

        if (!exists || status === 'NOT_EXIST' || status === 'NOT_EXISTS') {
            setNotExistChoice({identifier: processedIdentifier, identifierType, allowGoogleSignup: identifierType === 'EMAIL' && isGoogleEmail(processedIdentifier)});
        } else if (status === 'LOCKED' || status === 'DELETED' || status === 'BLOCKED') {
            setBlockedMessage('Your account is blocked. Please contact support.');
        } else if (status === 'ACTIVE' || status === 'CREATED' || status === 'VERIFIED') {
            if (emailVerified || status === 'VERIFIED') {
                navigate('/login', {state: {identifier: processedIdentifier}});
            } else {
                // Existing but unverified account: request SIGNUP OTP before allowing completion.
                const otpEndpoint = identifierType === 'EMAIL' ? API_CONFIG.ENDPOINTS.EMAIL_OTP_REQUEST : API_CONFIG.ENDPOINTS.PHONE_OTP_REQUEST;
                const otpPayload = identifierType === 'EMAIL' ? {
                    email: processedIdentifier,
                    otp_purpose: 'SIGNUP'
                } : {phone: processedIdentifier, otp_purpose: 'SIGNUP'};
                const otpResult = await apiService.post(otpEndpoint, otpPayload);
                if (apiService.isErrorResponse(otpResult)) {
                    toast.error(otpResult.message || 'Failed to send OTP.');
                    setLoading(false);
                    return;
                }
                toast.success('OTP sent successfully.');
                navigate('/otp', {state: {identifier: processedIdentifier, identifierType}});
            }
        } else {
            toast.warning(`Unexpected user status: ${status ?? 'UNKNOWN'}`);
        }

        setLoading(false);
    };

    return (
        <div className="relative auth-shell">
            <div className="absolute right-4 top-4 sm:right-8 sm:top-8">
                <details className="group relative">
                    <summary className="auth-quick-summary">Login ▾</summary>
                    <div className="absolute right-0 auth-quick-panel">
                        <p className="auth-quick-title">Quick test users</p>
                        {TEST_USERS.map((user) => <button key={user.identifier} type="button"
                                                          onClick={() => handleQuickLogin(user)}
                                                          className="auth-quick-user"><span
                            className="block font-medium">{user.label}</span><span
                            className="auth-quick-user__meta block">Password: {user.password}</span></button>)}
                        <button type="button" onClick={() => navigate('/login')}
                                className="btn-primary w-full text-xs">Go to Login
                        </button>
                    </div>
                </details>
            </div>

            <div className="auth-card">
                <h2 className="auth-title">Sign In / Sign Up</h2>
                <p className="auth-subtitle mb-6">Continue with email or phone number</p>
                <AuthFlowProgress currentStep="identifier"/>
                {blockedMessage && <div className="auth-alert">{blockedMessage}</div>}
                <form onSubmit={handleSubmit} noValidate>
                    <input type="text" value={identifier} onChange={(e) => handleIdentifierChange(e.target.value)}
                           disabled={loading} placeholder="Email or phone number"
                           className="auth-input disabled:opacity-50" aria-invalid={!!fieldErrors.identifier?.length}
                           aria-describedby="identifier-field-errors" required/>
                    <ValidationError id="identifier-field-errors" errors={fieldErrors.identifier || []}/>
                    <button type="submit" disabled={loading || !identifier.trim()}
                            className="mt-5 btn-primary disabled:cursor-not-allowed disabled:opacity-50">{loading ? 'Processing…' : 'Continue'}</button>
                </form>

                {notExistChoice && (
                    <div className="auth-section">
                        <p className="auth-helper">No account found. Choose how to continue:</p>
                        <button
                            type="button"
                            onClick={() => navigate('/signup', {state: {identifier: notExistChoice.identifier, identifierType: notExistChoice.identifierType}})}
                            className="btn-primary"
                        >
                            Continue with email signup
                        </button>
                        {notExistChoice.allowGoogleSignup && (
                            <GoogleLoginButton
                                mode="signup"
                                onSuccess={() => {
                                    login({identifier: notExistChoice.identifier});
                                    navigate('/sessions');
                                }}
                                onError={(message) => toast.error(message || 'Google signup failed.')}
                                disabled={loading}
                                expectedIdentifier={notExistChoice.identifier}
                            />
                        )}
                    </div>
                )}
                <p className="mt-6 text-center auth-helper">We may send an OTP if required</p>
            </div>
        </div>
    );
};

export default AuthIdentifierPage;
