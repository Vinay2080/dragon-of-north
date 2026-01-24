import React, {useEffect, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';

const OtpPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const { identifier, identifierType } = location.state || {};

    const [otp, setOtp] = useState(['', '', '', '', '', '']);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [resendLoading, setResendLoading] = useState(false);
    const [timer, setTimer] = useState(() => {
        const savedTimer = localStorage.getItem('otpTimer');
        const savedTime = localStorage.getItem('otpTimerTimestamp');
        if (savedTimer && savedTime) {
            const elapsed = Math.floor((Date.now() - parseInt(savedTime)) / 1000);
            const remaining = parseInt(savedTimer) - elapsed;
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
                setTimer((prev) => {
                    const next = prev - 1;
                    return next >= 0 ? next : 0;
                });
            }, 1000);
        }
        return () => clearInterval(interval);
    }, [timer]);

    const handleChange = (element, index) => {
        if (isNaN(element.value)) return false;

        setOtp([...otp.map((d, idx) => (idx === index ? element.value : d))]);

        // Focus next input
        if (element.nextSibling && element.value) {
            element.nextSibling.focus();
        }
    };

    const handleKeyDown = (e, index) => {
        if (e.key === 'Backspace') {
            if (!otp[index] && e.target.previousSibling) {
                e.target.previousSibling.focus();
            }
        }
    };

    const completeSignup = async (otpCode) => {
        try {
            const response = await fetch(`${API_CONFIG.BASE_URL}/api/v1/auth/identifier/sign-up/complete`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    identifier,
                    identifier_type: identifierType,
                    otp: otpCode
                }),
            });

            const result = await response.json();

            if (response.ok && result.api_response_status === 'success') {
                localStorage.removeItem('otpTimer');
                localStorage.removeItem('otpTimerTimestamp');
                navigate('/login', {state: {identifier}});
            } else {
                setError(result.message || 'Failed to complete registration.');
            }
        } catch (err) {
            setError('Failed to complete registration. Please try again.');
            console.error('Complete Signup Error:', err);
        }
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
            ? `${API_CONFIG.BASE_URL}/api/v1/otp/email/verify`
            : `${API_CONFIG.BASE_URL}/api/v1/otp/phone/verify`;

        const payload = identifierType === 'EMAIL'
            ? { email: identifier, otp: otpCode, otp_purpose: 'SIGNUP' }
            : { phone: identifier, otp: otpCode, otp_purpose: 'SIGNUP' };

        try {
            // First verify the OTP
            const verifyResponse = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(payload),
            });

            const verifyResult = await verifyResponse.json();

            if (verifyResponse.ok) {
                // If OTP is verified, complete the signup
                await completeSignup(otpCode);
            } else {
                setError(verifyResult.message || (verifyResult.data && verifyResult.data.message) || 'Invalid OTP. Please try again.');
            }
        } catch (err) {
            setError('Failed to connect to the server. Please try again later.');
            console.error('Verify OTP Error:', err);
        } finally {
            setLoading(false);
        }
    };


    const handleResendOtp = async () => {
        if (timer > 0) return;
        
        setResendLoading(true);
        setError('');

        const endpoint = identifierType === 'EMAIL'
            ? `${API_CONFIG.BASE_URL}/api/v1/otp/email/request`
            : `${API_CONFIG.BASE_URL}/api/v1/otp/phone/request`;
        
        const payload = identifierType === 'EMAIL'
            ? { email: identifier, otp_purpose: 'SIGNUP' }
            : { phone: identifier, otp_purpose: 'SIGNUP' };

        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(payload),
            });

            if (response.ok) {
                setTimer(60);
                setOtp(['', '', '', '', '', '']);
            } else {
                const result = await response.json();
                setError(result.message || 'Failed to resend OTP.');
            }
        } catch (err) {
            setError('Failed to resend OTP. Please try again.');
            console.error('Resend OTP Error:', err);
        } finally {
            setResendLoading(false);
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
                    Verify OTP
                </h2>

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

                    {error && (
                        <p className="mb-4 text-sm text-red-400 text-center">
                            {error}
                        </p>
                    )}

                    <button
                        type="submit"
                        disabled={loading || otp.join('').length !== 6}
                        className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {loading ? 'Verifying...' : 'Verify & Continue'}
                    </button>
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

                <div className="mt-6">
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

export default OtpPage;