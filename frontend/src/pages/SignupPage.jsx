import React, {useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import RateLimitInfo from '../components/RateLimitInfo';

const SignupPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const {identifier, identifierType} = location.state || {};

    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
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

        const signupResult = await apiService.post(API_CONFIG.ENDPOINTS.SIGNUP, {
            identifier,
            identifier_type: identifierType,
            password,
        });

        if (apiService.isErrorResponse(signupResult)) {
            if (signupResult.type === 'RATE_LIMIT_EXCEEDED') {
                setErrors([`Too many signup attempts. Please wait ${signupResult.retryAfter ?? 60} seconds before trying again.`]);
            } else {
                const validationErrors = signupResult?.data?.validation_error_list
                    ?.filter(e => e.field === 'password')
                    ?.map(e => e.message);
                setErrors(validationErrors?.length ? validationErrors : [signupResult.message || 'Signup failed']);
            }
            setLoading(false);
            return;
        }

        if (signupResult?.api_response_status !== 'success') {
            setErrors([signupResult?.message || 'Something went wrong']);
            setLoading(false);
            return;
        }

        const otpEndpoint = identifierType === 'EMAIL'
            ? API_CONFIG.ENDPOINTS.EMAIL_OTP_REQUEST
            : API_CONFIG.ENDPOINTS.PHONE_OTP_REQUEST;

        const otpPayload = identifierType === 'EMAIL'
            ? {email: identifier, otp_purpose: 'SIGNUP'}
            : {phone: identifier, otp_purpose: 'SIGNUP'};

        const otpResult = await apiService.post(otpEndpoint, otpPayload);

        if (apiService.isErrorResponse(otpResult)) {
            if (otpResult.type === 'RATE_LIMIT_EXCEEDED') {
                setErrors([`Too many OTP requests. Please wait ${otpResult.retryAfter ?? 60} seconds before trying again.`]);
            } else {
                setErrors([otpResult.message || 'Failed to send OTP. Please try again.']);
            }
            setLoading(false);
            return;
        }

        navigate('/otp', {state: {identifier, identifierType}});
        setLoading(false);
    };

    if (!identifier) {
        navigate('/');
        return null;
    }

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">Create Account</h2>

                <p className="mt-1 mb-6 text-sm text-slate-400">
                    Setting up account for <span className="text-blue-400 font-medium">{identifier}</span>
                </p>

                <form onSubmit={handleGetOtp} noValidate>
                    <div className="space-y-4">
                        <div className="relative">
                            <input
                                type={showPassword ? 'text' : 'password'}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="Enter password"
                                className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 pr-12 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                                required
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white transition"
                            >
                                {showPassword ? '🙈' : '👁️'}
                            </button>
                        </div>

                        {errors.length > 0 && (
                            <ul className="mt-3 list-disc pl-5 space-y-1">
                                {errors.map((err, index) => (
                                    <li key={index} className="text-sm text-red-400">{err}</li>
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
            </div>
        </div>
    );
};

export default SignupPage;
