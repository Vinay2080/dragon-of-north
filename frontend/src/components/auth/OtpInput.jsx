import React, {useCallback, useRef} from 'react';

const OtpInput = ({
                      value,
                      onChange,
                      length = 6,
                      idPrefix = 'otp',
                      disabled = false,
                      error = false,
                      autoSubmit = false,
                      onComplete,
                      className = '',
                  }) => {
    const inputRefs = useRef([]);

    const focusInput = useCallback((index) => {
        const clampedIndex = Math.max(0, Math.min(index, length - 1));
        inputRefs.current[clampedIndex]?.focus();
        inputRefs.current[clampedIndex]?.select();
    }, [length]);

    const notifyChange = useCallback((nextOtp) => {
        onChange(nextOtp);
        if (autoSubmit && onComplete && nextOtp.every((digit) => digit !== '')) {
            onComplete(nextOtp.join(''));
        }
    }, [autoSubmit, onChange, onComplete]);

    const handleInputChange = (index, event) => {
        if (disabled) {
            return;
        }

        const rawValue = event.target.value || '';
        const digitsOnly = rawValue.replace(/\D/g, '');

        if (rawValue === '') {
            const nextOtp = [...value];
            nextOtp[index] = '';
            notifyChange(nextOtp);
            return;
        }

        if (!digitsOnly) {
            return;
        }

        const nextOtp = [...value];
        nextOtp[index] = digitsOnly[digitsOnly.length - 1];
        notifyChange(nextOtp);

        if (index < length - 1) {
            focusInput(index + 1);
        }
    };

    const handleKeyDown = (index, event) => {
        if (disabled) {
            return;
        }

        if (event.key === 'Backspace') {
            event.preventDefault();
            const nextOtp = [...value];

            if (nextOtp[index]) {
                nextOtp[index] = '';
                notifyChange(nextOtp);
                if (index > 0) {
                    focusInput(index - 1);
                }
                return;
            }

            if (index > 0) {
                nextOtp[index - 1] = '';
                notifyChange(nextOtp);
                focusInput(index - 1);
            }
            return;
        }

        if (event.key === 'ArrowLeft' && index > 0) {
            event.preventDefault();
            focusInput(index - 1);
            return;
        }

        if (event.key === 'ArrowRight' && index < length - 1) {
            event.preventDefault();
            focusInput(index + 1);
        }
    };

    const handlePaste = (event) => {
        event.preventDefault();

        if (disabled) {
            return;
        }

        const pasted = event.clipboardData?.getData('text') || '';
        const digits = pasted.replace(/\D/g, '').slice(0, length).split('');

        if (digits.length === 0) {
            return;
        }

        const nextOtp = Array(length).fill('');
        digits.forEach((digit, idx) => {
            nextOtp[idx] = digit;
        });

        notifyChange(nextOtp);
        focusInput(Math.min(digits.length, length) - 1);
    };

    return (
        <div className={`flex items-center justify-center gap-3 ${className}`.trim()}>
            {Array.from({length}).map((_, index) => {
                const hasValue = Boolean(value[index]);
                const baseClasses = 'h-14 w-12 rounded-xl border text-center text-xl font-bold shadow-sm outline-none transition-all duration-200';
                const stateClasses = error
                    ? 'border-red-500/70 bg-red-500/5 text-red-700 focus:border-red-500 focus:ring-4 focus:ring-red-500/20 dark:text-red-300'
                    : 'border-[var(--don-border-default)] bg-[var(--don-bg-card)] text-[var(--don-text-primary)] focus:border-[var(--don-accent-border)] focus:ring-4 focus:ring-violet-500/20';
                const valueClasses = hasValue ? 'scale-[1.01]' : 'scale-100';

                return (
                    <input
                        key={`${idPrefix}-${index}`}
                        ref={(element) => {
                            inputRefs.current[index] = element;
                        }}
                        id={`${idPrefix}-${index}`}
                        type="text"
                        inputMode="numeric"
                        autoComplete="one-time-code"
                        pattern="[0-9]*"
                        maxLength={1}
                        value={value[index] || ''}
                        disabled={disabled}
                        onChange={(event) => handleInputChange(index, event)}
                        onKeyDown={(event) => handleKeyDown(index, event)}
                        onFocus={(event) => event.target.select()}
                        onPaste={handlePaste}
                        aria-label={`OTP digit ${index + 1}`}
                        aria-invalid={error ? 'true' : 'false'}
                        className={`${baseClasses} ${stateClasses} ${valueClasses}`}
                    />
                );
            })}
        </div>
    );
};

export default OtpInput;


