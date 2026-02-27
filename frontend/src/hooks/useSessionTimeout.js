import {useEffect, useRef} from 'react';

export const useSessionTimeout = ({enabled = false, warningMs = 120000, sessionMs = 3600000, onWarning, onTimeout}) => {
    const warningRef = useRef(null);
    const timeoutRef = useRef(null);

    useEffect(() => {
        if (!enabled) {
            return undefined;
        }

        warningRef.current = window.setTimeout(() => {
            onWarning?.();
        }, Math.max(1000, sessionMs - warningMs));

        timeoutRef.current = window.setTimeout(() => {
            onTimeout?.();
        }, sessionMs);

        return () => {
            if (warningRef.current) {
                clearTimeout(warningRef.current);
            }
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current);
            }
        };
    }, [enabled, warningMs, sessionMs, onWarning, onTimeout]);
};
