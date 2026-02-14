import React, {useEffect, useState} from 'react';
import {apiService} from '../services/apiService';

/**
 * RateLimitInfo Component
 * Displays rate limit information to the user
 */
const RateLimitInfo = () => {
    const [rateLimitInfo, setRateLimitInfo] = useState({
        remaining: null,
        capacity: null,
        retryAfter: null,
    });
    const [countdown, setCountdown] = useState(null);

    useEffect(() => {
        // Subscribe to rate limit updates
        const unsubscribe = apiService.onRateLimitUpdate((info) => {
            setRateLimitInfo(info);
            if (info.retryAfter) {
                setCountdown(info.retryAfter);
            }
        });

        // Initial load - defer to the next tick to prevent cascading renders
        const timeoutId = setTimeout(() => {
            setRateLimitInfo(apiService.getRateLimitInfo());
        }, 0);

        return () => {
            unsubscribe();
            clearTimeout(timeoutId);
        };
    }, []);

    useEffect(() => {
        // Countdown timer for retry after
        if (countdown > 0) {
            const timer = setInterval(() => {
                setCountdown((prev) => {
                    if (prev <= 1) {
                        return null;
                    }
                    return prev - 1;
                });
            }, 1000);

            return () => clearInterval(timer);
        }
    }, [countdown]);

    // Don't render if no rate limit info
    if (rateLimitInfo.remaining === null || rateLimitInfo.capacity === null) {
        return null;
    }

    const percentage = (rateLimitInfo.remaining / rateLimitInfo.capacity) * 100;
    const isLow = percentage < 30;
    const isBlocked = rateLimitInfo.retryAfter && countdown;

    return (
        <div className="mt-4">
            {isBlocked ? (
                <div className="rounded-lg border border-red-800 bg-red-950/50 p-3">
                    <div className="flex items-center gap-2 text-red-400">
                        <svg
                            className="h-5 w-5"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                            />
                        </svg>
                        <span className="text-sm font-medium">Rate limit exceeded</span>
                    </div>
                    <p className="mt-1 text-sm text-red-300">
                        Please wait {countdown} second{countdown !== 1 ? 's' : ''} before trying again
                    </p>
                </div>
            ) : (
                <div className="rounded-lg border border-slate-700 bg-slate-900/50 p-3">
                    <div className="mb-2 flex items-center justify-between">
                        <span className="text-xs font-medium text-slate-400">Requests remaining</span>
                        <span className={`text-xs font-bold ${isLow ? 'text-yellow-400' : 'text-slate-300'}`}>
                            {rateLimitInfo.remaining} / {rateLimitInfo.capacity}
                        </span>
                    </div>
                    <div className="h-2 w-full overflow-hidden rounded-full bg-slate-800">
                        <div
                            className={`h-full transition-all duration-300 ${
                                isLow ? 'bg-yellow-500' : 'bg-blue-500'
                            }`}
                            style={{width: `${percentage}%`}}
                        />
                    </div>
                    {isLow && (
                        <p className="mt-2 text-xs text-yellow-400">
                            ⚠ You're running low on requests. Please slow down.
                        </p>
                    )}
                </div>
            )}
        </div>
    );
};

export default RateLimitInfo;
