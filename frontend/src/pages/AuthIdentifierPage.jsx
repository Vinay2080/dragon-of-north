import React, {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^[6-9]\d{9}$/;

const AuthIdentifierPage = () => {
    const [identifier, setIdentifier] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [blockedMessage, setBlockedMessage] = useState('');
    const navigate = useNavigate();

    const detectIdentifierType = (value) => {
        if (EMAIL_REGEX.test(value)) return 'EMAIL';
        if (PHONE_REGEX.test(value)) return 'PHONE';
        return null;
    };

    const sendSignupOtp = async (idType, processedIdentifier) => {
        const otpEndpoint = idType === 'EMAIL'
            ? API_CONFIG.ENDPOINTS.EMAIL_OTP_REQUEST
            : API_CONFIG.ENDPOINTS.PHONE_OTP_REQUEST;

        const otpPayload = idType === 'EMAIL'
            ? {email: processedIdentifier, otp_purpose: 'SIGNUP'}
            : {phone: processedIdentifier, otp_purpose: 'SIGNUP'};

        const otpResult = await apiService.post(otpEndpoint, otpPayload);

        if (apiService.isErrorResponse(otpResult)) {
            if (otpResult.type === 'RATE_LIMIT_EXCEEDED') {
                setError(`Too many OTP requests. Please wait ${otpResult.retryAfter ?? 60} seconds before trying again.`);
                return false;
            }
            setError(otpResult.message || 'Failed to send OTP. Please try again.');
            return false;
        }

        return otpResult?.api_response_status === 'success';
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setBlockedMessage('');

        let processedIdentifier = identifier.trim();
        const identifierType = detectIdentifierType(processedIdentifier);

        if (identifierType === 'PHONE') {
            processedIdentifier = processedIdentifier.replace(/\D/g, '');
        }

        if (!identifierType) {
            setError('Please enter a valid email or phone number.');
            return;
        }

        setLoading(true);

        const result = await apiService.post(API_CONFIG.ENDPOINTS.IDENTIFIER_STATUS, {
            identifier: processedIdentifier,
            identifier_type: identifierType,
        });

        if (apiService.isErrorResponse(result)) {
            if (result.type === 'RATE_LIMIT_EXCEEDED') {
                setError(`Too many attempts. Please wait ${result.retryAfter ?? 60} seconds and retry.`);
            } else {
                setError(result.message || 'An error occurred. Please try again.');
            }
            setLoading(false);
            return;
        }

        if (result?.api_response_status !== 'success') {
            setError(result?.message || 'Something went wrong');
            setLoading(false);
            return;
        }

        const status = result.data?.app_user_status;
        switch (status) {
            case 'NOT_EXIST':
                navigate('/signup', {state: {identifier: processedIdentifier, identifierType}});
                break;
            case 'CREATED': {
                const otpSent = await sendSignupOtp(identifierType, processedIdentifier);
                if (otpSent) {
                    navigate('/otp', {state: {identifier: processedIdentifier, identifierType}});
                }
                break;
            }
            case 'VERIFIED':
                navigate('/login', {state: {identifier: processedIdentifier}});
                break;
            case 'DELETED':
                setBlockedMessage('This account is deleted. Please contact support to recover access.');
                break;
            default:
                setError(`Unexpected user status: ${status}`);
        }

        setLoading(false);
    };

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">Sign In / Sign Up</h2>

                <p className="mt-1 mb-6 text-sm text-slate-400">Continue with email or phone number</p>

                {blockedMessage && (
                    <div className="mb-4 rounded-lg border border-red-800 bg-red-950 px-4 py-3 text-sm text-red-300">
                        {blockedMessage}
                    </div>
                )}

                <form onSubmit={handleSubmit} noValidate>
                    <div className="space-y-4">
                        <div>
                            <label className="mb-1 block text-sm font-medium text-slate-400">Email or Phone</label>
                            <input
                                type="text"
                                value={identifier}
                                onChange={(e) => setIdentifier(e.target.value)}
                                placeholder="Enter email or phone number"
                                className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                                required
                            />
                        </div>

                        {error && <p className="text-sm text-red-400">{error}</p>}

                        <button
                            type="submit"
                            disabled={loading || !identifier}
                            className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {loading ? 'Checking...' : 'Continue'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default AuthIdentifierPage;
