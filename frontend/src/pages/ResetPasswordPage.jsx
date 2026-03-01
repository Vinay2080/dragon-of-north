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

        const payload = {
            identifier,
            identifier_type: identifierType,
            otp,
            new_password: newPassword,
        };

        const result = await apiService.post(API_CONFIG.ENDPOINTS.PASSWORD_RESET_CONFIRM, payload);

        if (apiService.isErrorResponse(result)) {
            toast.error(result.message || 'Password reset failed.');
            setLoading(false);
            return;
        }

        if (result?.api_response_status !== 'success') {
            toast.error(result?.message || 'Password reset failed.');
            setLoading(false);
            return;
        }

        toast.success('Password reset successful. Please log in.');
        navigate('/login', {state: {identifier}});
        setLoading(false);
    };

    if (!identifier || !identifierType) {
        navigate('/forgot-password');
        return null;
    }

    return (
        <div className="auth-shell">
            <div className="auth-card">
                <h2 className="auth-title">Reset Password</h2>
                <p className="auth-subtitle mb-6">Enter OTP and your new password</p>
                <form onSubmit={handleSubmit} noValidate className="space-y-4">
                    <input type="text" value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="OTP code" className="auth-input text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none" required/>
                    <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} placeholder="New password" className="auth-input text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none" required/>
                    <button type="submit" disabled={loading || !otp || !newPassword} className="btn-primary text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50">{loading ? 'Resetting...' : 'Reset Password'}</button>
                </form>
            </div>
        </div>
    );
};

export default ResetPasswordPage;
