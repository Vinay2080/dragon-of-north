import React, {useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import {useToast} from '../hooks/useToast';
import AuthCardLayout from '../components/auth/AuthCardLayout';
import AuthInput from '../components/auth/AuthInput';
import PasswordInput from '../components/auth/PasswordInput';
import AuthButton from '../components/auth/AuthButton';

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
        <AuthCardLayout
            title="Reset password"
            subtitle="Enter OTP and your new password"
        >
            <form onSubmit={handleSubmit} noValidate className="space-y-4">
                <AuthInput type="text" value={otp}
                           onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                           placeholder="OTP code" required/>
                <PasswordInput
                    name="newPassword"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="New password"
                    autoComplete="new-password"
                    required
                />
                <AuthButton type="submit"
                            disabled={loading || !otp || !newPassword}>{loading ? 'Resetting...' : 'Reset Password'}</AuthButton>
            </form>
        </AuthCardLayout>
    );
};

export default ResetPasswordPage;
