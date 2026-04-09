import React, {useState} from 'react';

/**
 * Password Input with Show/Hide Toggle
 * Features:
 * - Toggle button inside the input field
 * - Right-aligned, minimal styling
 * - Consistent with dark theme
 * - No layout shift on toggle
 */
const PasswordInput = ({
                           value,
                           onChange,
                           onInput,
                           onPaste,
                           placeholder = 'Enter your password',
                           hasError = false,
                           disabled = false,
                           required = false,
                           className = '',
                           ...props
                       }) => {
    const [showPassword, setShowPassword] = useState(false);

    return (
        <div className="password-input-wrapper">
            <input
                {...props}
                type={showPassword ? 'text' : 'password'}
                value={value}
                onChange={onChange}
                onInput={onInput}
                onPaste={onPaste}
                placeholder={placeholder}
                disabled={disabled}
                required={required}
                className={`auth-input password-input-field ${hasError ? 'error' : ''} ${className}`.trim()}
            />
            <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                disabled={disabled}
                className="password-toggle-btn"
                title={showPassword ? 'Hide password' : 'Show password'}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
                {showPassword ? 'Hide' : 'Show'}
            </button>
        </div>
    );
};

export default PasswordInput;
