import React, {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';

const ForgotPasswordRequestPage = () => {
    const navigate = useNavigate();

    const [identifier, setIdentifier] = useState('');
    const [identifierType, setIdentifierType] = useState('EMAIL');
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccessMessage('');
        setLoading(true);

        try {
            const result = await apiService.post(API_CONFIG.ENDPOINTS.PASSWORD_RESET_REQUEST, {
                identifier,
                identifier_type: identifierType,
            });

            if (result.api_response_status === 'success') {
                setSuccessMessage(result.message || 'OTP sent successfully');
                navigate('/reset-password', {
                    state: {
                        identifier,
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

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">Forgot Password</h2>
                <p className="mt-1 mb-6 text-sm text-slate-400">
                    Enter your email or phone to receive a password reset OTP.
                </p>

                <form onSubmit={handleSubmit} noValidate className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-1">Identifier Type</label>
                        <select
                            value={identifierType}
                            onChange={(e) => setIdentifierType(e.target.value)}
                            className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white focus:border-blue-500 focus:outline-none"
                        >
                            <option value="EMAIL">Email</option>
                            <option value="PHONE">Phone</option>
                        </select>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-1">
                            {identifierType === 'EMAIL' ? 'Email Address' : 'Phone Number'}
                        </label>
                        <input
                            type="text"
                            value={identifier}
                            onChange={(e) => setIdentifier(e.target.value)}
                            placeholder={identifierType === 'EMAIL' ? 'name@example.com' : '9876543210'}
                            className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                            required
                        />
                    </div>

                    {error && <p className="text-sm text-red-400">{error}</p>}
                    {successMessage && <p className="text-sm text-green-400">{successMessage}</p>}

                    <button
                        type="submit"
                        disabled={loading || !identifier}
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
