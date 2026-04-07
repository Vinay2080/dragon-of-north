import React, {useState} from 'react';
import AuthButton from '../auth/AuthButton';
import PasswordInput from '../auth/PasswordInput';
import ValidationError from '../Validation/ValidationError';
import {apiService} from '../../services/apiService';
import {API_CONFIG} from '../../config';
import {useToast} from '../../hooks/useToast';
import {useAuth} from '../../context/authUtils';
import DeleteAccountSection from './DeleteAccountSection';

const EMPTY_PASSWORD_STATE = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
};

const EMPTY_PASSWORD_ERRORS = {
    currentPassword: [],
    newPassword: [],
    confirmPassword: [],
};

const PASSWORD_COMPLEXITY_MESSAGE = 'Password must be at least 8 characters with letters and numbers';

const SecuritySection = ({authProvider}) => {
    const {toast} = useToast();
    const {user} = useAuth();
    const [passwordForm, setPasswordForm] = useState(EMPTY_PASSWORD_STATE);
    const [passwordErrors, setPasswordErrors] = useState(EMPTY_PASSWORD_ERRORS);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const normalizedAuthProvider = String(authProvider || '').toUpperCase();
    const canChangePassword = !normalizedAuthProvider || normalizedAuthProvider === 'LOCAL';

    const resetPasswordForm = () => {
        setPasswordForm(EMPTY_PASSWORD_STATE);
        setPasswordErrors(EMPTY_PASSWORD_ERRORS);
    };

    const clearPasswordFieldErrors = (field) => {
        setPasswordErrors((prev) => {
            const nextErrors = {
                ...prev,
                [field]: [],
            };

            if (field === 'newPassword' || field === 'confirmPassword') {
                nextErrors.confirmPassword = [];
                if (field === 'newPassword') {
                    nextErrors.newPassword = [];
                }
            }

            if (field === 'currentPassword') {
                nextErrors.currentPassword = [];
            }

            return nextErrors;
        });
    };

    const updatePasswordField = (field, value) => {
        setPasswordForm((prev) => ({
            ...prev,
            [field]: value,
        }));
        clearPasswordFieldErrors(field);
    };

    const setInlineErrorByCode = (result) => {
        const errorCode = result?.errorCode || result?.raw?.code;
        const backendMessage = result.backendMessage || result.message;

        if (errorCode === 'PASSWORD_MISMATCH') {
            setPasswordErrors((prev) => ({
                ...prev,
                confirmPassword: [backendMessage || 'New password and confirm password do not match'],
            }));
            return true;
        }

        if (errorCode === 'INVALID_CURRENT_PASSWORD') {
            setPasswordErrors((prev) => ({
                ...prev,
                currentPassword: [backendMessage || 'Current password is incorrect'],
            }));
            return true;
        }

        if (errorCode === 'WEAK_PASSWORD') {
            setPasswordErrors((prev) => ({
                ...prev,
                newPassword: [backendMessage || PASSWORD_COMPLEXITY_MESSAGE],
            }));
            return true;
        }

        if (errorCode === 'SAME_PASSWORD') {
            setPasswordErrors((prev) => ({
                ...prev,
                newPassword: [backendMessage || 'New password must be different from current password'],
            }));
            return true;
        }

        if (result?.fieldErrors?.length) {
            const nextErrors = {
                currentPassword: [],
                newPassword: [],
                confirmPassword: [],
            };

            result.fieldErrors.forEach((error) => {
                if (error.field === 'newPassword') {
                    nextErrors.newPassword.push(error.message || PASSWORD_COMPLEXITY_MESSAGE);
                }
            });

            if (nextErrors.currentPassword.length || nextErrors.newPassword.length || nextErrors.confirmPassword.length) {
                setPasswordErrors(nextErrors);
                return true;
            }
        }

        return false;
    };

    const validatePasswordForm = () => {
        const nextErrors = {
            currentPassword: [],
            newPassword: [],
            confirmPassword: [],
        };

        if (!passwordForm.currentPassword) {
            nextErrors.currentPassword.push('Current password is required.');
        }

        if (!passwordForm.newPassword) {
            nextErrors.newPassword.push('New password is required.');
        } else if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(passwordForm.newPassword)) {
            nextErrors.newPassword.push(PASSWORD_COMPLEXITY_MESSAGE);
        }

        if (!passwordForm.confirmPassword) {
            nextErrors.confirmPassword.push('Please confirm your new password.');
        }

        if (passwordForm.newPassword && passwordForm.confirmPassword && passwordForm.newPassword !== passwordForm.confirmPassword) {
            nextErrors.confirmPassword.push('New password and confirm password do not match');
        }

        if (passwordForm.currentPassword && passwordForm.newPassword && passwordForm.currentPassword === passwordForm.newPassword) {
            nextErrors.newPassword.push('New password must be different from current password');
        }

        setPasswordErrors(nextErrors);
        return Object.values(nextErrors).every((errors) => errors.length === 0);
    };

    const submitPassword = async (event) => {
        event.preventDefault();
        if (isSubmitting || !canChangePassword) {
            return;
        }

        if (!validatePasswordForm()) {
            return;
        }

        setIsSubmitting(true);

        try {
            const result = await apiService.post(API_CONFIG.ENDPOINTS.PASSWORD_CHANGE, {
                oldPassword: passwordForm.currentPassword,
                newPassword: passwordForm.newPassword,
            });

            if (apiService.isErrorResponse(result)) {
                const consumed = setInlineErrorByCode(result);
                if (!consumed) {
                    if (result?.status === 401 || result?.status === 403) {
                        toast.error('Your session has expired. Please log in again to change password.');
                    } else {
                        toast.error(result.backendMessage || result.message || 'Unable to update password.');
                    }
                }
                return;
            }

            resetPasswordForm();
            toast.success('Password updated successfully.');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <section
            className="group rounded-3xl border border-slate-200/80 bg-[rgba(255,255,255,0.88)] p-6 shadow-[0_20px_40px_rgba(15,23,42,0.10)] transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_28px_48px_rgba(59,130,246,0.12)] dark:border-slate-800/80 dark:bg-[rgba(11,18,32,0.94)]">
            <div className="mb-6 space-y-3">
                <div className="flex items-start gap-3">
                    <div className={`mt-0.5 flex h-10 w-10 items-center justify-center rounded-lg border shadow-sm ${
                        canChangePassword
                            ? 'border-emerald-300/50 bg-gradient-to-br from-emerald-500/20 to-teal-500/15 text-emerald-700 dark:border-emerald-400/30 dark:from-emerald-500/15 dark:to-teal-500/10 dark:text-emerald-300'
                            : 'border-blue-300/50 bg-gradient-to-br from-blue-500/20 to-cyan-500/15 text-blue-700 dark:border-blue-400/30 dark:from-blue-500/15 dark:to-cyan-500/10 dark:text-blue-300'
                    }`}>
                        {canChangePassword ? (
                            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                            </svg>
                        ) : (
                            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        )}
                    </div>
                    <div>
                        <h2 className="text-lg font-bold text-slate-900 dark:text-slate-50">Security control panel</h2>
                        <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">
                            {normalizedAuthProvider === 'GOOGLE' ? 'Signed in via Google' : 'Signed in with local credentials'}
                        </p>
                    </div>
                </div>
                <div className={`rounded-2xl border px-4 py-3 text-sm shadow-sm transition-all ${
                    canChangePassword
                        ? 'border-emerald-200/70 bg-gradient-to-br from-emerald-50/80 to-teal-50/60 text-slate-700 dark:border-emerald-500/25 dark:bg-gradient-to-br dark:from-emerald-500/10 dark:to-teal-500/8 dark:text-slate-200'
                        : 'border-blue-200/70 bg-gradient-to-br from-blue-50/80 to-cyan-50/60 text-slate-700 dark:border-blue-500/25 dark:bg-gradient-to-br dark:from-blue-500/10 dark:to-cyan-500/8 dark:text-slate-200'
                }`}>
                    <p className="font-medium">{canChangePassword ? 'Password is managed in this account.' : 'Password managed by Google.'}</p>
                    <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">Enable MFA (coming soon) · Add backup authentication (coming soon)</p>
                </div>
            </div>

            {!canChangePassword ? null : (
                <form className="space-y-4" onSubmit={submitPassword}>
                {/* Improves password-manager accessibility by providing username context. */}
                <input
                    type="text"
                    name="username"
                    autoComplete="username"
                    defaultValue={user?.email || user?.identifier || user?.username || ''}
                    tabIndex={-1}
                    aria-hidden="true"
                    readOnly
                    style={{position: 'absolute', left: '-9999px', width: '1px', height: '1px', opacity: 0}}
                />

                <div>
                    <label className="block text-xs font-semibold uppercase tracking-[0.15em] text-slate-700 dark:text-slate-300 mb-2">Current password</label>
                    <PasswordInput
                        id="currentPassword"
                        name="currentPassword"
                        value={passwordForm.currentPassword}
                        onChange={(event) => updatePasswordField('currentPassword', event.target.value)}
                        hasError={Boolean(passwordErrors.currentPassword.length)}
                        className="border-slate-200/70 bg-white/85 focus:border-teal-500 focus:ring-4 focus:ring-teal-500/20 dark:border-slate-700/70 dark:bg-slate-950/50 dark:focus:border-teal-400 dark:focus:ring-teal-400/15 rounded-lg transition-colors"
                        placeholder="Current password"
                        autoComplete="current-password"
                    />
                    <ValidationError errors={passwordErrors.currentPassword}/>
                </div>

                <div>
                    <label className="block text-xs font-semibold uppercase tracking-[0.15em] text-slate-700 dark:text-slate-300 mb-2">New password</label>
                    <PasswordInput
                        id="newPassword"
                        name="newPassword"
                        value={passwordForm.newPassword}
                        onChange={(event) => updatePasswordField('newPassword', event.target.value)}
                        hasError={Boolean(passwordErrors.newPassword.length)}
                        className="border-slate-200/70 bg-white/85 focus:border-teal-500 focus:ring-4 focus:ring-teal-500/20 dark:border-slate-700/70 dark:bg-slate-950/50 dark:focus:border-teal-400 dark:focus:ring-teal-400/15 rounded-lg transition-colors"
                        placeholder="New password"
                        autoComplete="new-password"
                    />
                    <ValidationError errors={passwordErrors.newPassword}/>
                </div>

                <div>
                    <label className="block text-xs font-semibold uppercase tracking-[0.15em] text-slate-700 dark:text-slate-300 mb-2">Confirm password</label>
                    <PasswordInput
                        id="confirmPassword"
                        name="confirmPassword"
                        value={passwordForm.confirmPassword}
                        onChange={(event) => updatePasswordField('confirmPassword', event.target.value)}
                        hasError={Boolean(passwordErrors.confirmPassword.length)}
                        className="border-slate-200/70 bg-white/85 focus:border-teal-500 focus:ring-4 focus:ring-teal-500/20 dark:border-slate-700/70 dark:bg-slate-950/50 dark:focus:border-teal-400 dark:focus:ring-teal-400/15 rounded-lg transition-colors"
                        placeholder="Confirm password"
                        autoComplete="new-password"
                    />
                    <ValidationError errors={passwordErrors.confirmPassword}/>
                </div>

                <div className="pt-2">
                    <AuthButton
                        type="submit"
                        disabled={isSubmitting}
                        className="h-10 rounded-lg border-0 bg-gradient-to-br from-teal-500 to-teal-600 px-4 text-sm font-semibold text-white shadow-[0_12px_24px_rgba(20,184,166,0.28)] transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_16px_32px_rgba(20,184,166,0.36)] focus:outline-none focus:ring-2 focus:ring-teal-400/40 disabled:cursor-not-allowed disabled:opacity-50 dark:from-teal-600 dark:to-teal-700"
                    >
                    {isSubmitting ? (
                        <span className="btn-loading-indicator">
                            <span className="spinner spinner-sm"></span>
                            <span>Updating password...</span>
                        </span>
                    ) : 'Update password'}
                </AuthButton>
                </div>
                </form>
            )}

            <DeleteAccountSection/>
        </section>
    );
};

export default SecuritySection;

