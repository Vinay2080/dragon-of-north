import React, {useState} from 'react';
import {Zap} from 'lucide-react';
import {useNavigate} from 'react-router-dom';
import {useToast} from '../../hooks/useToast';
import {useAuth} from '../../context/authUtils';
import {deleteCurrentUser} from '../../services/userAccountService';
import DeleteAccountModal from './DeleteAccountModal';

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
            <div
                className="mt-8 rounded-2xl border border-rose-200/70 bg-white/55 p-5 shadow-sm backdrop-blur transition-colors dark:border-rose-500/25 dark:bg-slate-950/35">
                <div className="flex items-start gap-3">
                    <span
                        className="inline-flex h-10 w-10 items-center justify-center rounded-2xl border border-rose-200/70 bg-rose-50/70 text-rose-700 shadow-sm dark:border-rose-500/25 dark:bg-rose-500/10 dark:text-rose-200">
                        <Zap className="h-5 w-5"/>
                    </span>
                    <div className="min-w-0">
                        <h3 className="text-sm font-semibold uppercase tracking-[0.12em] text-rose-800 dark:text-rose-200">Danger
                            zone</h3>
                        <p className="mt-2 text-sm text-slate-700 dark:text-slate-200">
                    Deleting your account marks it as deleted and immediately signs you out.
                </p>
                    </div>
                </div>
                <button
                    type="button"
                    onClick={() => setIsConfirmOpen(true)}
                    disabled={isDeleting}
                    className="mt-4 h-11 rounded-2xl border border-rose-300/70 bg-[linear-gradient(135deg,#f43f5e,#fb7185)] px-4 text-sm font-semibold text-white shadow-[0_16px_30px_rgba(244,63,94,0.24)] transition-all hover:-translate-y-0.5 hover:shadow-[0_18px_34px_rgba(244,63,94,0.32)] focus:outline-none focus:ring-2 focus:ring-rose-300/60 disabled:cursor-not-allowed disabled:opacity-60 dark:border-rose-400/35"
                >
                    {isDeleting ? 'Deleting account...' : 'Delete account'}
                </button>
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
