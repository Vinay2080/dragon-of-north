import {useCallback, useEffect, useMemo, useState} from 'react';

const safeReadNumber = (value) => {
    const parsed = Number.parseInt(String(value ?? ''), 10);
    return Number.isFinite(parsed) ? parsed : 0;
};

const readEndsAtFromStorage = (storageKey) => {
    if (!storageKey) return 0;

    try {
        return safeReadNumber(localStorage.getItem(storageKey));
    } catch {
        return 0;
    }
};

const writeEndsAtToStorage = (storageKey, endsAt) => {
    if (!storageKey) return;

    try {
        localStorage.setItem(storageKey, String(endsAt));
    } catch {
        // ignore storage failures (private mode / blocked)
    }
};

const removeEndsAtFromStorage = (storageKey) => {
    if (!storageKey) return;

    try {
        localStorage.removeItem(storageKey);
    } catch {
        // ignore
    }
};

/**
 * Small utility hook for “cooldown” UX (e.g., prevent double-click / rapid retries).
 *
 * - Persists the cooldown end timestamp in localStorage (optional)
 * - Updates remaining seconds every 250ms while active
 */
export const useCooldown = ({storageKey, cooldownMs = 30_000} = {}) => {
    const [localEndsAt, setLocalEndsAt] = useState(0);
    const [now, setNow] = useState(() => Date.now());

    // Used only to force a re-read from localStorage when another tab updates the value.
    const [storageSignal, setStorageSignal] = useState(0);

    // Keep in sync across tabs.
    useEffect(() => {
        if (!storageKey) return;

        const handler = (event) => {
            if (event.key !== storageKey) return;
            setStorageSignal((current) => current + 1);
        };

        window.addEventListener('storage', handler);
        return () => window.removeEventListener('storage', handler);
    }, [storageKey]);

    const storedEndsAt = useMemo(() => {
        // Intentionally depend on storageSignal to force a re-read when another tab updates localStorage.
        void storageSignal;
        return readEndsAtFromStorage(storageKey);
    }, [storageKey, storageSignal]);

    const endsAt = Math.max(localEndsAt, storedEndsAt);

    const remainingMs = useMemo(() => {
        if (!endsAt) return 0;
        return Math.max(0, endsAt - now);
    }, [endsAt, now]);

    const isCoolingDown = remainingMs > 0;

    const secondsRemaining = useMemo(() => {
        if (!isCoolingDown) return 0;
        return Math.ceil(remainingMs / 1000);
    }, [isCoolingDown, remainingMs]);

    // Drive the live countdown.
    useEffect(() => {
        if (!isCoolingDown) return undefined;

        const interval = window.setInterval(() => {
            const currentNow = Date.now();
            setNow(currentNow);

            const nextRemaining = Math.max(0, endsAt - currentNow);
            if (nextRemaining <= 0) {
                removeEndsAtFromStorage(storageKey);
                setLocalEndsAt(0);
                setStorageSignal((current) => current + 1);
            }
        }, 250);

        return () => window.clearInterval(interval);
    }, [endsAt, isCoolingDown, storageKey]);

    const startCooldown = useCallback(() => {
        const nextEndsAt = Date.now() + cooldownMs;
        writeEndsAtToStorage(storageKey, nextEndsAt);
        setLocalEndsAt(nextEndsAt);
        setStorageSignal((current) => current + 1);
        setNow(Date.now());
    }, [cooldownMs, storageKey]);

    const clearCooldown = useCallback(() => {
        removeEndsAtFromStorage(storageKey);
        setLocalEndsAt(0);
        setStorageSignal((current) => current + 1);
        setNow(Date.now());
    }, [storageKey]);

    return {
        isCoolingDown,
        secondsRemaining,
        endsAt,
        startCooldown,
        clearCooldown,
    };
};





