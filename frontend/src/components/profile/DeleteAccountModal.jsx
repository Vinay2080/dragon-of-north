import React from 'react';
import {createPortal} from 'react-dom';

const DeleteAccountModal = ({open, isLoading, onCancel, onConfirm}) => {
    if (!open) {
        return null;
    }

    const modalContent = (
        <div
            className="fixed top-0 left-0 z-[9999] flex h-screen w-screen items-center justify-center p-4"
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-account-title"
        >
            <div
                className="absolute inset-0 bg-black/60 backdrop-blur-[6px] transition-opacity duration-200"
                onClick={isLoading ? undefined : onCancel}
                aria-hidden="true"
            />
            <div className="relative w-full max-w-md rounded-3xl border border-slate-200/80 bg-white p-6 shadow-[0_24px_50px_rgba(15,23,42,0.24)] transition-all duration-200 dark:border-slate-700/70 dark:bg-slate-900">
                <h3 id="delete-account-title" className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                    Delete Account
                </h3>
                <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
                    Are you sure you want to delete your account? This action cannot be undone.
                </p>
                <div className="mt-5 flex items-center justify-end gap-3">
                    <button
                        type="button"
                        onClick={onCancel}
                        disabled={isLoading}
                        className="h-10 rounded-2xl border border-slate-300/80 bg-white px-4 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
                    >
                        Cancel
                    </button>
                    <button
                        type="button"
                        onClick={onConfirm}
                        disabled={isLoading}
                        className="h-10 rounded-2xl border border-rose-300/70 bg-[linear-gradient(135deg,#f43f5e,#fb7185)] px-4 text-sm font-semibold text-white shadow-[0_12px_24px_rgba(244,63,94,0.26)] transition-all hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60 dark:border-rose-400/40"
                    >
                        {isLoading ? 'Deleting...' : 'Confirm Delete'}
                    </button>
                </div>
            </div>
        </div>
    );

    return createPortal(modalContent, document.body);
};

export default DeleteAccountModal;

