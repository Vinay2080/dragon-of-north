import React, {useEffect, useMemo, useRef, useState} from 'react';

const variantStyles = {
    success: {
        accent: 'border-l-[#22c55e]',
        iconColor: 'text-[#22c55e]',
        progressColor: 'bg-[#22c55e]',
    },
    error: {
        accent: 'border-l-[#ef4444]',
        iconColor: 'text-[#ef4444]',
        progressColor: 'bg-[#ef4444]',
    },
    warning: {
        accent: 'border-l-[#f59e0b]',
        iconColor: 'text-[#f59e0b]',
        progressColor: 'bg-[#f59e0b]',
    },
    info: {
        accent: 'border-l-[#3b82f6]',
        iconColor: 'text-[#3b82f6]',
        progressColor: 'bg-[#3b82f6]',
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
    const [isPaused, setIsPaused] = useState(false);
    const [remaining, setRemaining] = useState(duration);
    const timerRef = useRef(null);
    const startRef = useRef(Date.now());

    const styles = variantStyles[variant] || variantStyles.info;
    const progressPct = useMemo(() => {
        if (!duration || duration <= 0) return 0;
        return Math.max(0, Math.min(100, (remaining / duration) * 100));
    }, [duration, remaining]);

    useEffect(() => {
        const visibleTimer = window.setTimeout(() => setIsVisible(true), 10);
        return () => window.clearTimeout(visibleTimer);
    }, []);

    useEffect(() => {
        if (duration <= 0 || isPaused) {
            return undefined;
        }

        startRef.current = Date.now();
        timerRef.current = window.setTimeout(() => {
            setIsVisible(false);
            window.setTimeout(() => onClose(), 180);
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
            className={`relative w-full overflow-hidden rounded-xl border border-white/10 border-l-4 bg-[#0f172a] shadow-2xl transition-all duration-200 ${styles.accent} ${isVisible ? 'translate-y-0 scale-100 opacity-100' : '-translate-y-2 scale-[0.98] opacity-0'}`}
        >
            <div className="flex items-start gap-3 px-4 py-3">
                <div className={`mt-0.5 ${styles.iconColor}`}>
                    <VariantIcon variant={variant} />
                </div>

                <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-[#f9fafb]">{title}</p>
                    {message && <p className="mt-1 text-sm text-[#9ca3af]">{message}</p>}
                </div>

                <button
                    onClick={() => {
                        setIsVisible(false);
                        window.setTimeout(() => onClose(), 120);
                    }}
                    aria-label="Dismiss notification"
                    className="rounded p-1.5 text-[#9ca3af] transition hover:text-[#f9fafb] focus:outline-none focus-visible:ring-2 focus-visible:ring-cyan-300/60"
                >
                    <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden>
                        <path fill="currentColor" d="m18.3 5.71-1.41-1.42L12 9.17 7.11 4.29 5.7 5.71 10.59 10.6 5.7 15.49l1.41 1.42L12 12l4.89 4.91 1.41-1.42-4.89-4.89z"/>
                    </svg>
                </button>
            </div>

            {duration > 0 && (
                <div className="h-0.5 w-full bg-white/5">
                    <div
                        className={`h-full transition-[width] duration-100 ease-linear ${styles.progressColor}`}
                        style={{width: `${progressPct}%`}}
                    />
                </div>
            )}
        </div>
    );
};

export default Toast;
