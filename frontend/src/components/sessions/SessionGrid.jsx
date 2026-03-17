import React from 'react';
import Skeleton from '../Loading/Skeleton';
import SessionCard from './SessionCard';
import {formatDateTime, formatLocation, getDeviceSummary} from './sessionFormatters';

const SessionGrid = ({sessions, loading, currentDeviceId, revokingIds, onRevoke}) => {
    if (loading) {
        return (
            <div className="session-grid" aria-label="Loading sessions">
                {[...Array(3)].map((_, index) => (
                    <div className="session-card session-card--skeleton" key={index}>
                        <Skeleton className="h-5 w-32"/>
                        <Skeleton className="mt-2 h-4 w-44"/>
                        <div className="mt-5 grid gap-3 sm:grid-cols-2">
                            <Skeleton className="h-10 w-full"/>
                            <Skeleton className="h-10 w-full"/>
                        </div>
                        <Skeleton className="mt-5 h-8 w-full"/>
                    </div>
                ))}
            </div>
        );
    }

    if (!sessions.length) {
        return (
            <div className="session-empty-state">
                <p className="session-empty-state__title">No sessions found</p>
                <p className="session-empty-state__body">Your account currently has no tracked device sessions.</p>
            </div>
        );
    }

    return (
        <div className="session-grid" aria-label="Session cards grid">
            {sessions.map((session) => {
                const isCurrentDevice = session.device_id === currentDeviceId;
                return (
                    <SessionCard
                        key={session.session_id}
                        session={session}
                        deviceLabel={getDeviceSummary(session)}
                        secondaryLabel={formatLocation(session)}
                        lastUsed={formatDateTime(session.last_used_at)}
                        expiresAt={formatDateTime(session.expiry_date)}
                        isCurrentDevice={isCurrentDevice}
                        isRevoking={revokingIds.has(session.session_id)}
                        onRevoke={onRevoke}
                    />
                );
            })}
        </div>
    );
};

export default SessionGrid;

