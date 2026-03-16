import React, {useCallback, useMemo, useState} from 'react';
import {ToastContext} from './ToastContext.js';

let toastId = 0;

const withDefaultDuration = (variant, message, duration) => {
    if (typeof duration === 'number') {
        return duration;
    }

    if (variant === 'error') {
        return 5000;
    }

    if (variant === 'success') {
        const normalized = (message || '').toLowerCase();
        if (normalized.includes('revoke') || normalized.includes('session')) {
            return 4000;
        }
        return 3000;
    }

    return 4000;
};

export const ToastProvider = ({children}) => {
    const [toasts, setToasts] = useState([]);

    const removeToast = useCallback((id) => {
        setToasts(prev => prev.filter(toast => toast.id !== id));
    }, []);

    const addToast = useCallback((toast) => {
        const id = ++toastId;
        const variant = toast.variant || 'info';
        const nextToast = {
            id,
            variant,
            duration: withDefaultDuration(variant, toast.message, toast.duration),
            title: toast.title,
            message: toast.message,
        };

        setToasts(prev => [...prev, nextToast].slice(-4));
        return id;
    }, []);

    const value = useMemo(() => ({toasts, addToast, removeToast}), [toasts, addToast, removeToast]);

    return <ToastContext.Provider value={value}>{children}</ToastContext.Provider>;
};
