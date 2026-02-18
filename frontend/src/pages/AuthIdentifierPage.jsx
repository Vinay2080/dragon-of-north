import React, {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {apiService} from '../services/apiService';

/**
 * Regex for basic email validation.
 */
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Regex for basic phone validation (10-digit number starting with 6-9)
 */
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

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setBlockedMessage('');

        let processedIdentifier = identifier.trim();
        const identifierType = detectIdentifierType(processedIdentifier);

        // If it's a phone number, clean it up (remove all non-digit characters)
        if (identifierType === 'PHONE') {
            processedIdentifier = processedIdentifier.replace(/\D/g, '');
        }

        if (!identifierType) {
            setError('Please enter a valid email or phone number.');
            return;
        }

        setLoading(true);

        try {
            const result = await apiService.post('/api/v1/auth/identifier/status', {
                body: JSON.stringify({
                    identifier: processedIdentifier,
                    identifier_type: identifierType,
                }),
            });

            if (result.type === 'NETWORK_ERROR' || result.type === 'API_ERROR') {
                setError(result.message || 'An error occurred. Please try again.');
                return;
            }

            if (result.type === 'RATE_LIMIT_EXCEEDED') {
                setError(`Rate limit exceeded. Please try again in ${result.retryAfter} seconds.`);
                return;
            }

            // Read response using snake_case keys as per requirement
            if (result.apiResponseStatus === 'success') {
                const status = result.data.app_user_status;

                switch (status) {
                    case 'NOT_EXIST':
                        navigate('/signup', { state: { identifier: processedIdentifier, identifierType } });
                        break;
                    case 'CREATED':
                        setLoading(true);
                        try {
                            const otpEndpoint = identifierType === 'EMAIL'
                                ? '/api/v1/otp/email/request'
                                : '/api/v1/otp/phone/request';

                            const otpPayload = identifierType === 'EMAIL'
                                ? {email: processedIdentifier, otp_purpose: 'SIGNUP'}
                                : {phone: processedIdentifier, otp_purpose: 'SIGNUP'};

                            const otpResponse = await apiService.post(otpEndpoint, {
                                body: JSON.stringify(otpPayload),
                            });

                            if (otpResponse.type === 'NETWORK_ERROR' || otpResponse.type === 'API_ERROR') {
                                setError(otpResponse.message || 'Failed to send OTP. Please try again.');
                                return;
                            }

                            if (otpResponse.type === 'RATE_LIMIT_EXCEEDED') {
                                setError(`Rate limit exceeded. Please try again in ${otpResponse.retryAfter} seconds.`);
                                return;
                            }

                            navigate('/otp', {state: {identifier: processedIdentifier, identifierType}});
                        } catch (otpErr) {
                            setError('Failed to send OTP. Please check your connection.');
                            console.error('OTP API Error:', otpErr);
                        } finally {
                            setLoading(false);
                        }
                        break;
                    case 'VERIFIED':
                        navigate('/login', { state: { identifier: processedIdentifier } });
                        break;
                    case 'BLOCKED':
                        setBlockedMessage('Your account is blocked. Please contact support.');
                        break;
                    default:
                        setError(`Unexpected user status: ${status}`);
                }
            } else {
                setError(result.message || 'Something went wrong');
            }
        } catch (err) {
            setError(err.message || 'Failed to connect to the server. Please try again later.');
            console.error('API Error:', err);
        } finally {
            setLoading(false);
        }
    };
    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">
                    Sign In / Sign Up
                </h2>

                <p className="mt-1 mb-6 text-sm text-slate-400">
                    Continue with email or phone number
                </p>

                {blockedMessage && (
                    <div className="mb-4 rounded-lg border border-red-800 bg-red-950 px-4 py-3 text-sm text-red-300">
                        {blockedMessage}
                    </div>
                )}

                <form onSubmit={handleSubmit} noValidate>
                    <input
                        type="text"
                        value={identifier}
                        onChange={(e) => setIdentifier(e.target.value)}
                        disabled={loading}
                        placeholder="Email or phone number"
                        className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none disabled:opacity-50"
                        required
                    />

                    {error && (
                        <p className="mt-3 text-sm text-red-400">
                            {error}
                        </p>
                    )}

                    <button
                        type="submit"
                        disabled={loading || !identifier.trim()}
                        className="mt-5 w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {loading ? 'Processing…' : 'Continue'}
                    </button>
                </form>

                <p className="mt-6 text-center text-xs text-slate-500">
                    We may send an OTP if required
                </p>
            </div>
        </div>
    );

};


export default AuthIdentifierPage;
