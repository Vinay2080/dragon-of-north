import React from 'react';

const AuthCardLayout = ({title, subtitle, children, footer}) => {
    return (
        <div className="auth-shell">
            <div className="auth-card">
                <h1 className="auth-title">{title}</h1>
                {subtitle && <p className="auth-subtitle">{subtitle}</p>}
                <div className="auth-card-content">{children}</div>
                {footer}
            </div>
        </div>
    );
};

export default AuthCardLayout;

