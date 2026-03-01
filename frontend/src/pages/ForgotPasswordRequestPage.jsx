import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import {useToast} from '../hooks/useToast';
import {useAuth} from '../context/authUtils';

const ForgotPasswordRequestPage = () => {
    const navigate = useNavigate();
    const {toast} = useToast();
    const {isAuthenticated, isLoading} = useAuth();
    const [identifier, setIdentifier] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!isLoading && isAuthenticated) {
            navigate('/dashboard', {replace: true});
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
        <div className="auth-shell">
            <div className="auth-card">
                <h2 className="auth-title">Forgot Password</h2>
                <p className="auth-subtitle mb-6">Enter your email or phone to receive reset OTP</p>
                <form onSubmit={handleSubmit} noValidate>
                    <input type="text" value={identifier} onChange={(e) => setIdentifier(e.target.value)} placeholder="Email or phone number" className="auth-input text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none" required/>
                    <button type="submit" disabled={loading || !identifier.trim()} className="mt-5 btn-primary text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50">{loading ? 'Sending OTP...' : 'Send OTP'}</button>
                </form>
            </div>
        </div>
    );
};

export default ForgotPasswordRequestPage;
