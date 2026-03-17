import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {useAuth} from '../context/authUtils';
import {apiService} from '../services/apiService';
import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device';
import {useToast} from '../hooks/useToast';
import {RefreshCw} from 'lucide-react';
import SummarySection from '../components/sessions/SummarySection';
import SessionGrid from '../components/sessions/SessionGrid';
import {formatDateTime} from '../components/sessions/sessionFormatters';

const SessionsPage = () => {
    const {user, isAuthenticated} = useAuth();
    const {toast} = useToast();

    const [sessions, setSessions] = useState([]);
    const [loadingSessions, setLoadingSessions] = useState(false);
    const [refreshSpinning, setRefreshSpinning] = useState(false);
    const [revokingIds, setRevokingIds] = useState(new Set());
    const [revokingOthers, setRevokingOthers] = useState(false);

    const currentDeviceId = getDeviceId();
    const sessionStats = useMemo(() => {
        const active = sessions.filter(s => !s.revoked).length;
        const revoked = sessions.filter(s => s.revoked).length;
        return {active, revoked, total: sessions.length};
    }, [sessions]);
    const activeOtherDevices = useMemo(() => sessions.filter(s => !s.revoked && s.device_id !== currentDeviceId).length, [sessions, currentDeviceId]);
    const summaryStats = useMemo(() => {
        const lastLoginSession = sessions
            .filter((session) => Boolean(session.last_used_at))
            .sort((a, b) => new Date(b.last_used_at).getTime() - new Date(a.last_used_at).getTime())[0];

        return {
            totalSessions: sessionStats.total,
            activeSessions: sessionStats.active,
            lastLogin: lastLoginSession ? formatDateTime(lastLoginSession.last_used_at) : 'No recent activity',
        };
    }, [sessions, sessionStats.active, sessionStats.total]);

    const loadSessions = useCallback(async (withAnimation = false) => {
        if (!isAuthenticated) {
            setSessions([]);
            setLoadingSessions(false);
            return;
        }

        if (withAnimation) {
            setRefreshSpinning(true);
        }

        setLoadingSessions(true);
        const result = await apiService.get(API_CONFIG.ENDPOINTS.SESSIONS_ALL);
        if (apiService.isErrorResponse(result)) {
            toast.error(result.message || 'Failed to load sessions.');
            setLoadingSessions(false);
            setRefreshSpinning(false);
            return;
        }

        if (result?.api_response_status === 'success' && Array.isArray(result?.data)) {
            setSessions(result.data);
        } else {
            toast.warning('Unexpected sessions response from server.');
        }

        setLoadingSessions(false);
        setRefreshSpinning(false);
    }, [isAuthenticated, toast]);

    useEffect(() => {
        const timer = setTimeout(() => {
            void loadSessions();
        }, 0);
        return () => clearTimeout(timer);
    }, [loadSessions]);

    const revokeSession = async (sessionId) => {
        if (revokingIds.has(sessionId)) return;

        setRevokingIds((prev) => new Set(prev).add(sessionId));
        const previous = [...sessions];
        setSessions(prev => prev.map(s => s.session_id === sessionId ? {...s, revoked: true} : s));

        const result = await apiService.delete(API_CONFIG.ENDPOINTS.SESSION_REVOKE(sessionId));
        if (apiService.isErrorResponse(result)) {
            setSessions(previous);
            setRevokingIds((prev) => {
                const next = new Set(prev);
                next.delete(sessionId);
                return next;
            });
            toast.error(result.message || 'Failed to revoke session.');
            return;
        }

        setRevokingIds((prev) => {
            const next = new Set(prev);
            next.delete(sessionId);
            return next;
        });
        toast.success('Session revoked successfully.');
    };

    const revokeOthers = async () => {
        if (revokingOthers || activeOtherDevices === 0) return;

        setRevokingOthers(true);
        const previous = [...sessions];
        setSessions(prev => prev.map(s => s.device_id !== currentDeviceId ? {...s, revoked: true} : s));

        const result = await apiService.post(API_CONFIG.ENDPOINTS.SESSION_REVOKE_OTHERS, {device_id: currentDeviceId});
        if (apiService.isErrorResponse(result)) {
            setSessions(previous);
            setRevokingOthers(false);
            toast.error(result.message || 'Failed to revoke other sessions.');
            return;
        }

        setRevokingOthers(false);
        toast.success(result?.message || 'Other sessions revoked successfully.');
    };

    return (
        <div className="sessions-page space-y-6">
            <header>
                <h1 className="sessions-page__title">Session Security</h1>
                <p className="sessions-page__subtitle">Review active devices and control account access in one
                    place.</p>
            </header>

            <SummarySection
                totalSessions={summaryStats.totalSessions}
                activeSessions={summaryStats.activeSessions}
                lastLogin={summaryStats.lastLogin}
            />

            <section id="sessions-section" className="sessions-panel" aria-label="Session management">
                <div className="sessions-panel__header">
                    <div>
                        <h2 className="sessions-panel__title">Session Management</h2>
                        <p className="sessions-panel__subtitle">
                            {isAuthenticated
                                ? `Signed in as ${user?.identifier || 'account user'} • ${activeOtherDevices} other active device${activeOtherDevices === 1 ? '' : 's'}`
                                : 'Login required to view active sessions.'}
                        </p>
                    </div>

                    <div className="sessions-panel__actions">
                        <button
                            type="button"
                            onClick={() => loadSessions(true)}
                            disabled={!isAuthenticated || refreshSpinning || loadingSessions}
                            className="sessions-refresh-btn"
                            aria-label="Refresh sessions"
                        >
                            <RefreshCw size={14} className={refreshSpinning ? 'db-spin' : ''}/>
                        </button>

                        <button
                            type="button"
                            onClick={revokeOthers}
                            disabled={!isAuthenticated || activeOtherDevices === 0 || revokingOthers}
                            className="sessions-revoke-all-btn"
                        >
                            {revokingOthers ? 'Revoking...' : 'Revoke All Other Devices'}
                        </button>
                    </div>
                </div>

                <SessionGrid
                    sessions={sessions}
                    loading={loadingSessions}
                    currentDeviceId={currentDeviceId}
                    revokingIds={revokingIds}
                    onRevoke={revokeSession}
                />
            </section>
        </div>
    );
};

export default SessionsPage;

