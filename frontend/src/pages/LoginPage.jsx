import React, {useEffect, useMemo, useState} from 'react';
import {useLocation} from 'react-router-dom';
import AuthPage from './AuthPage';

const PREFILL_EMAIL_STORAGE_KEY = 'loginPrefillEmail';

const LoginPage = () => {
    const location = useLocation();

    const emailFromState = useMemo(() => {
        const state = location.state;
        const candidate =
            (typeof state?.email === 'string' ? state.email : '') ||
            // Backwards-compat: other parts of the app historically used `identifier`.
            (typeof state?.identifier === 'string' ? state.identifier : '');
        return candidate.trim().toLowerCase();
    }, [location.state]);

    // null => derive from router state / sessionStorage
    // ''   => user explicitly cleared prefill (show full login flow)
    const [prefilledEmailOverride, setPrefilledEmailOverride] = useState(null);

    const resolvedPrefilledEmail = useMemo(() => {
        if (prefilledEmailOverride !== null) {
            return prefilledEmailOverride;
        }

        const fromStorage = sessionStorage.getItem(PREFILL_EMAIL_STORAGE_KEY) || '';
        return (emailFromState || fromStorage).trim().toLowerCase();
    }, [emailFromState, prefilledEmailOverride]);

    useEffect(() => {
        // Persist route-provided prefill so refresh falls back to the same email.
        if (prefilledEmailOverride !== null) return;
        if (!emailFromState) return;
        sessionStorage.setItem(PREFILL_EMAIL_STORAGE_KEY, emailFromState);
    }, [emailFromState, prefilledEmailOverride]);

    const handleClearPrefilledEmail = () => {
        setPrefilledEmailOverride('');
        sessionStorage.removeItem(PREFILL_EMAIL_STORAGE_KEY);
    };

    return (
        <AuthPage
            key={resolvedPrefilledEmail || 'no-prefill'}
            prefilledEmail={resolvedPrefilledEmail}
            onClearPrefilledEmail={handleClearPrefilledEmail}
        />
    );
};

export default LoginPage;
