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

    const metricCards = [
        {label: 'Total Sessions', value: animatedTotal, variant: 'violet'},
        {label: 'Active Sessions', value: animatedActive, variant: 'mint'},
        {label: 'Revoked Sessions', value: animatedRevoked, variant: 'amber'},
    ];

    return (
        <div className="dashboard-shell">
            <header className="dashboard-header">
                <div className="dashboard-header__inner">
                    <div>
                        <h1 className="dashboard-title">Dashboard</h1>
                        <p className="dashboard-subtitle">Session-aware authentication center</p>
                    </div>
                    <button onClick={handleLogout} disabled={isLoggingOut} className="db-btn db-btn-secondary">
                        {isLoggingOut ? <span className="inline-flex items-center gap-2"><Spinner size="sm"/> Logging out...</span> : 'Logout'}
                    </button>
                </div>
            </header>

            <main className="dashboard-main">
                <div className="db-card db-card-hover">
                    <h2 className="db-section-title">Account Information</h2>
                    <p className="db-text-muted">Identifier: <span className="db-text-primary">{user?.identifier || 'Not available'}</span></p>
                    <p className="db-text-muted">Current Device ID: <span className="db-mono">{currentDeviceId}</span></p>
                    <p className="mt-3 text-sm text-[#A0A8B8]">Active sessions on other devices: <span className="db-accent-violet">{activeOtherDevices}</span></p>
                </div>

                <div className="db-metric-grid">
                    {metricCards.map((metric) => (
                        <div key={metric.label} className="db-card db-card-hover">
                            <h3 className="db-metric-label">{metric.label}</h3>
                            <p className={`db-metric-value db-metric-${metric.variant}`}>{metric.value}</p>
                        </div>
                    ))}
                </div>

                <div id="sessions-section" className="db-card">
                    <div className="mb-5 flex flex-wrap gap-3">
                        <button onClick={() => loadSessions(true)} disabled={!isAuthenticated} className="db-btn db-btn-secondary">
                            <span className={`inline-flex items-center gap-2 ${refreshSpinning ? 'db-spin' : ''}`}>↻</span>
                            Refresh Sessions
                        </button>
                        <button onClick={revokeOthers} className="db-btn db-btn-primary">Graceful logout on all devices</button>
                    </div>

                    {loadingSessions ? (
                        <div className="space-y-3">
                            <Skeleton className="h-10 w-full"/>
                            <Skeleton className="h-10 w-full"/>
                            <Skeleton className="h-10 w-full"/>
                        </div>
                    ) : (
                        <div className="overflow-x-auto rounded-xl border border-white/10">
                            <table className="db-table">
                                <thead>
                                <tr>
                                    <th>Device ID</th>
                                    <th>IP</th>
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
                                        <tr key={session.session_id} className="db-table-row">
                                            <td className="db-mono">
                                                {session.device_id}
                                                {isCurrentDevice && <span className="db-chip db-chip-violet ml-2">current</span>}
                                            </td>
                                            <td>{session.ip_address || '-'}</td>
                                            <td>{session.last_used_at ? new Date(session.last_used_at).toLocaleString() : '-'}</td>
                                            <td>{session.expiry_date ? new Date(session.expiry_date).toLocaleString() : '-'}</td>
                                            <td>
                                                {session.revoked
                                                    ? <span className="db-chip db-chip-coral">Revoked</span>
                                                    : <span className="db-chip db-chip-mint">Active</span>}
                                            </td>
                                            <td>
                                                <button
                                                    onClick={() => revokeSession(session.session_id)}
                                                    disabled={session.revoked || isCurrentDevice}
                                                    className="db-btn db-btn-inline"
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
