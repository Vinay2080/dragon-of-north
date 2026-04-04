import React from 'react';
import AuthInput from '../auth/AuthInput';
import AuthButton from '../auth/AuthButton';

const EmailStep = ({email, onEmailChange, onContinue, isLoading, disabled}) => {
    return (
        <form onSubmit={onContinue} className="auth-form-stack">
            <label className="auth-label">Email</label>
            <AuthInput
                type="email"
                value={email}
                onChange={(event) => onEmailChange(event.target.value)}
                placeholder="you@example.com"
                disabled={isLoading || disabled}
                required
            />
            <AuthButton type="submit" loading={isLoading} disabled={isLoading || disabled || !email.trim()}>
                Continue
            </AuthButton>
        </form>
    );
};

export default EmailStep;

