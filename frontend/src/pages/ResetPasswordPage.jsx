import React, {useMemo, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';

const ResetPasswordPage = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const identifierFromState = location.state?.identifier || '';
    const identifierTypeFromState = location.state?.identifierType || 'EMAIL';

    const [identifier, setIdentifier] = useState(identifierFromState);
    const [identifierType, setIdentifierType] = useState(identifierTypeFromState);
    const [otp, setOtp] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [loading, setLoading] = useState(false);

    const canSubmit = useMemo(() => identifier && otp && newPassword, [identifier, otp, newPassword]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccessMessage('');
        setLoading(true);

        try {
            const result = await apiService.post(API_CONFIG.ENDPOINTS.PASSWORD_RESET_CONFIRM, {
                identifier,
                identifier_type: identifierType,
                otp,
                new_password: newPassword,
            });

            if (result.api_response_status === 'success') {
                setSuccessMessage(result.message || 'Password reset successful');
                setTimeout(() => navigate('/login', {state: {identifier}}), 1200);
            } else {
                setError(result.message || 'Password reset failed.');
            }
        } catch (err) {
            setError(err.message || 'Password reset failed.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div
            className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">Reset Password</h2>
                <p className="mt-1 mb-6 text-sm text-slate-400">
                    Enter OTP and set a new password.
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
                            className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-1">OTP</label>
                        <input
                            type="text"
                            value={otp}
                            onChange={(e) => setOtp(e.target.value)}
                            placeholder="6 digit OTP"
                            className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-1">New Password</label>
                        <div className="relative">
                            <input
                                type={showPassword ? 'text' : 'password'}
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 pr-12 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                                required
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white"
                            >
                                {showPassword ? 'Hide' : 'Show'}
                            </button>
                        </div>
                    </div>

                    {error && <p className="text-sm text-red-400">{error}</p>}
                    {successMessage && <p className="text-sm text-green-400">{successMessage}</p>}

                    <button
                        type="submit"
                        disabled={loading || !canSubmit}
                        className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {loading ? 'Resetting...' : 'Reset Password'}
                    </button>
                </form>

                <div className="mt-6 text-center">
                    <button
                        onClick={() => navigate('/forgot-password')}
                        className="text-xs text-slate-500 hover:text-white transition"
                    >
                        Resend OTP
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ResetPasswordPage;
