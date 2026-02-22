import React, {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';

const isEmailIdentifier = (value) => /.+@.+\..+/.test(value.trim());

const ForgotPasswordRequestPage = () => {
    const navigate = useNavigate();
    const [identifier, setIdentifier] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccessMessage('');
        setLoading(true);

        const trimmedIdentifier = identifier.trim();
        const identifierType = isEmailIdentifier(trimmedIdentifier) ? 'EMAIL' : 'PHONE';

        try {
            const result = await apiService.post(API_CONFIG.ENDPOINTS.PASSWORD_RESET_REQUEST, {
                identifier: trimmedIdentifier,
                identifier_type: identifierType,
            });

            if (result.api_response_status === 'success') {
                setSuccessMessage(result.message || 'OTP sent successfully.');
                navigate('/reset-password', {
                    state: {
                        identifier: trimmedIdentifier,
                        identifierType,
                    },
                });
            } else {
                setError(result.message || 'Could not send OTP. Please try again.');
            }
        } catch (err) {
            setError(err.message || 'Could not send OTP. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const inferredType = isEmailIdentifier(identifier) ? 'Email' : 'Phone';

    return (
        <div
            className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">Forgot Password</h2>
                <p className="mt-1 mb-6 text-sm text-slate-400">
                    Enter your email or phone to receive a password reset OTP.
                </p>

                <form onSubmit={handleSubmit} noValidate className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-1">
                            Identifier ({inferredType})
                        </label>
                        <input
                            type="text"
                            value={identifier}
                            onChange={(e) => setIdentifier(e.target.value)}
                            placeholder="name@example.com or 9876543210"
                            className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                            required
                        />
                    </div>

                    {error && <p className="text-sm text-red-400">{error}</p>}
                    {successMessage && <p className="text-sm text-green-400">{successMessage}</p>}

                    <button
                        type="submit"
                        disabled={loading || !identifier.trim()}
                        className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {loading ? 'Sending OTP...' : 'Send OTP'}
                    </button>
                </form>

                <div className="mt-6 text-center">
                    <button
                        onClick={() => navigate('/login')}
                        className="text-xs text-slate-500 hover:text-white transition"
                    >
                        Back to Login
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ForgotPasswordRequestPage;