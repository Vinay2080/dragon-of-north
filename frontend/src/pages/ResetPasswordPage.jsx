import React, {useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import {useToast} from '../hooks/useToast';

const ResetPasswordPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const {toast} = useToast();
    const {identifier, identifierType} = location.state || {};

    const [otp, setOtp] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        const payload = identifierType === 'EMAIL'
            ? {email: identifier, otp, new_password: newPassword}
            : {phone: identifier, otp, new_password: newPassword};

        const result = await apiService.post(API_CONFIG.ENDPOINTS.PASSWORD_RESET_CONFIRM, payload);

        if (apiService.isErrorResponse(result)) {
            toast.error(result.message || 'Password reset failed.');
            setLoading(false);
            return;
        }

        toast.success('Password reset successful. Please log in.');
        navigate('/login', {state: {identifier}});
        setLoading(false);
    };

    if (!identifier) {
        navigate('/forgot-password');
        return null;
    }

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">Reset Password</h2>
                <p className="mt-1 mb-6 text-sm text-slate-400">Enter OTP and your new password</p>
                <form onSubmit={handleSubmit} noValidate className="space-y-4">
                    <input type="text" value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="OTP code" className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none" required/>
                    <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} placeholder="New password" className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none" required/>
                    <button type="submit" disabled={loading || !otp || !newPassword} className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50">{loading ? 'Resetting...' : 'Reset Password'}</button>
                </form>
            </div>
        </div>
    );
};

export default ResetPasswordPage;
