import React from 'react';
import GoogleLoginButton from '../auth/GoogleLoginButton';
import AuthButton from '../auth/AuthButton';

const GoogleAuthButton = ({email, onSuccess, onError, onBack, disabled}) => {
    return (
        <div className="auth-form-stack">
            <p className="auth-helper">This account uses Google sign-in.</p>
            <GoogleLoginButton
                mode="login"
                onSuccess={onSuccess}
                onError={onError}
                expectedIdentifier={email}
                disabled={disabled}
            />
            <AuthButton type="button" onClick={onBack} disabled={disabled}>
                Change email
            </AuthButton>
        </div>
    );
};

export default GoogleAuthButton;

