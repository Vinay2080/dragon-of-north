import React, {useState} from 'react';
import {useNavigate} from 'react-router-dom';

/**
 * Regex for basic email validation.
 */
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Regex for basic phone validation (10-digit number with optional +91 or 0 prefix)
 */
const PHONE_REGEX = /^(\+91|0)?[6-9]\d{9}$/;

const API_BASE_URL = 'http://localhost:8080'; // Update this to match your backend port

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

        // If it's a phone number, clean it up and add +91 country code if needed
        if (identifierType === 'PHONE') {
            // Remove all non-digit characters
            const digitsOnly = processedIdentifier.replace(/\D/g, '');
            // If it starts with 91, remove it first
            const cleanNumber = digitsOnly.startsWith('91') ? digitsOnly.substring(2) : digitsOnly;
            // Add +91 prefix
            processedIdentifier = `+91${cleanNumber}`;
        }

        if (!identifierType) {
            setError('Please enter a valid email or phone number.');
            return;
        }

        setLoading(true);

        try {
            const response = await fetch(`${API_BASE_URL}/api/v1/auth/identifier/status`, {
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
                // Handle non-2xx responses
                try {

                    const errorData = await response.json().catch(() => ({}));
                    setError(errorData.message || 'An error occurred. Please try again.');
                } catch {
                    setError('Network error. Please check your connection and try again.');
                }

            }

            const result = await response.json();

            // Read response using snake_case keys as per requirement
            if (result.api_response_status === 'success') {
                const status = result.data.app_user_status;

                switch (status) {
                    case 'NOT_EXIST':
                        navigate('/signup');
                        break;
                    case 'CREATED':
                        navigate('/otp');
                        break;
                    case 'VERIFIED':
                        navigate('/dashboard');
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
