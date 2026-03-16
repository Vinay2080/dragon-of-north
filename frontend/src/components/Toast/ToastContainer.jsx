import React from 'react';
import Toast from './Toast';
import {useToast} from '../../hooks/useToast';

const ToastContainer = () => {
  const {toasts, removeToast} = useToast();

  return (
    <div className="toast-container" aria-live="polite" aria-label="Notifications">
      {toasts.map(toast => (
        <Toast
          key={toast.id}
          title={toast.title}
          message={toast.message}
          variant={toast.variant}
          duration={toast.duration}
          onClose={() => removeToast(toast.id)}
        />
      ))}
    </div>
  );
};

export default ToastContainer;
