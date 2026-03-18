import {useContext, useMemo} from 'react';
import {ToastContext} from '../context/ToastContext.js';

export const useToast = () => {
    const context = useContext(ToastContext);
    if (!context) {
        throw new Error('useToast must be used within a ToastProvider');
    }

    const toast = useMemo(() => ({
        success: (message, title = 'Success') => context.addToast({variant: 'success', message, title}),
        error: (message, title = 'Error') => context.addToast({variant: 'error', message, title}),
        warning: (message, title = 'Warning') => context.addToast({variant: 'warning', message, title}),
        info: (message, title = 'Info') => context.addToast({variant: 'info', message, title}),
    }), [context]);

    return {
        toasts: context.toasts,
        removeToast: context.removeToast,
        toast,
    };
};
