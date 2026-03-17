import React, {useEffect, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import RateLimitInfo from '../components/RateLimitInfo';
import {useToast} from '../hooks/useToast';
import AuthFlowProgress from '../components/AuthFlowProgress';
import AuthCardLayout from '../components/auth/AuthCardLayout';
import AuthButton from '../components/auth/AuthButton';

const OtpPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const {toast} = useToast();
    const {identifier, identifierType} = location.state || {};

    const [otp, setOtp] = useState(['', '', '', '', '', '']);
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
        if (timer > 0) interval = setInterval(() => setTimer((prev) => (prev > 0 ? prev - 1 : 0)), 1000);
        return () => clearInterval(interval);
    }, [timer]);

    const handleChange = (element, index) => {
        if (Number.isNaN(Number(element.value)) && element.value !== '') return;
        setOtp([...otp.map((d, idx) => (idx === index ? element.value : d))]);
        if (element.nextSibling && element.value) element.nextSibling.focus();
    };

    const handleKeyDown = (e, index) => {
        if (e.key === 'Backspace' && !otp[index] && e.target.previousSibling) e.target.previousSibling.focus();
    };

    const completeSignup = async () => {
        // Finalization API after OTP success: POST /api/v1/auth/identifier/sign-up/complete
        const result = await apiService.post(API_CONFIG.ENDPOINTS.SIGNUP_COMPLETE, {identifier, identifier_type: identifierType});
        if (apiService.isErrorResponse(result)) {
            toast.error(result.message || 'Failed to complete registration.');
            return false;
        }

        if (result?.api_response_status === 'success') {
            localStorage.removeItem('otpTimer');
            localStorage.removeItem('otpTimerTimestamp');
            toast.success('Account verification completed. Please log in.');
            navigate('/login', {state: {identifier}});
            return true;
        }

        toast.error(result?.message || 'Failed to complete registration.');
        return false;
    };

    const handleVerifyOtp = async (e) => {
        e.preventDefault();
        const otpCode = otp.join('');
        if (otpCode.length !== 6) {
            toast.warning('Please enter all 6 digits.');
            return;
        }

        setLoading(true);
        // Verification API: /api/v1/otp/email/verify or /api/v1/otp/phone/verify (otp_purpose=SIGNUP)
        const endpoint = identifierType === 'EMAIL' ? API_CONFIG.ENDPOINTS.EMAIL_OTP_VERIFY : API_CONFIG.ENDPOINTS.PHONE_OTP_VERIFY;
        const payload = identifierType === 'EMAIL' ? {email: identifier, otp: otpCode, otp_purpose: 'SIGNUP'} : {phone: identifier, otp: otpCode, otp_purpose: 'SIGNUP'};
        const verifyResult = await apiService.post(endpoint, payload);

        if (apiService.isErrorResponse(verifyResult)) {
            toast.error(verifyResult.message || 'OTP verification failed.');
            setLoading(false);
            return;
        }

        if (verifyResult?.api_response_status === 'success') {
            // Backend contract: OTP verify success gates sign-up completion call.
            await completeSignup();
        } else {
            toast.error(verifyResult?.message || 'Invalid OTP. Please try again.');
        }

        setLoading(false);
    };

    const handleResendOtp = async () => {
        if (timer > 0) return;
        setResendLoading(true);

        // Resend uses the same OTP request APIs as initial signup OTP issuance.
        const endpoint = identifierType === 'EMAIL' ? API_CONFIG.ENDPOINTS.EMAIL_OTP_REQUEST : API_CONFIG.ENDPOINTS.PHONE_OTP_REQUEST;
        const payload = identifierType === 'EMAIL' ? {email: identifier, otp_purpose: 'SIGNUP'} : {phone: identifier, otp_purpose: 'SIGNUP'};
        const result = await apiService.post(endpoint, payload);

        if (apiService.isErrorResponse(result)) {
            toast.error(result.message || 'Failed to resend OTP.');
            setResendLoading(false);
            return;
        }

        setTimer(60);
        setOtp(['', '', '', '', '', '']);
        toast.success('A new OTP has been sent.');
        setResendLoading(false);
    };

    if (!identifier) {
        navigate('/');
        return null;
    }

    return (
        <AuthCardLayout
            title="Verify OTP"
            subtitle={<span>Enter the 6-digit code sent to <span
                className="font-medium" style={{color: 'var(--don-accent-text)'}}>{identifier}</span></span>}
        >
            <AuthFlowProgress currentStep="otp"/>
            <form onSubmit={handleVerifyOtp} noValidate>
                <div className="mb-6 flex justify-between gap-2">
                    {otp.map((data, index) => (
                        <input key={index} type="text" maxLength="1" value={data}
                               onChange={(e) => handleChange(e.target, index)}
                               onKeyDown={(e) => handleKeyDown(e, index)}
                               className="auth-otp-input"
                               aria-label={`OTP digit ${index + 1}`}/>
                    ))}
                </div>
                <AuthButton type="submit"
                            disabled={loading || otp.join('').length !== 6}>{loading ? 'Verifying...' : 'Verify & Continue'}</AuthButton>
                <RateLimitInfo/>
            </form>
            <div className="mt-8 text-center">
                <p className="auth-helper text-sm">Didn't receive the code? {timer > 0 ?
                    <span style={{color: 'var(--don-text-muted)'}}>Resend in {timer}s</span> :
                    <button onClick={handleResendOtp} disabled={resendLoading}
                            className="auth-link font-medium transition disabled:opacity-50">{resendLoading ? 'Sending...' : 'Resend OTP'}</button>}</p>
            </div>
        </AuthCardLayout>
    );
};

export default OtpPage;
