import React from 'react';

const TYPE_STYLES = {
    success: 'border-emerald-300/80 bg-emerald-50/90 text-emerald-800 dark:border-emerald-500/35 dark:bg-emerald-500/10 dark:text-emerald-200',
    error: 'border-rose-300/80 bg-rose-50/90 text-rose-800 dark:border-rose-500/35 dark:bg-rose-500/10 dark:text-rose-200',
    info: 'border-sky-300/80 bg-sky-50/90 text-sky-800 dark:border-sky-500/35 dark:bg-sky-500/10 dark:text-sky-200',
};

const AlertBanner = ({type = 'info', message}) => {
    if (!message) {
        return null;
    }

    return (
        <div
            role="alert"
            className={`mb-4 rounded-2xl border px-4 py-3 text-sm font-medium shadow-sm transition-all ${TYPE_STYLES[type] || TYPE_STYLES.info}`}
        >
            {message}
        </div>
    );
};

export default AlertBanner;

