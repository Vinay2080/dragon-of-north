import React, {useState} from 'react';
import AuthButton from '../auth/AuthButton';
import PasswordInput from '../auth/PasswordInput';
import ValidationError from '../Validation/ValidationError';
import {apiService} from '../../services/apiService';
import {API_CONFIG} from '../../config';
import {useToast} from '../../hooks/useToast';
import {useAuth} from '../../context/authUtils';

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

const MIN_PASSWORD_LENGTH = 8;

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

    const setInlineErrorByCode = (result) => {
        const errorCode = result?.errorCode || result?.raw?.code;

        if (errorCode === 'PASSWORD_MISMATCH') {
            setPasswordErrors((prev) => ({
                ...prev,
                confirmPassword: [result.backendMessage || result.message || 'Passwords do not match.'],
            }));
            return true;
        }

        if (errorCode === 'INVALID_CURRENT_PASSWORD') {
            setPasswordErrors((prev) => ({
                ...prev,
                currentPassword: [result.backendMessage || result.message || 'Current password is incorrect.'],
            }));
            return true;
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
        } else if (passwordForm.newPassword.length < MIN_PASSWORD_LENGTH) {
            nextErrors.newPassword.push(`New password must be at least ${MIN_PASSWORD_LENGTH} characters.`);
        }

        if (!passwordForm.confirmPassword) {
            nextErrors.confirmPassword.push('Please confirm your new password.');
        }

        if (passwordForm.newPassword && passwordForm.confirmPassword && passwordForm.newPassword !== passwordForm.confirmPassword) {
            nextErrors.confirmPassword.push('New password and confirm password must match.');
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

            toast.success('Password updated successfully.');
        } finally {
            // Clear sensitive fields immediately after request completion.
            resetPasswordForm();
            setIsSubmitting(false);
        }
    };

    return (
        <section className="group rounded-2xl border border-slate-200/80 bg-slate-50/80 p-6 shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-md dark:border-slate-800 dark:bg-slate-900/70">
            <div className="mb-4 space-y-3">
                <div className="flex items-center gap-3">
                    <span className="inline-flex h-8 w-8 items-center justify-center rounded-full border border-slate-300 bg-white text-xs font-semibold text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200">
                        {normalizedAuthProvider === 'GOOGLE' ? 'G' : '🔒'}
                    </span>
                    <div>
                        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Security control panel</h2>
                        <p className="text-sm text-slate-500 dark:text-slate-400">
                            {normalizedAuthProvider === 'GOOGLE' ? 'Signed in via Google' : 'Signed in with local credentials'}
                        </p>
                    </div>
                </div>
                <div className="rounded-xl border border-slate-200/80 bg-white/70 px-3 py-2 text-sm text-slate-600 dark:border-slate-800 dark:bg-slate-900/70 dark:text-slate-300">
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
                        onChange={(event) => setPasswordForm((prev) => ({
                            ...prev,
                            currentPassword: event.target.value
                        }))}
                        hasError={Boolean(passwordErrors.currentPassword.length)}
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
                        onChange={(event) => setPasswordForm((prev) => ({...prev, newPassword: event.target.value}))}
                        hasError={Boolean(passwordErrors.newPassword.length)}
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
                        onChange={(event) => setPasswordForm((prev) => ({
                            ...prev,
                            confirmPassword: event.target.value
                        }))}
                        hasError={Boolean(passwordErrors.confirmPassword.length)}
                        placeholder="Confirm password"
                        autoComplete="new-password"
                    />
                    <ValidationError errors={passwordErrors.confirmPassword}/>
                </div>

                <AuthButton type="submit" disabled={isSubmitting}>
                    {isSubmitting ? (
                        <span className="btn-loading-indicator">
                            <span className="spinner spinner-sm"></span>
                            <span>Updating password...</span>
                        </span>
                    ) : 'Update password'}
                </AuthButton>
                </form>
            )}
        </section>
    );
};

export default SecuritySection;







