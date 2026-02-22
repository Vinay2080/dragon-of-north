import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {useAuth} from '../context/authUtils';

const TEST_USERS = [
    {
        label: 'shaking.121@gmail.com',
        identifier: 'shaking.121@gmail.com',
        password: 'Example@123',
    },
];

const AuthIdentifierPage = () => {
    const navigate = useNavigate();
    const {isAuthenticated, isLoading} = useAuth();
    const [identifier, setIdentifier] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [blockedMessage, setBlockedMessage] = useState('');

    useEffect(() => {
        if (!isLoading && isAuthenticated) {
            navigate('/dashboard', {replace: true});
        }
    }, [isAuthenticated, isLoading, navigate]);

    const detectIdentifierType = (value) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (emailRegex.test(value)) return 'EMAIL';
        return 'PHONE';
    };

    const normalizePhone = (value) => value.replace(/\D/g, '');

    const handleQuickLogin = (user) => {
        navigate('/login', {
            state: {
                identifier: user.identifier,
                password: user.password,
            },
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setBlockedMessage('');

        const trimmed = identifier.trim();
        if (!trimmed) {
            setError('Please enter your email or phone number.');
            return;
        }

        const identifierType = detectIdentifierType(trimmed);
        const processedIdentifier = identifierType === 'PHONE' ? normalizePhone(trimmed) : trimmed;

        setLoading(true);

        try {
            const response = await fetch(`${API_CONFIG.BASE_URL}/api/v1/auth/identifier/status`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    identifier: processedIdentifier,
                    identifier_type: identifierType,
                }),
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                setError(errorData.message || 'An error occurred. Please try again.');
                return;
            }

            const result = await response.json();

            if (result.api_response_status === 'success') {
                const status = result.data.app_user_status;

                switch (status) {
                    case 'NOT_EXIST':
                        navigate('/signup', {state: {identifier: processedIdentifier, identifierType}});
                        break;
                    case 'CREATED':
                        setLoading(true);
                        try {
                            const otpEndpoint = identifierType === 'EMAIL'
                                ? `${API_CONFIG.BASE_URL}/api/v1/otp/email/request`
                                : `${API_CONFIG.BASE_URL}/api/v1/otp/phone/request`;

                            const otpPayload = identifierType === 'EMAIL'
                                ? {email: processedIdentifier, otp_purpose: 'SIGNUP'}
                                : {phone: processedIdentifier, otp_purpose: 'SIGNUP'};

                            const otpResponse = await fetch(otpEndpoint, {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json',
                                },
                                body: JSON.stringify(otpPayload),
                            });

                            if (!otpResponse.ok) {
                                const otpError = await otpResponse.json().catch(() => ({}));
                                setError(otpError.message || 'Failed to send OTP. Please try again.');
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
                        navigate('/login', {state: {identifier: processedIdentifier}});
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
        <div
            className="relative min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="absolute right-4 top-4 sm:right-8 sm:top-8">
                <details className="group relative">
                    <summary
                        className="cursor-pointer list-none rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-200 hover:border-blue-500">
                        Login ▾
                    </summary>
                    <div
                        className="absolute right-0 mt-2 min-w-72 rounded-lg border border-slate-700 bg-slate-900 p-3 shadow-xl">
                        <p className="mb-2 text-xs text-slate-400">Quick test users</p>
                        {TEST_USERS.map((user) => (
                            <button
                                key={user.identifier}
                                type="button"
                                onClick={() => handleQuickLogin(user)}
                                className="mb-2 block w-full rounded-md border border-slate-700 px-3 py-2 text-left text-xs text-slate-200 hover:border-blue-500"
                            >
                                <span className="block font-medium">{user.label}</span>
                                <span className="block text-slate-400">Password: {user.password}</span>
                            </button>
                        ))}
                        <button
                            type="button"
                            onClick={() => navigate('/login')}
                            className="mt-1 w-full rounded-md bg-blue-600 px-3 py-2 text-xs font-semibold text-white hover:bg-blue-500"
                        >
                            Go to Login
                        </button>
                    </div>
                </details>
            </div>

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