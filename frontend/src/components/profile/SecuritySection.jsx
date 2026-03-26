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

const SecuritySection = () => {
    const {toast} = useToast();
    const {user} = useAuth();
    const [passwordForm, setPasswordForm] = useState(EMPTY_PASSWORD_STATE);
    const [passwordErrors, setPasswordErrors] = useState(EMPTY_PASSWORD_ERRORS);
    const [isSubmitting, setIsSubmitting] = useState(false);

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
        if (isSubmitting) {
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
        <section className="rounded-xl border border-border bg-card p-6">
            <div className="mb-4">
                <h2 className="text-lg font-semibold text-foreground">Security</h2>
                <p className="text-sm text-muted-foreground">Change your password and keep your account protected.</p>
            </div>

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
        </section>
    );
};

export default SecuritySection;








