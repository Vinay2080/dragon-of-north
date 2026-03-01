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
        if (!isLoading && isAuthenticated) navigate('/dashboard', {replace: true});
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

        const status = result.data?.app_user_status;
        if (status === 'NOT_EXIST') {
            setNotExistChoice({identifier: processedIdentifier, identifierType, allowGoogleSignup: identifierType === 'EMAIL' && isGoogleEmail(processedIdentifier)});
        } else if (status === 'CREATED') {
            const otpEndpoint = identifierType === 'EMAIL' ? API_CONFIG.ENDPOINTS.EMAIL_OTP_REQUEST : API_CONFIG.ENDPOINTS.PHONE_OTP_REQUEST;
            const otpPayload = identifierType === 'EMAIL' ? {email: processedIdentifier, otp_purpose: 'SIGNUP'} : {phone: processedIdentifier, otp_purpose: 'SIGNUP'};
            const otpResult = await apiService.post(otpEndpoint, otpPayload);
            if (apiService.isErrorResponse(otpResult)) {
                toast.error(otpResult.message || 'Failed to send OTP.');
                setLoading(false);
                return;
            }
            toast.success('OTP sent successfully.');
            navigate('/otp', {state: {identifier: processedIdentifier, identifierType}});
        } else if (status === 'VERIFIED') {
            navigate('/login', {state: {identifier: processedIdentifier}});
        } else if (status === 'BLOCKED') {
            setBlockedMessage('Your account is blocked. Please contact support.');
        } else {
            toast.warning(`Unexpected user status: ${status}`);
        }

        setLoading(false);
    };

    return (
        <div className="relative min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="absolute right-4 top-4 sm:right-8 sm:top-8">
                <details className="group relative">
                    <summary className="cursor-pointer list-none rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-200 hover:border-blue-500">Login ▾</summary>
                    <div className="absolute right-0 mt-2 min-w-72 rounded-lg border border-slate-700 bg-slate-900 p-3 shadow-xl">
                        <p className="mb-2 text-xs text-slate-400">Quick test users</p>
                        {TEST_USERS.map((user) => <button key={user.identifier} type="button" onClick={() => handleQuickLogin(user)} className="mb-2 block w-full rounded-md border border-slate-700 px-3 py-2 text-left text-xs text-slate-200 hover:border-blue-500"><span className="block font-medium">{user.label}</span><span className="block text-slate-400">Password: {user.password}</span></button>)}
                        <button type="button" onClick={() => navigate('/login')} className="mt-1 w-full rounded-md bg-blue-600 px-3 py-2 text-xs font-semibold text-white hover:bg-blue-500">Go to Login</button>
                    </div>
                </details>
            </div>

            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">Sign In / Sign Up</h2>
                <p className="mt-1 mb-6 text-sm text-slate-400">Continue with email or phone number</p>
                <AuthFlowProgress currentStep="identifier"/>
                {blockedMessage && <div className="mb-4 rounded-lg border border-red-800 bg-red-950 px-4 py-3 text-sm text-red-300">{blockedMessage}</div>}
                <form onSubmit={handleSubmit} noValidate>
                    <input type="text" value={identifier} onChange={(e) => handleIdentifierChange(e.target.value)} disabled={loading} placeholder="Email or phone number" className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none disabled:opacity-50" aria-invalid={!!fieldErrors.identifier?.length} aria-describedby="identifier-field-errors" required/>
                    <ValidationError id="identifier-field-errors" errors={fieldErrors.identifier || []}/>
                    <button type="submit" disabled={loading || !identifier.trim()} className="mt-5 w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50">{loading ? 'Processing…' : 'Continue'}</button>
                </form>

                {notExistChoice && (
                    <div className="mt-6 space-y-3 rounded-lg border border-slate-800 bg-slate-900/60 p-4">
                        <p className="text-sm text-slate-300">No account found. Choose how to continue:</p>
                        <button
                            type="button"
                            onClick={() => navigate('/signup', {state: {identifier: notExistChoice.identifier, identifierType: notExistChoice.identifierType}})}
                            className="w-full rounded-lg bg-blue-600 py-3 text-sm font-semibold text-white hover:bg-blue-500"
                        >
                            Continue with email signup
                        </button>
                        {notExistChoice.allowGoogleSignup && (
                            <GoogleLoginButton
                                mode="signup"
                                onSuccess={() => {
                                    login({identifier: notExistChoice.identifier});
                                    navigate('/dashboard');
                                }}
                                onError={(message) => toast.error(message || 'Google signup failed.')}
                                disabled={loading}
                            />
                        )}
                    </div>
                )}
                <p className="mt-6 text-center text-xs text-slate-500">We may send an OTP if required</p>
            </div>
        </div>
    );
};

export default AuthIdentifierPage;
