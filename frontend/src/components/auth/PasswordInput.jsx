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
                           inputRef,
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
                ref={inputRef}
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
                {showPassword ? (
                    <svg
                        className="password-toggle-icon"
                        viewBox="0 0 24 24"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                        aria-hidden="true"
                    >
                        <path
                            d="M3 3L21 21M10.58 10.58C10.21 10.95 10 11.46 10 12C10 13.1 10.9 14 12 14C12.54 14 13.05 13.79 13.42 13.42M9.88 5.09C10.56 4.91 11.27 4.81 12 4.81C16.8 4.81 20.79 8.02 22 12C21.48 13.72 20.46 15.21 19.12 16.31M14.12 18.89C13.43 19.08 12.72 19.19 12 19.19C7.2 19.19 3.21 15.98 2 12C2.52 10.28 3.54 8.79 4.88 7.69"
                            stroke="currentColor"
                            strokeWidth="1.8"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        />
                    </svg>
                ) : (
                    <svg
                        className="password-toggle-icon"
                        viewBox="0 0 24 24"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                        aria-hidden="true"
                    >
                        <path
                            d="M2 12C3.21 8.02 7.2 4.81 12 4.81C16.8 4.81 20.79 8.02 22 12C20.79 15.98 16.8 19.19 12 19.19C7.2 19.19 3.21 15.98 2 12Z"
                            stroke="currentColor"
                            strokeWidth="1.8"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        />
                        <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="1.8"/>
                    </svg>
                )}
            </button>
        </div>
    );
};

export default PasswordInput;
