import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import {useToast} from '../hooks/useToast';
import {useAuth} from '../context/authUtils';
import AuthCardLayout from '../components/auth/AuthCardLayout';
import AuthInput from '../components/auth/AuthInput';
import AuthButton from '../components/auth/AuthButton';

const ForgotPasswordRequestPage = () => {
    const navigate = useNavigate();
    const {toast} = useToast();
    const {isAuthenticated, isLoading} = useAuth();
    const [identifier, setIdentifier] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!isLoading && isAuthenticated) {
            navigate('/sessions', {replace: true});
        }
    }, [isAuthenticated, isLoading, navigate]);

    const detectIdentifierType = (value) => (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) ? 'EMAIL' : 'PHONE');
    const normalizePhone = (value) => value.replace(/\D/g, '');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        const identifierType = detectIdentifierType(identifier.trim());
        const processedIdentifier = identifierType === 'PHONE' ? normalizePhone(identifier) : identifier.trim();
        const result = await apiService.post(API_CONFIG.ENDPOINTS.PASSWORD_RESET_REQUEST, {
            identifier: processedIdentifier,
            identifier_type: identifierType,
        });

        if (apiService.isErrorResponse(result) || result?.api_response_status !== 'success') {
            toast.error(result?.message || 'Unable to request password reset. Please try again.');
            setLoading(false);
            return;
        }

        toast.info('If an account exists, you’ll receive an email or OTP shortly.');
        navigate('/reset-password', {state: {identifier: processedIdentifier, identifierType}});
        setLoading(false);
    };

    return (
        <AuthCardLayout
            title="Forgot password"
            subtitle="Enter your email or phone to receive reset OTP"
        >
            <form onSubmit={handleSubmit} noValidate className="space-y-4">
                <AuthInput type="text" value={identifier} onChange={(e) => setIdentifier(e.target.value)}
                           placeholder="Email or phone number" required/>
                <AuthButton type="submit"
                            disabled={loading || !identifier.trim()}>{loading ? 'Sending OTP...' : 'Send OTP'}</AuthButton>
            </form>
        </AuthCardLayout>
    );
};

export default ForgotPasswordRequestPage;
