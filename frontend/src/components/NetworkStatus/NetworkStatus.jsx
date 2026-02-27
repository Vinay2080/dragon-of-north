import React from 'react';
import {useNetworkStatus} from '../../hooks/useNetworkStatus';

const NetworkStatus = () => {
    const {isOnline} = useNetworkStatus();

    if (isOnline) {
        return null;
    }

    return (
        <div role="alert" className="fixed bottom-4 left-1/2 z-[100] -translate-x-1/2 rounded-lg bg-red-700 px-4 py-2 text-sm text-white shadow-lg">
            You are offline. Some actions may fail until connection is restored.
        </div>
    );
};

export default NetworkStatus;
