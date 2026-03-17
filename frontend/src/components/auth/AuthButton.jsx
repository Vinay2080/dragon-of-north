import React from 'react';

const AuthButton = ({className = '', ...props}) => {
    const resolvedClassName = `auth-primary-btn ${className}`.trim();
    return <button {...props} className={resolvedClassName}/>;
};

export default AuthButton;

