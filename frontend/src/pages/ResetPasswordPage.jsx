import React, {useEffect, useState} from 'react';
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
    const [confirmPassword, setConfirmPassword] = useState('');
    const [loading, setLoading] = useState(false);

    const [resendLoading, setResendLoading] = useState(false);
    const [resendTimer, setResendTimer] = useState(60);
    const canResend = resendTimer <= 0;

    const isConfirmDisabled = !newPassword;
    const passwordsMatch = newPassword === confirmPassword;
    const showPasswordMismatchError = !isConfirmDisabled && Boolean(confirmPassword) && !passwordsMatch;

    // Real-time password requirements (derived from current password value — NOT stored in state)
    const validations = {
        length: newPassword.length >= 8,
        uppercase: /[A-Z]/.test(newPassword),
        lowercase: /[a-z]/.test(newPassword),
        number: /[0-9]/.test(newPassword),
        special: /[^A-Za-z0-9]/.test(newPassword),
    };

    const isPasswordValid = Object.values(validations).every(Boolean);
    const showPasswordRequirementsError = Boolean(newPassword) && !isPasswordValid;

    useEffect(() => {
        if (resendTimer <= 0) return;
        const interval = setInterval(() => {
            setResendTimer((prev) => Math.max(0, prev - 1));
        }, 1000);
        return () => clearInterval(interval);
    }, [resendTimer]);

    // Optional UX: if the user changes the new password after typing confirm password,
    // clear the confirmation so they re-confirm the latest value.
    useEffect(() => {
        setConfirmPassword('');
    }, [newPassword]);

    const handleResendOtp = async () => {
        if (resendLoading || !canResend) return;

        setResendLoading(true);

        const endpoint = identifierType === 'EMAIL'
            ? API_CONFIG.ENDPOINTS.EMAIL_OTP_REQUEST
            : API_CONFIG.ENDPOINTS.PHONE_OTP_REQUEST;
        const payload = identifierType === 'EMAIL'
            ? {email: identifier, otp_purpose: 'PASSWORD_RESET'}
            : {phone: identifier, otp_purpose: 'PASSWORD_RESET'};

        const result = await apiService.post(endpoint, payload);

        if (apiService.isErrorResponse(result) || result?.api_response_status !== 'success') {
            toast.error(result?.message || 'Failed to resend OTP.');
            setResendLoading(false);
            return;
        }

        setResendTimer(60);
        setOtp('');
        toast.success('A new OTP has been sent.');
        setResendLoading(false);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!passwordsMatch) {
            toast.warning('Passwords do not match');
            return;
        }

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
            <form onSubmit={handleSubmit} noValidate className="auth-form-stack">
                <AuthInput type="text" value={otp}
                           onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                           placeholder="OTP code" required/>
                <p className="-mt-1 text-center text-sm auth-helper">
                    Didn't receive the code?{' '}
                    {canResend ? (
                        <button
                            type="button"
                            onClick={handleResendOtp}
                            disabled={resendLoading}
                            className="auth-link font-medium transition disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {resendLoading ? 'Sending...' : 'Resend OTP'}
                        </button>
                    ) : (
                        <span style={{color: 'var(--don-text-muted)'}}>Resend in {resendTimer}s</span>
                    )}
                </p>
                <PasswordInput
                    name="newPassword"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="New password"
                    autoComplete="new-password"
                    required
                />

                {/* Password requirements: ALWAYS visible and placed directly below the new password input */}
                <div
                    className="mt-2 rounded-lg border bg-slate-100 p-3 text-slate-800 dark:border-slate-700/60 dark:bg-slate-900/60 dark:text-slate-200"
                    aria-live="polite"
                >
                    <ul className="space-y-1 text-sm text-slate-700 dark:text-slate-400">
                        <li className="flex items-center gap-2">
                            <span
                                className={
                                    validations.length
                                        ? 'text-green-600 dark:text-green-400'
                                        : 'text-slate-500 dark:text-slate-400'
                                }
                                aria-hidden="true"
                            >
                                {validations.length ? '✔' : '✖'}
                            </span>
                            <span className={validations.length ? 'text-slate-900 dark:text-slate-200' : ''}>
                                At least 8 characters
                            </span>
                        </li>
                        <li className="flex items-center gap-2">
                            <span
                                className={
                                    validations.uppercase
                                        ? 'text-green-600 dark:text-green-400'
                                        : 'text-slate-500 dark:text-slate-400'
                                }
                                aria-hidden="true"
                            >
                                {validations.uppercase ? '✔' : '✖'}
                            </span>
                            <span className={validations.uppercase ? 'text-slate-900 dark:text-slate-200' : ''}>
                                At least one uppercase letter (A-Z)
                            </span>
                        </li>
                        <li className="flex items-center gap-2">
                            <span
                                className={
                                    validations.lowercase
                                        ? 'text-green-600 dark:text-green-400'
                                        : 'text-slate-500 dark:text-slate-400'
                                }
                                aria-hidden="true"
                            >
                                {validations.lowercase ? '✔' : '✖'}
                            </span>
                            <span className={validations.lowercase ? 'text-slate-900 dark:text-slate-200' : ''}>
                                At least one lowercase letter (a-z)
                            </span>
                        </li>
                        <li className="flex items-center gap-2">
                            <span
                                className={
                                    validations.number
                                        ? 'text-green-600 dark:text-green-400'
                                        : 'text-slate-500 dark:text-slate-400'
                                }
                                aria-hidden="true"
                            >
                                {validations.number ? '✔' : '✖'}
                            </span>
                            <span className={validations.number ? 'text-slate-900 dark:text-slate-200' : ''}>
                                At least one number (0-9)
                            </span>
                        </li>
                        <li className="flex items-center gap-2">
                            <span
                                className={
                                    validations.special
                                        ? 'text-green-600 dark:text-green-400'
                                        : 'text-slate-500 dark:text-slate-400'
                                }
                                aria-hidden="true"
                            >
                                {validations.special ? '✔' : '✖'}
                            </span>
                            <span className={validations.special ? 'text-slate-900 dark:text-slate-200' : ''}>
                                At least one special character (!@#$%^&*)
                            </span>
                        </li>
                    </ul>

                    {/* Reserve space to avoid layout shift when showing/hiding the message */}
                    <div className="min-h-[20px] pt-2">
                        {showPasswordRequirementsError ? (
                            <p className="text-center text-sm text-red-600 dark:text-red-400">Password does not meet
                                requirements</p>
                        ) : null}
                    </div>
                </div>

                <PasswordInput
                    name="confirmPassword"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Confirm password"
                    autoComplete="new-password"
                    required
                    disabled={isConfirmDisabled}
                    hasError={showPasswordMismatchError}
                />

                {isConfirmDisabled ? (
                    <p className="-mt-2 text-center text-xs auth-helper" style={{color: 'var(--don-text-muted)'}}>
                        Enter password first
                    </p>
                ) : null}

                {/* Reserve space to avoid layout shift when showing/hiding the error */}
                <div className="min-h-[20px] -mt-1">
                    {showPasswordMismatchError ? (
                        <p className="text-center text-sm text-red-500">Passwords do not match</p>
                    ) : null}
                </div>
                <AuthButton type="submit"
                            disabled={
                                loading ||
                                !otp ||
                                !newPassword ||
                                !confirmPassword ||
                                showPasswordMismatchError
                            }>{loading ? 'Resetting...' : 'Reset Password'}</AuthButton>
            </form>
        </AuthCardLayout>
    );
};

export default ResetPasswordPage;
