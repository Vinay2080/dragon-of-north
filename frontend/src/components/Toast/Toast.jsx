import React, {useEffect, useMemo, useRef, useState} from 'react';

const variantStyles = {
    success: {
        colorVar: 'var(--don-success)',
    },
    error: {
        colorVar: 'var(--don-danger)',
    },
    warning: {
        colorVar: 'var(--don-warning)',
    },
    info: {
        colorVar: 'var(--don-info)',
    },
};

const VariantIcon = ({variant}) => {
    const common = 'h-4 w-4';

    if (variant === 'success') {
        return (
            <svg viewBox="0 0 24 24" className={common} aria-hidden>
                <path fill="currentColor" d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2Zm-1.1 14.2-3.2-3.2 1.4-1.4 1.8 1.8 4.2-4.2 1.4 1.4Z"/>
            </svg>
        );
    }

    if (variant === 'error') {
        return (
            <svg viewBox="0 0 24 24" className={common} aria-hidden>
                <path fill="currentColor" d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2Zm1 15h-2v-2h2Zm0-4h-2V7h2Z"/>
            </svg>
        );
    }

    if (variant === 'warning') {
        return (
            <svg viewBox="0 0 24 24" className={common} aria-hidden>
                <path fill="currentColor" d="M1 21h22L12 2Zm12-3h-2v-2h2Zm0-4h-2v-4h2Z"/>
            </svg>
        );
    }

    return (
        <svg viewBox="0 0 24 24" className={common} aria-hidden>
            <path fill="currentColor" d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2Zm1 15h-2v-6h2Zm0-8h-2V7h2Z"/>
        </svg>
    );
};

const Toast = ({title, message, variant = 'info', duration = 4000, onClose}) => {
    const [isVisible, setIsVisible] = useState(false);
    const [isExiting, setIsExiting] = useState(false);
    const [isPaused, setIsPaused] = useState(false);
    const [remaining, setRemaining] = useState(duration);
    const timerRef = useRef(null);
    const startRef = useRef(null);

    const styles = variantStyles[variant] || variantStyles.info;
    const progressPct = useMemo(() => {
        if (!duration || duration <= 0) return 0;
        return Math.max(0, Math.min(100, (remaining / duration) * 100));
    }, [duration, remaining]);

    useEffect(() => {
        startRef.current = Date.now();
        const visibleTimer = window.setTimeout(() => setIsVisible(true), 10);
        return () => window.clearTimeout(visibleTimer);
    }, []);

    useEffect(() => {
        if (duration <= 0 || isPaused) {
            return undefined;
        }

        startRef.current = Date.now();
        timerRef.current = window.setTimeout(() => {
            setIsExiting(true);
            window.setTimeout(() => onClose(), 250);
        }, remaining);

        return () => {
            if (timerRef.current) {
                window.clearTimeout(timerRef.current);
                const elapsed = Date.now() - startRef.current;
                setRemaining((prev) => Math.max(0, prev - elapsed));
            }
        };
    }, [duration, isPaused, onClose, remaining]);

    return (
        <div
            role="alert"
            aria-live="assertive"
            onMouseEnter={() => setIsPaused(true)}
            onMouseLeave={() => setIsPaused(false)}
            onFocusCapture={() => setIsPaused(true)}
            onBlurCapture={() => setIsPaused(false)}
            className={`toast ${variant} ${isVisible && !isExiting ? 'toast-enter' : ''} ${isExiting ? 'toast-exit' : ''} relative w-full overflow-hidden`}
        >
            <div className="flex items-start gap-3">
                <div className="mt-0.5" style={{color: styles.colorVar}}>
                    <VariantIcon variant={variant} />
                </div>

                <div className="min-w-0 flex-1">
                    <p className="toast-title">{title}</p>
                    {message && <p className="toast-body mt-1">{message}</p>}
                </div>

                <button
                    onClick={() => {
                        setIsExiting(true);
                        window.setTimeout(() => onClose(), 250);
                    }}
                    aria-label="Dismiss notification"
                    className="toast-close-btn"
                >
                    <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden>
                        <path fill="currentColor" d="m18.3 5.71-1.41-1.42L12 9.17 7.11 4.29 5.7 5.71 10.59 10.6 5.7 15.49l1.41 1.42L12 12l4.89 4.91 1.41-1.42-4.89-4.89z"/>
                    </svg>
                </button>
            </div>

            {duration > 0 && (
                <div className="toast-progress-track">
                    <div
                        className="toast-progress-bar"
                        style={Object.assign({}, {
                            '--progress-color': styles.colorVar,
                            animation: isExiting ? 'none' : `toast-progress ${duration}ms cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards`,
                            width: isExiting ? '0%' : `${progressPct}%`,
                        })}
                    />
                </div>
            )}
        </div>
    );
};

export default Toast;
