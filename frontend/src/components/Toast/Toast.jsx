import React from 'react';

const styleMap = {
    success: 'border-green-500/40 bg-green-900/40 text-green-100',
    error: 'border-red-500/40 bg-red-900/40 text-red-100',
    warning: 'border-yellow-500/40 bg-yellow-900/40 text-yellow-100',
    info: 'border-blue-500/40 bg-blue-900/40 text-blue-100',
};

const Toast = ({title, message, variant = 'info', onClose}) => {
    return (
        <div
            role="status"
            aria-live="polite"
            className={`w-full rounded-xl border px-4 py-3 shadow-xl ${styleMap[variant] || styleMap.info}`}
        >
            <div className="flex items-start justify-between gap-3">
                <div>
                    <p className="text-sm font-semibold">{title}</p>
                    <p className="mt-1 text-sm opacity-90">{message}</p>
                </div>
                <button
                    onClick={onClose}
                    aria-label="Dismiss notification"
                    className="rounded px-2 py-1 text-xs bg-black/20 hover:bg-black/30"
                >
                    ✕
                </button>
            </div>
        </div>
    );
};

export default Toast;
