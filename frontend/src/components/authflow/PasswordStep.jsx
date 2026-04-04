import React from 'react';
import PasswordInput from '../auth/PasswordInput';
import AuthButton from '../auth/AuthButton';
import ValidationError from '../Validation/ValidationError';

const PasswordStep = ({password, onPasswordChange, onSubmit, onBack, isLoading, errorMessage}) => {
    return (
        <form onSubmit={onSubmit} className="auth-form-stack">
            <label className="auth-label block">Password</label>
            <PasswordInput
                value={password}
                onChange={(event) => onPasswordChange(event.target.value)}
                placeholder="Enter your password"
                hasError={Boolean(errorMessage)}
                disabled={isLoading}
                required
            />
            <ValidationError errors={errorMessage ? [errorMessage] : []}/>
            <div className="flex items-center gap-3">
                <AuthButton type="button" onClick={onBack} disabled={isLoading}>
                    Change email
                </AuthButton>
                <AuthButton type="submit" loading={isLoading} disabled={isLoading || !password}>
                    Login
                </AuthButton>
            </div>
        </form>
    );
};

export default PasswordStep;

