import React from 'react';
import Toast from './Toast';
import {useToast} from '../../hooks/useToast';

const ToastContainer = () => {
    const {toasts, removeToast} = useToast();

    return (
        <div className="pointer-events-none fixed right-4 top-4 z-[120] flex w-[calc(100%-2rem)] max-w-sm flex-col gap-3" aria-live="polite" aria-label="Notifications">
            {toasts.map(toast => (
                <div key={toast.id} className="pointer-events-auto">
                    <Toast
                        title={toast.title}
                        message={toast.message}
                        variant={toast.variant}
                        duration={toast.duration}
                        onClose={() => removeToast(toast.id)}
                    />
                </div>
            ))}
        </div>
    );
};

export default ToastContainer;
