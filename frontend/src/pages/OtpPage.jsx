import React, {useEffect, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import RateLimitInfo from '../components/RateLimitInfo';

const OtpPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const {identifier, identifierType} = location.state || {};

    const [otp, setOtp] = useState(['', '', '', '', '', '']);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [resendLoading, setResendLoading] = useState(false);
    const [timer, setTimer] = useState(() => {
        const savedTimer = localStorage.getItem('otpTimer');
        const savedTime = localStorage.getItem('otpTimerTimestamp');
        if (savedTimer && savedTime) {
            const elapsed = Math.floor((Date.now() - parseInt(savedTime, 10)) / 1000);
            const remaining = parseInt(savedTimer, 10) - elapsed;
            return remaining > 0 ? remaining : 0;
        }
        return 60;
    });

    useEffect(() => {
        localStorage.setItem('otpTimer', timer.toString());
        localStorage.setItem('otpTimerTimestamp', Date.now().toString());
    }, [timer]);

    useEffect(() => {
        let interval;
        if (timer > 0) {
            interval = setInterval(() => {
                setTimer((prev) => (prev > 0 ? prev - 1 : 0));
            }, 1000);
        }
        return () => clearInterval(interval);
    }, [timer]);

    const handleChange = (element, index) => {
        if (Number.isNaN(Number(element.value)) && element.value !== '') return;

        setOtp([...otp.map((d, idx) => (idx === index ? element.value : d))]);

        if (element.nextSibling && element.value) {
            element.nextSibling.focus();
        }
    };

    const handleKeyDown = (e, index) => {
        if (e.key === 'Backspace' && !otp[index] && e.target.previousSibling) {
            e.target.previousSibling.focus();
        }
    };

    const completeSignup = async () => {
        const result = await apiService.post(API_CONFIG.ENDPOINTS.SIGNUP_COMPLETE, {
            identifier,
            identifier_type: identifierType,
        });

        if (apiService.isErrorResponse(result)) {
            setError(result.message || 'Failed to complete registration.');
            return false;
        }

        if (result?.api_response_status === 'success') {
            localStorage.removeItem('otpTimer');
            localStorage.removeItem('otpTimerTimestamp');
            navigate('/login', {state: {identifier}});
            return true;
        }

        setError(result?.message || 'Failed to complete registration.');
        return false;
    };

    const handleVerifyOtp = async (e) => {
        e.preventDefault();
        const otpCode = otp.join('');
        if (otpCode.length !== 6) {
            setError('Please enter all 6 digits.');
            return;
        }

        setLoading(true);
        setError('');

        const endpoint = identifierType === 'EMAIL'
            ? API_CONFIG.ENDPOINTS.EMAIL_OTP_VERIFY
            : API_CONFIG.ENDPOINTS.PHONE_OTP_VERIFY;

        const payload = identifierType === 'EMAIL'
            ? {email: identifier, otp: otpCode, otp_purpose: 'SIGNUP'}
            : {phone: identifier, otp: otpCode, otp_purpose: 'SIGNUP'};

        const verifyResult = await apiService.post(endpoint, payload);

        if (apiService.isErrorResponse(verifyResult)) {
            if (verifyResult.type === 'RATE_LIMIT_EXCEEDED') {
                setError(`Too many verification attempts. Please wait ${verifyResult.retryAfter ?? 60} seconds before trying again.`);
            } else {
                setError(verifyResult.message || 'OTP verification failed.');
            }
            setLoading(false);
            return;
        }

        if (verifyResult?.api_response_status === 'success') {
            await completeSignup();
        } else {
            setError(verifyResult?.message || 'Invalid OTP. Please try again.');
        }

        setLoading(false);
    };

    const handleResendOtp = async () => {
        if (timer > 0) return;

        setResendLoading(true);
        setError('');

        const endpoint = identifierType === 'EMAIL'
            ? API_CONFIG.ENDPOINTS.EMAIL_OTP_REQUEST
            : API_CONFIG.ENDPOINTS.PHONE_OTP_REQUEST;

        const payload = identifierType === 'EMAIL'
            ? {email: identifier, otp_purpose: 'SIGNUP'}
            : {phone: identifier, otp_purpose: 'SIGNUP'};

        const result = await apiService.post(endpoint, payload);

        if (apiService.isErrorResponse(result)) {
            if (result.type === 'RATE_LIMIT_EXCEEDED') {
                setError(`Too many OTP requests. Please wait ${result.retryAfter ?? 60} seconds before trying again.`);
            } else {
                setError(result.message || 'Failed to resend OTP. Please try again.');
            }
            setResendLoading(false);
            return;
        }

        setTimer(60);
        setOtp(['', '', '', '', '', '']);
        setResendLoading(false);
    };

    if (!identifier) {
        navigate('/');
        return null;
    }

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">Verify OTP</h2>

                <p className="mt-1 mb-6 text-sm text-slate-400">
                    Enter the 6-digit code sent to <span className="text-blue-400 font-medium">{identifier}</span>
                </p>

                <form onSubmit={handleVerifyOtp} noValidate>
                    <div className="flex justify-between gap-2 mb-6">
                        {otp.map((data, index) => (
                            <input
                                key={index}
                                type="text"
                                maxLength="1"
                                value={data}
                                onChange={(e) => handleChange(e.target, index)}
                                onKeyDown={(e) => handleKeyDown(e, index)}
                                className="w-12 h-14 text-center text-xl font-bold rounded-lg border border-slate-700 bg-slate-900 text-white focus:border-blue-500 focus:outline-none"
                            />
                        ))}
                    </div>

                    {error && <p className="mb-4 text-sm text-red-400 text-center">{error}</p>}

                    <button
                        type="submit"
                        disabled={loading || otp.join('').length !== 6}
                        className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {loading ? 'Verifying...' : 'Verify & Continue'}
                    </button>

                    <RateLimitInfo/>
                </form>

                <div className="mt-8 text-center">
                    <p className="text-sm text-slate-500">
                        Didn't receive the code?{' '}
                        {timer > 0 ? (
                            <span className="text-slate-400">Resend in {timer}s</span>
                        ) : (
                            <button
                                onClick={handleResendOtp}
                                disabled={resendLoading}
                                className="text-blue-500 hover:text-blue-400 font-medium transition disabled:opacity-50"
                            >
                                {resendLoading ? 'Sending...' : 'Resend OTP'}
                            </button>
                        )}
                    </p>
                </div>
            </div>
        </div>
    );
};

export default OtpPage;
