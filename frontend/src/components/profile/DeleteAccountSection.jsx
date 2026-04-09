import React, {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useToast} from '../../hooks/useToast';
import {useAuth} from '../../context/authUtils';
import {deleteCurrentUser} from '../../services/userAccountService';
import DeleteAccountModal from './DeleteAccountModal';
import Button from '../ui/Button.jsx';

const DeleteAccountSection = () => {
    const navigate = useNavigate();
    const {toast} = useToast();
    const {forceLogout, user} = useAuth();
    const [isDeleting, setIsDeleting] = useState(false);
    const [isConfirmOpen, setIsConfirmOpen] = useState(false);

    const handleDelete = async () => {
        if (isDeleting) {
            return;
        }

        setIsDeleting(true);
        try {
            const result = await deleteCurrentUser();

            if (result?.type === 'RATE_LIMIT_EXCEEDED') {
                toast.error('Too many attempts. Please try again shortly.');
                return;
            }

            if (result?.type === 'API_ERROR' || result?.type === 'NETWORK_ERROR') {
                toast.error(result.backendMessage || result.message || 'Unable to delete account right now.');
                return;
            }

            toast.success('Account deleted successfully.');
            forceLogout({redirectTo: null});
            navigate('/signup', {
                replace: true,
                state: {
                    reason: 'deleted',
                    identifier: user?.email || user?.identifier || '',
                    identifierType: 'EMAIL',
                },
            });
        } finally {
            setIsDeleting(false);
            setIsConfirmOpen(false);
        }
    };

    return (
        <>
            <div className="mt-6 rounded-2xl border border-rose-200/70 bg-gradient-to-br from-rose-50/80 to-rose-100/60 p-5 shadow-sm transition-all dark:border-rose-500/25 dark:bg-gradient-to-br dark:from-rose-500/10 dark:to-rose-600/5">
                <div className="flex items-start gap-3">
                    <div className="mt-0.5 flex h-10 w-10 items-center justify-center rounded-lg bg-rose-500/15 ring-1 ring-rose-300/40 dark:bg-rose-500/10 dark:ring-rose-400/30">
                        <svg className="h-5 w-5 text-rose-700 dark:text-rose-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                    </div>
                    <div className="flex-1">
                        <h3 className="text-sm font-bold uppercase tracking-[0.12em] text-rose-800 dark:text-rose-200">Danger zone</h3>
                        <p className="mt-1.5 text-sm text-slate-700 dark:text-slate-300">
                            Deleting your account marks it as deleted and immediately signs you out.
                        </p>
                    </div>
                </div>
                <Button
                    type="button"
                    onClick={() => setIsConfirmOpen(true)}
                    disabled={isDeleting}
                    variant="danger"
                    className="mt-4 h-11 w-full rounded-lg sm:w-auto"
                >
                    {isDeleting ? (
                        <span className="flex items-center justify-center gap-2">
                            <span className="spinner spinner-sm"></span>
                            <span>Deleting account...</span>
                        </span>
                    ) : (
                        'Delete account'
                    )}
                </Button>
            </div>

            <DeleteAccountModal
                open={isConfirmOpen}
                isLoading={isDeleting}
                onCancel={() => setIsConfirmOpen(false)}
                onConfirm={handleDelete}
            />
        </>
    );
};

export default DeleteAccountSection;
