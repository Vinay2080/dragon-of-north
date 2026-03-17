import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {useAuth} from '../context/authUtils';
import {apiService} from '../services/apiService';
import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device';
import Skeleton from '../components/Loading/Skeleton';
import Spinner from '../components/Loading/Spinner';
import {useToast} from '../hooks/useToast';

const animateTo = (target, duration = 450) => {
    const steps = 16;
    const increment = target / steps;
    const interval = duration / steps;
    let current = 0;
    return {steps, increment, interval, current};
};

const DashboardPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const {user, logout, isAuthenticated} = useAuth();
    const {toast} = useToast();

    const [isLoggingOut, setIsLoggingOut] = useState(false);
    const [sessions, setSessions] = useState([]);
    const [loadingSessions, setLoadingSessions] = useState(true);
    const [refreshSpinning, setRefreshSpinning] = useState(false);

    const [animatedTotal, setAnimatedTotal] = useState(0);
    const [animatedActive, setAnimatedActive] = useState(0);
    const [animatedRevoked, setAnimatedRevoked] = useState(0);

    const currentDeviceId = getDeviceId();
    const sessionStats = useMemo(() => {
        const active = sessions.filter(s => !s.revoked).length;
        const revoked = sessions.filter(s => s.revoked).length;
        return {active, revoked, total: sessions.length};
    }, [sessions]);
    const activeOtherDevices = useMemo(() => sessions.filter(s => !s.revoked && s.device_id !== currentDeviceId).length, [sessions, currentDeviceId]);

    useEffect(() => {
        const runCounter = (target, setter) => {
            const {steps, increment, interval} = animateTo(target);
            let currentStep = 0;
            let currentValue = 0;

            const timer = setInterval(() => {
                currentStep += 1;
                currentValue += increment;
                if (currentStep >= steps) {
                    setter(target);
                    clearInterval(timer);
                    return;
                }
                setter(Math.round(currentValue));
            }, interval);

            return timer;
        };

        const timers = [
            runCounter(sessionStats.total, setAnimatedTotal),
            runCounter(sessionStats.active, setAnimatedActive),
            runCounter(sessionStats.revoked, setAnimatedRevoked),
        ];

        return () => timers.forEach(clearInterval);
    }, [sessionStats.active, sessionStats.revoked, sessionStats.total]);

    const loadSessions = useCallback(async (withAnimation = false) => {
        if (!isAuthenticated) {
            toast.warning('Please log in first to manage sessions.');
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
        if (isAuthenticated) {
            loadSessions();
        }
    }, [isAuthenticated, loadSessions]);

    useEffect(() => {
        if (location.hash !== '#sessions') {
            return;
        }

        const timer = window.setTimeout(() => {
            document.getElementById('sessions-section')?.scrollIntoView({behavior: 'smooth', block: 'start'});
        }, 120);

        return () => window.clearTimeout(timer);
    }, [location.hash]);

    const handleLogout = async () => {
        setIsLoggingOut(true);
        try {
            await logout();
            navigate('/', {replace: true});
        } finally {
            setIsLoggingOut(false);
        }
    };

    const revokeSession = async (sessionId) => {
        const previous = [...sessions];
        setSessions(prev => prev.map(s => s.session_id === sessionId ? {...s, revoked: true} : s));

        const result = await apiService.delete(API_CONFIG.ENDPOINTS.SESSION_REVOKE(sessionId));
        if (apiService.isErrorResponse(result)) {
            setSessions(previous);
            toast.error(result.message || 'Failed to revoke session.');
            return;
        }

        toast.success('Session revoked successfully.');
    };

    const revokeOthers = async () => {
        const previous = [...sessions];
        setSessions(prev => prev.map(s => s.device_id !== currentDeviceId ? {...s, revoked: true} : s));

        const result = await apiService.post(API_CONFIG.ENDPOINTS.SESSION_REVOKE_OTHERS, {device_id: currentDeviceId});
        if (apiService.isErrorResponse(result)) {
            setSessions(previous);
            toast.error(result.message || 'Failed to revoke other sessions.');
            return;
        }

        toast.success(result?.message || 'Other sessions revoked successfully.');
    };

    return (
        <div className="dashboard-shell">
            <header className="dashboard-header">
                <div className="dashboard-header__inner">
                    <div className="page-header mb-0">
                        <h1 className="page-title mb-1">Dashboard</h1>
                        <p className="page-subtitle">Session-aware authentication center</p>
                    </div>
                    <button onClick={handleLogout} disabled={isLoggingOut} className="btn-subtle dashboard-action-btn">
                        {isLoggingOut ? <span className="inline-flex items-center gap-2"><Spinner size="sm"/> Logging out...</span> : 'Logout'}
                    </button>
                </div>
            </header>

            <main className="dashboard-main">
                <div className="card dashboard-card">
                    <div className="card-header">
                        <h2 className="card-title">Account Information</h2>
                    </div>
                    <div className="space-y-2">
                        <p style={{color: 'var(--don-text-secondary)', fontSize: '14px'}}>
                            Identifier: <span style={{
                            color: 'var(--don-text-primary)',
                            fontWeight: 500
                        }}>{user?.identifier || 'Not available'}</span>
                        </p>
                        <p style={{color: 'var(--don-text-secondary)', fontSize: '14px'}}>
                            Current Device: <span className="cell-mono ip-addr">{currentDeviceId}</span>
                        </p>
                        <p style={{color: 'var(--don-text-secondary)', fontSize: '14px'}}>
                            Active on other devices: <span
                            style={{color: 'var(--don-accent-text)', fontWeight: 600}}>{activeOtherDevices}</span>
                        </p>
                    </div>
                </div>

                <div className="stats-grid">
                    <div className="stat-card stat-card--total">
                        <div className="stat-value">{animatedTotal}</div>
                        <div className="stat-label">Total Sessions</div>
                    </div>
                    <div className="stat-card stat-card--active">
                        <div className="stat-value" style={{color: 'var(--don-success)'}}>{animatedActive}</div>
                        <div className="stat-label">Active Sessions</div>
                    </div>
                    <div className="stat-card stat-card--revoked">
                        <div className="stat-value" style={{color: 'var(--don-warning)'}}>{animatedRevoked}</div>
                        <div className="stat-label">Revoked Sessions</div>
                    </div>
                </div>

                <div id="sessions-section" className="card dashboard-card dashboard-card--sessions">
                    <div className="card-header">
                        <h2 className="card-title">Session Management</h2>
                        <div className="dashboard-actions">
                            <button onClick={() => loadSessions(true)} disabled={!isAuthenticated}
                                    className="btn-subtle dashboard-action-btn">
                                <span className={refreshSpinning ? 'db-spin' : ''}>↻</span>
                                Refresh
                            </button>
                            <button onClick={revokeOthers} className="btn-danger dashboard-action-btn dashboard-action-btn--wide">Revoke All Other Devices</button>
                        </div>
                    </div>

                    {loadingSessions ? (
                        <div className="space-y-3 mt-4">
                            <Skeleton className="h-10 w-full"/>
                            <Skeleton className="h-10 w-full"/>
                            <Skeleton className="h-10 w-full"/>
                        </div>
                    ) : (
                        <div className="table-wrapper mt-4">
                            <table className="table">
                                <thead>
                                <tr>
                                    <th>Device ID</th>
                                    <th>IP Address</th>
                                    <th>Last Used</th>
                                    <th>Expires</th>
                                    <th>Status</th>
                                    <th>Action</th>
                                </tr>
                                </thead>
                                <tbody>
                                {sessions.map((session) => {
                                    const isCurrentDevice = session.device_id === currentDeviceId;
                                    return (
                                        <tr
                                            key={session.session_id}
                                            className={`session-row ${session.revoked ? 'session-row--revoked' : 'session-row--active'} ${isCurrentDevice ? 'session-row--current' : ''}`}
                                        >
                                            <td className="cell-mono">
                                                {session.device_id}
                                                {isCurrentDevice && <span className="badge-accent ml-2">current</span>}
                                            </td>
                                            <td className="cell-mono cell-ip">{session.ip_address || '-'}</td>
                                            <td className="cell-mono cell-timestamp">{session.last_used_at ? new Date(session.last_used_at).toLocaleString() : '-'}</td>
                                            <td className="cell-mono cell-timestamp">{session.expiry_date ? new Date(session.expiry_date).toLocaleString() : '-'}</td>
                                            <td>
                                                {session.revoked
                                                    ? <span className="badge badge-danger">Revoked</span>
                                                    : <span className="badge badge-success">Active</span>}
                                            </td>
                                            <td>
                                                <button
                                                    onClick={() => revokeSession(session.session_id)}
                                                    disabled={session.revoked || isCurrentDevice}
                                                    className="btn-subtle text-xs px-3 py-1"
                                                    style={session.revoked || isCurrentDevice ? {
                                                        opacity: 0.4,
                                                        cursor: 'not-allowed'
                                                    } : {}}
                                                >
                                                    Revoke
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
};

export default DashboardPage;
