import React from 'react';
import Button from '../ui/Button.jsx';

const AuthButton = ({className = '', loading = false, children, disabled, ...props}) => {
    const resolvedClassName = `auth-primary-btn ${className}`.trim();

    return (
        <Button
            {...props}
            variant="primary"
            className={resolvedClassName}
            disabled={disabled || loading}
        >
            {loading ? (
                <span className="btn-loading-indicator">
                    <span className="spinner spinner-sm"></span>
                    <span>Signing in...</span>
                </span>
            ) : (
                children
            )}
        </Button>
    );
};

export default AuthButton;

