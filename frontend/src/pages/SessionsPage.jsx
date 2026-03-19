import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {ChevronDown, Loader2, Trash2} from 'lucide-react';
import {useAuth} from '../context/authUtils';
import {useToast} from '../hooks/useToast';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import {formatDateTime, formatLocation, parseUserAgent} from '../components/sessions/sessionFormatters';
import {getDeviceId} from '../utils/device';

const browserGlyph = (browser) => {
    const normalized = (browser || '').toLowerCase();
    if (normalized.includes('edge')) return 'E';
    if (normalized.includes('chrome')) return 'C';
    if (normalized.includes('safari')) return 'S';
    if (normalized.includes('firefox')) return 'F';
    if (normalized.includes('opera')) return 'O';
    return '?';
};

const buildFingerprint = (session) => {
    const {browser, os} = parseUserAgent(session?.user_agent || session?.userAgent);
    return {
        browser,
        os,
        label: `${os} + ${browser}`,
        glyph: browserGlyph(browser),
    };
};

const getSortTime = (session) => {
    const ts = session?.last_used_at || session?.created_at || session?.issued_at || session?.login_at;
    const value = ts ? new Date(ts).getTime() : 0;
    return Number.isFinite(value) ? value : 0;
};

const SessionsPage = () => {
    const {user, isAuthenticated} = useAuth();
    const {toast} = useToast();

    const [sessions, setSessions] = useState([]);
    const [loadingSessions, setLoadingSessions] = useState(false);
    const [refreshSpinning, setRefreshSpinning] = useState(false);
    const [revokingIds, setRevokingIds] = useState(new Set());
    const [revokingOthers, setRevokingOthers] = useState(false);
    const [showRevoked, setShowRevoked] = useState(false);
    const [isLoading, setIsLoading] = useState(true);

    const currentDeviceId = getDeviceId();

    const {activeSessions, revokedSessions, currentSession, activeOtherDevices, timelineItems} = useMemo(() => {
        const sorted = [...sessions].sort((a, b) => getSortTime(b) - getSortTime(a));
        const active = sorted.filter((s) => !s.revoked);
        const revoked = sorted.filter((s) => s.revoked);
        const current = active.find((s) => s.device_id === currentDeviceId) || sorted.find((s) => s.device_id === currentDeviceId) || null;
        const others = active.filter((s) => s.device_id !== currentDeviceId).length;
        const timeline = sorted.map((session) => {
            const fingerprint = buildFingerprint(session);
            const location = formatLocation(session);
            const loginTime = session.created_at || session.issued_at || session.last_used_at;
            return {
                id: session.session_id,
                isRevoked: Boolean(session.revoked),
                device: fingerprint.label,
                location,
                loginTime,
            };
        });

        return {
            activeSessions: active,
            revokedSessions: revoked,
            currentSession: current,
            activeOtherDevices: others,
            timelineItems: timeline,
        };
    }, [sessions, currentDeviceId]);

    const loadSessions = useCallback(async (withAnimation = false) => {
        if (!isAuthenticated) {
            setSessions([]);
            setLoadingSessions(false);
            setIsLoading(false);
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
            setIsLoading(false);
            return;
        }

        if (result?.api_response_status === 'success' && Array.isArray(result?.data)) {
            setSessions(result.data);
        } else {
            toast.warning('Unexpected sessions response from server.');
        }

        setLoadingSessions(false);
        setRefreshSpinning(false);
        setIsLoading(false);
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

        // Simulate backend latency (300-500ms)
        await new Promise((resolve) => setTimeout(resolve, 400));

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
        void loadSessions();
    };

    const revokeOthers = async () => {
        if (revokingOthers || activeOtherDevices === 0) return;

        setRevokingOthers(true);
        const previous = [...sessions];
        setSessions(prev => prev.map(s => s.device_id !== currentDeviceId ? {...s, revoked: true} : s));

        // Simulate backend latency (400-600ms)
        await new Promise((resolve) => setTimeout(resolve, 450));

        const result = await apiService.post(API_CONFIG.ENDPOINTS.SESSION_REVOKE_OTHERS, {device_id: currentDeviceId});
        if (apiService.isErrorResponse(result)) {
            setSessions(previous);
            setRevokingOthers(false);
            toast.error(result.message || 'Failed to revoke other sessions.');
            return;
        }

        setRevokingOthers(false);
        toast.success(result?.message || 'Other sessions revoked successfully.');
        void loadSessions();
    };

    const renderSessionCard = (session, {primary = false} = {}) => {
        const fingerprint = buildFingerprint(session);
        const location = formatLocation(session);
        const isCurrentDevice = session.device_id === currentDeviceId;
        const isRevoked = Boolean(session.revoked);
        const isRevoking = revokingIds.has(session.session_id);
        const revokeDisabled = isCurrentDevice || isRevoked || isRevoking;

        return (
            <article
                key={session.session_id}
                className={`scc-card ${primary ? 'scc-card--primary' : ''} ${isRevoked ? 'scc-card--revoked' : ''}`}
            >
                <div className="scc-card__content">
                    <div>
                        <h3 className="scc-device-title">{fingerprint.label}</h3>
                        <p className="scc-device-meta">{location}</p>
                        <p className="scc-device-meta">{formatDateTime(session.last_used_at)}</p>
                    </div>
                </div>

                <div className="scc-card__badges">
                    {isCurrentDevice && <span className="scc-badge scc-badge--current">This Device</span>}
                    <span className={`scc-badge ${isRevoked ? 'scc-badge--revoked' : 'scc-badge--active'}`}>{isRevoked ? 'Revoked' : 'Active'}</span>
                </div>

                <button
                    type="button"
                    className="scc-revoke-btn"
                    onClick={() => revokeSession(session.session_id)}
                    disabled={revokeDisabled}
                >
                    {isRevoking ? <Loader2 size={14} className="db-spin"/> : <Trash2 size={14}/>}
                </button>
            </article>
        );
    };

    return (
        <div className="scc-page scc-page--enter">
            {isLoading && (
                <div className="scc-loading-skeleton">
                    <div className="scc-hero-skeleton">
                        <div className="scc-skeleton scc-skeleton--lg" style={{width: '200px'}}></div>
                        <div className="scc-skeleton scc-skeleton--sm" style={{width: '100px'}}></div>
                    </div>
                    <div className="scc-section-skeleton">
                        <div className="scc-skeleton scc-skeleton--md" style={{width: '120px', marginBottom: '12px'}}></div>
                        <div className="scc-skeleton scc-skeleton--card"></div>
                        <div className="scc-skeleton scc-skeleton--card"></div>
                    </div>
                </div>
            )}

            {!isLoading && (
                <>
                    {/* ── MINI HERO / STATUS SECTION ──────────────────── */}
                    <div className="scc-mini-hero scc-section--enter" style={{'--i': 0}}>
                        <div className="scc-mini-hero__badge">
                            <span className="scc-status-pulse"></span>
                            <span className="scc-status-text">All Sessions Secure</span>
                        </div>
                        <p className="scc-mini-hero__subtitle">Monitor and control all active sessions in real time</p>
                    </div>

                    {/* ── CONTROL HEADER ──────────────────────────────── */}
                    <header className="scc-hero scc-section--enter" style={{'--i': 1}}>
                        <div>
                            <h1 className="scc-hero__title">Session Security</h1>
                            <p className="scc-hero__subtitle">Device management & activity overview</p>
                        </div>
                        <div className="scc-hero__actions">
                            <button
                                type="button"
                                onClick={() => loadSessions(true)}
                                disabled={!isAuthenticated || refreshSpinning || loadingSessions}
                                className={`scc-refresh-btn ${refreshSpinning ? 'loading' : ''}`}
                                aria-label="Refresh sessions"
                                title="Refresh sessions"
                            >
                                {/* Refresh Icon */}
                            </button>
                            <button
                                type="button"
                                onClick={revokeOthers}
                                disabled={!isAuthenticated || activeOtherDevices === 0 || revokingOthers}
                                className="scc-danger-btn"
                            >
                                {revokingOthers ? 'Revoking...' : 'Revoke All Other Devices'}
                            </button>
                        </div>
                    </header>

                    {!loadingSessions && sessions.length === 0 && (
                        <section className="scc-empty scc-section--enter" style={{'--i': 2}} aria-live="polite">
                            <div className="scc-empty__art" aria-hidden>🛡️</div>
                            <h2>No other active sessions. Your account is secure.</h2>
                            <p>Signed in as {user?.identifier || 'account user'}.</p>
                        </section>
                    )}

                    {!!currentSession && (
                        <section className="scc-section scc-section--current scc-section--enter" style={{'--i': 2}}>
                            <div className="scc-section__header">
                                <h2>Your Current Device</h2>
                            </div>
                            {renderSessionCard(currentSession, {primary: true})}
                        </section>
                    )}

                    {activeSessions.length > 0 && (
                        <section className="scc-section scc-section--enter" style={{'--i': 3}}>
                            <div className="scc-section__header">
                                <h2>Active Sessions</h2>
                                <p>{activeSessions.length} active session{activeSessions.length === 1 ? '' : 's'}</p>
                            </div>
                            <div className="scc-grid">
                                {activeSessions
                                    .filter((session) => session.session_id !== currentSession?.session_id)
                                    .map((session, idx) => (
                                        <div key={session.session_id} style={{'--i': idx}} className="scc-card-wrapper">
                                            {renderSessionCard(session)}
                                        </div>
                                    ))}
                            </div>
                        </section>
                    )}

                    {revokedSessions.length > 0 && (
                        <section className="scc-section scc-section--muted scc-section--enter" style={{'--i': 4}}>
                            <button
                                type="button"
                                className={`scc-revoked-toggle ${showRevoked ? 'open' : ''}`}
                                onClick={() => setShowRevoked((prev) => !prev)}
                                aria-expanded={showRevoked}
                            >
                                <div className="scc-revoked-toggle__label">
                                    <span className="scc-revoked-toggle__text">Past / Revoked Sessions</span>
                                    <span className="scc-revoked-count">{revokedSessions.length}</span>
                                </div>
                                <ChevronDown size={20} className="scc-revoked-toggle__icon" />
                            </button>
                            {showRevoked && (
                                <div className="scc-revoked-content">
                                    <div className="scc-grid scc-grid--revoked">
                                        {revokedSessions.map((session) => renderSessionCard(session))}
                                    </div>
                                </div>
                            )}
                        </section>
                    )}

                    <section className="scc-section scc-section--enter" style={{'--i': 5}}>
                        <div className="scc-section__header">
                            <h2>Session Activity Timeline</h2>
                        </div>
                        <ol className="scc-timeline">
                            {timelineItems.map((entry) => (
                                <li key={entry.id} className="scc-timeline__item">
                                    <span className={`scc-timeline__dot ${entry.isRevoked ? 'is-revoked' : 'is-active'} ${!entry.isRevoked ? 'pulse-live' : ''}`} aria-hidden />
                                    <div className="scc-timeline__content">
                                        <div className="scc-timeline__title-row">
                                            <strong>{entry.device}</strong>
                                            <span className={`scc-badge ${entry.isRevoked ? 'scc-badge--revoked' : 'scc-badge--active'}`}>
                                                {entry.isRevoked ? 'Revoked' : 'Active'}
                                            </span>
                                        </div>
                                        <p>{entry.location}</p>
                                        <small>Login time: {formatDateTime(entry.loginTime)}</small>
                                    </div>
                                </li>
                            ))}
                        </ol>
                    </section>
                </>
            )}
        </div>
    );
};

export default SessionsPage;

