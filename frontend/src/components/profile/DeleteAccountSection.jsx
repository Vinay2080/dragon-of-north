import React, {useState} from 'react';
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
            <div className="danger-zone-panel">
                <h3 className="danger-zone-title">Danger zone</h3>
                <p className="mt-2 text-sm text-slate-700 dark:text-slate-200">
                    Deleting your account marks it as deleted and immediately signs you out.
                </p>
                <button
                    type="button"
                    onClick={() => setIsConfirmOpen(true)}
                    disabled={isDeleting}
                    className="danger-zone-button"
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
