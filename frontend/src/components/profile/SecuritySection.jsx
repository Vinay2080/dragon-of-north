import React, {useState} from 'react';
import {Shield} from 'lucide-react';
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
            className="group rounded-3xl border border-slate-200/80 bg-white/60 p-6 shadow-[0_22px_52px_rgba(15,23,42,0.10)] backdrop-blur transition-all duration-300 hover:-translate-y-0.5 hover:border-sky-200/70 hover:shadow-[0_28px_62px_rgba(14,165,233,0.12)] dark:border-slate-800/80 dark:bg-slate-950/30 dark:hover:border-sky-500/25">
            <div className="mb-4 space-y-3">
                <div className="flex items-center gap-3">
                    <span
                        className={`inline-flex h-11 w-11 items-center justify-center rounded-2xl border text-xs font-semibold shadow-sm backdrop-blur ${
                            canChangePassword
                                ? 'border-emerald-200/80 bg-emerald-50/70 text-emerald-700 dark:border-emerald-500/25 dark:bg-emerald-500/10 dark:text-emerald-200'
                                : 'border-orange-200/80 bg-orange-50/70 text-orange-700 dark:border-orange-500/25 dark:bg-orange-500/10 dark:text-orange-200'
                        }`}>
                        {normalizedAuthProvider === 'GOOGLE' ? (
                            <span className="text-sm font-semibold">G</span>
                        ) : (
                            <Shield className="h-5 w-5"/>
                        )}
                    </span>
                    <div>
                        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Security control panel</h2>
                        <p className="text-sm text-slate-500 dark:text-slate-400">
                            {normalizedAuthProvider === 'GOOGLE' ? 'Signed in via Google' : 'Signed in with local credentials'}
                        </p>
                    </div>
                </div>
                <div className={`rounded-2xl border px-4 py-3 text-sm shadow-sm backdrop-blur ${
                    canChangePassword
                        ? 'border-teal-200/70 bg-white/55 text-slate-700 dark:border-teal-500/25 dark:bg-slate-950/35 dark:text-slate-200'
                        : 'border-orange-200/70 bg-white/55 text-slate-700 dark:border-orange-500/25 dark:bg-slate-950/35 dark:text-slate-200'
                }`}>
                    <p>{canChangePassword ? 'Password is managed in this account.' : 'Password managed by Google.'}</p>
                    <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Enable MFA (coming soon) · Add backup authentication (coming soon)</p>
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
                    <label className="auth-label">Current password</label>
                    <PasswordInput
                        id="currentPassword"
                        name="currentPassword"
                        value={passwordForm.currentPassword}
                        onChange={(event) => updatePasswordField('currentPassword', event.target.value)}
                        hasError={Boolean(passwordErrors.currentPassword.length)}
                        className="border-slate-200 bg-white/90 focus:border-[#14B8A6] focus:ring-4 focus:ring-teal-500/15 dark:border-slate-700 dark:bg-slate-950/60 dark:focus:border-[#14B8A6] dark:focus:ring-teal-400/10"
                        placeholder="Current password"
                        autoComplete="current-password"
                    />
                    <ValidationError errors={passwordErrors.currentPassword}/>
                </div>

                <div>
                    <label className="auth-label">New password</label>
                    <PasswordInput
                        id="newPassword"
                        name="newPassword"
                        value={passwordForm.newPassword}
                        onChange={(event) => updatePasswordField('newPassword', event.target.value)}
                        hasError={Boolean(passwordErrors.newPassword.length)}
                        className="border-slate-200 bg-white/90 focus:border-[#14B8A6] focus:ring-4 focus:ring-teal-500/15 dark:border-slate-700 dark:bg-slate-950/60 dark:focus:border-[#14B8A6] dark:focus:ring-teal-400/10"
                        placeholder="New password"
                        autoComplete="new-password"
                    />
                    <ValidationError errors={passwordErrors.newPassword}/>
                </div>

                <div>
                    <label className="auth-label">Confirm password</label>
                    <PasswordInput
                        id="confirmPassword"
                        name="confirmPassword"
                        value={passwordForm.confirmPassword}
                        onChange={(event) => updatePasswordField('confirmPassword', event.target.value)}
                        hasError={Boolean(passwordErrors.confirmPassword.length)}
                        className="border-slate-200 bg-white/90 focus:border-[#14B8A6] focus:ring-4 focus:ring-teal-500/15 dark:border-slate-700 dark:bg-slate-950/60 dark:focus:border-[#14B8A6] dark:focus:ring-teal-400/10"
                        placeholder="Confirm password"
                        autoComplete="new-password"
                    />
                    <ValidationError errors={passwordErrors.confirmPassword}/>
                </div>

                    <AuthButton
                        type="submit"
                        disabled={isSubmitting}
                        className="h-11 rounded-2xl border-0 bg-[linear-gradient(135deg,#14B8A6,#0EA5E9)] px-4 text-sm font-semibold text-white shadow-[0_16px_30px_rgba(20,184,166,0.24)] transition-all hover:-translate-y-0.5 hover:shadow-[0_18px_34px_rgba(20,184,166,0.32)] disabled:cursor-not-allowed disabled:opacity-60"
                    >
                    {isSubmitting ? (
                        <span className="btn-loading-indicator">
                            <span className="spinner spinner-sm"></span>
                            <span>Updating password...</span>
                        </span>
                    ) : 'Update password'}
                </AuthButton>
                </form>
            )}

            <DeleteAccountSection/>
        </section>
    );
};

export default SecuritySection;

