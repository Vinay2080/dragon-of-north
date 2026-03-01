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
        await apiService.post(API_CONFIG.ENDPOINTS.PASSWORD_RESET_REQUEST, {
            identifier: processedIdentifier,
            identifier_type: identifierType,
        });

        toast.info('If an account exists, you’ll receive an email or OTP shortly.');
        navigate('/reset-password', {state: {identifier: processedIdentifier, identifierType}});
        setLoading(false);
    };

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">Forgot Password</h2>
                <p className="mt-1 mb-6 text-sm text-slate-400">Enter your email or phone to receive reset OTP</p>
                <form onSubmit={handleSubmit} noValidate>
                    <input type="text" value={identifier} onChange={(e) => setIdentifier(e.target.value)} placeholder="Email or phone number" className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none" required/>
                    <button type="submit" disabled={loading || !identifier.trim()} className="mt-5 w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50">{loading ? 'Sending OTP...' : 'Send OTP'}</button>
                </form>
            </div>
        </div>
    );
};

export default ForgotPasswordRequestPage;
