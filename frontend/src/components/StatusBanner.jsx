import React from 'react';

const VARIANT_STYLES = {
    success: 'border-emerald-300/80 bg-emerald-50/90 text-emerald-800 dark:border-emerald-500/35 dark:bg-emerald-500/10 dark:text-emerald-200',
    error: 'border-rose-300/80 bg-rose-50/90 text-rose-800 dark:border-rose-500/35 dark:bg-rose-500/10 dark:text-rose-200',
    info: 'border-amber-300/80 bg-amber-50/90 text-amber-800 dark:border-amber-500/35 dark:bg-amber-500/10 dark:text-amber-200',
};

const StatusBanner = ({variant = 'info', message}) => {
    if (!message) {
        return null;
    }

    return (
        <div
            role="status"
            className={`mb-4 rounded-2xl border px-4 py-3 text-sm font-medium shadow-sm ${VARIANT_STYLES[variant] || VARIANT_STYLES.info}`}
        >
            {message}
        </div>
    );
};

export default StatusBanner;

