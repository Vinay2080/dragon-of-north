import React from 'react';

const AuthInput = ({className = '', hasError = false, ...props}) => {
    const resolvedClassName = `auth-input ${hasError ? 'error' : ''} ${className}`.trim();
    return <input {...props} className={resolvedClassName}/>;
};

export default AuthInput;

