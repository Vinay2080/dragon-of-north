import React, {useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import RateLimitInfo from '../components/RateLimitInfo';

const SignupPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const { identifier, identifierType } = location.state || {};

    const [password, setPassword] = useState('');
    const [errors, setErrors] = useState([]);
    const [loading, setLoading] = useState(false);

    const handleGetOtp = async (e) => {
        e.preventDefault();
        setErrors([]);

        if (!password) {
            setErrors(['Please enter a password.']);
            return;
        }

        setLoading(true);

        try {
            const result = await apiService.post(API_CONFIG.ENDPOINTS.SIGNUP);

            if (result.api_response_status === 'success') {
                // Determine OTP endpoint based on identifierType
                const otpEndpoint = identifierType === 'EMAIL'
                    ? API_CONFIG.ENDPOINTS.EMAIL_OTP_REQUEST
                    : API_CONFIG.ENDPOINTS.PHONE_OTP_REQUEST;

                const otpPayload = identifierType === 'EMAIL'
                    ? { email: identifier, otp_purpose: 'SIGNUP' }
                    : { phone: identifier, otp_purpose: 'SIGNUP' };

                try {
                    await apiService.post(otpEndpoint, otpPayload);
                    navigate('/otp', { state: { identifier, identifierType } });
                } catch (otpErr) {
                    if (otpErr.type === 'RATE_LIMIT_EXCEEDED') {
                        setErrors([`Too many OTP requests. Please wait ${otpErr.retryAfter} seconds before trying again.`]);
                    } else {
                        setErrors([otpErr.message || 'Failed to send OTP. Please try again.']);
                    }
                    console.error('OTP API Error:', otpErr);
                }
            } else {
                setErrors([result.message || 'Something went wrong']);
            }
        } catch (err) {
            if (err.type === 'RATE_LIMIT_EXCEEDED') {
                setErrors([`Too many signup attempts. Please wait ${err.retryAfter} seconds before trying again.`]);
            } else if (err.type === 'API_ERROR') {
                if (err.data && err.data.data && err.data.data.validationErrorList) {
                    const validationErrors = err.data.data.validationErrorList
                        .filter(e => e.field === 'password')
                        .map(e => e.message);
                    setErrors(validationErrors.length > 0 ? validationErrors : [err.message || 'Validation failed']);
                } else {
                    setErrors([err.message || 'Something went wrong']);
                }
            } else {
                setErrors(['Failed to connect to the server. Please try again later.']);
            }
            console.error('API Error:', err);
        } finally {
            setLoading(false);
        }
    };

    if (!identifier) {
        navigate('/');
        return null;
    }

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">
                    Create Account
                </h2>

                <p className="mt-1 mb-6 text-sm text-slate-400">
                    Setting up account for <span className="text-blue-400 font-medium">{identifier}</span>
                </p>

                <form onSubmit={handleGetOtp} noValidate>
                    <div className="space-y-4">
                        <div>
                            <input
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="Enter password"
                                className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                                required
                            />
                        </div>

                        {errors.length > 0 && (
                            <ul className="mt-3 list-disc pl-5 space-y-1">
                                {errors.map((err, index) => (
                                    <li key={index} className="text-sm text-red-400">
                                        {err}
                                    </li>
                                ))}
                            </ul>
                        )}

                        <button
                            type="submit"
                            disabled={loading || !password}
                            className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {loading ? 'Processing...' : 'Get OTP'}
                        </button>
                    </div>

                    <RateLimitInfo/>
                </form>

                <div className="mt-6 flex items-center justify-between">
                    <button
                        onClick={() => navigate(-1)}
                        className="text-xs text-slate-500 hover:text-white transition"
                    >
                        &larr; Back
                    </button>
                </div>
            </div>
        </div>
    );
};

export default SignupPage;
