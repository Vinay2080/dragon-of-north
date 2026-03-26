import {useContext, useMemo} from 'react';
import {ToastContext} from '../context/ToastContext.js';

export const useToast = () => {
    const context = useContext(ToastContext);
    if (!context) {
        throw new Error('useToast must be used within a ToastProvider');
    }

    const {addToast, toasts, removeToast} = context;

    const toast = useMemo(() => ({
        success: (message, title = 'Success') => addToast({variant: 'success', message, title}),
        error: (message, title = 'Error') => addToast({variant: 'error', message, title}),
        warning: (message, title = 'Warning') => addToast({variant: 'warning', message, title}),
        info: (message, title = 'Info') => addToast({variant: 'info', message, title}),
    }), [addToast]);

    return {
        toasts,
        removeToast,
        toast,
    };
};
