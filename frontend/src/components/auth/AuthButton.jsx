import React from 'react';

const AuthButton = ({className = '', loading = false, children, disabled, ...props}) => {
    const resolvedClassName = `auth-primary-btn ${className}`.trim();

    return (
        <button
            {...props}
            className={resolvedClassName}
            disabled={disabled || loading}
            style={{
                opacity: loading ? 0.8 : 1,
                cursor: loading ? 'not-allowed' : 'pointer',
            }}
        >
            {loading ? (
                <span className="btn-loading-indicator">
                    <span className="spinner spinner-sm"></span>
                    <span>Signing in...</span>
                </span>
            ) : (
                children
            )}
        </button>
    );
};

export default AuthButton;

