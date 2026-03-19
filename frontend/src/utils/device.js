const fallbackDeviceId = () => {
    // Fallback for browsers/environments without crypto.randomUUID.
    const randomChunk = () => Math.random().toString(16).slice(2, 10);
    return `dev-${Date.now().toString(16)}-${randomChunk()}-${randomChunk()}`;
};

const generateDeviceId = () => {
    if (typeof globalThis !== 'undefined' && globalThis.crypto?.randomUUID) {
        return globalThis.crypto.randomUUID();
    }

    return fallbackDeviceId();
};

export const getDeviceId = () => {
    try {
        let deviceId = localStorage.getItem('deviceId');
        if (!deviceId) {
            deviceId = generateDeviceId();
            localStorage.setItem('deviceId', deviceId);
        }

        return deviceId;
    } catch {
        // Keep the app functional even when storage access is blocked.
        return generateDeviceId();
    }
};