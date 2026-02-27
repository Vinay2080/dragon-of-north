import React from 'react';
import Toast from './Toast';
import {useToast} from '../../hooks/useToast';

const ToastContainer = () => {
    const {toasts, removeToast} = useToast();

    return (
        <div className="fixed right-4 top-4 z-[100] flex w-full max-w-sm flex-col gap-3" aria-live="polite">
            {toasts.map(toast => (
                <Toast
                    key={toast.id}
                    title={toast.title}
                    message={toast.message}
                    variant={toast.variant}
                    onClose={() => removeToast(toast.id)}
                />
            ))}
        </div>
    );
};

export default ToastContainer;
