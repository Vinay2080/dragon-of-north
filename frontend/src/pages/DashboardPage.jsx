import React, {useEffect, useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../context/authUtils';
import {apiService} from '../services/apiService';
import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device';

const DashboardPage = () => {
    const navigate = useNavigate();
    const {user, logout} = useAuth();

    const [isLoggingOut, setIsLoggingOut] = useState(false);
    const [sessions, setSessions] = useState([]);
    const [loadingSessions, setLoadingSessions] = useState(true);
    const [sessionsError, setSessionsError] = useState('');
    const [actionError, setActionError] = useState('');
    const [actionMessage, setActionMessage] = useState('');

    const currentDeviceId = getDeviceId();

    const sessionStats = useMemo(() => {
        const active = sessions.filter(s => !s.revoked).length;
        const revoked = sessions.filter(s => s.revoked).length;
        return {active, revoked, total: sessions.length};
    }, [sessions]);

    const loadSessions = async () => {
        setLoadingSessions(true);
        setSessionsError('');

        const result = await apiService.get(API_CONFIG.ENDPOINTS.SESSIONS_ALL);
        if (apiService.isErrorResponse(result)) {
            setSessionsError(result.message || 'Failed to load sessions.');
            setLoadingSessions(false);
            return;
        }

        if (result?.api_response_status === 'success' && Array.isArray(result?.data)) {
            setSessions(result.data);
        } else {
            setSessionsError('Unexpected sessions response from server.');
        }

        setLoadingSessions(false);
    };

    useEffect(() => {
        loadSessions();
    }, []);

    const handleLogout = async () => {
        setIsLoggingOut(true);
        try {
            await logout();
            navigate('/', {replace: true});
        } catch (error) {
            console.error('Logout failed:', error);
            navigate('/', {replace: true});
        } finally {
            setIsLoggingOut(false);
        }
    };

    const revokeSession = async (sessionId) => {
        setActionError('');
        setActionMessage('');

        const result = await apiService.delete(API_CONFIG.ENDPOINTS.SESSION_REVOKE(sessionId));
        if (apiService.isErrorResponse(result)) {
            setActionError(result.message || 'Failed to revoke session.');
            return;
        }

        setActionMessage('Session revoked successfully.');
        await loadSessions();
    };

    const revokeOthers = async () => {
        setActionError('');
        setActionMessage('');

        const result = await apiService.post(API_CONFIG.ENDPOINTS.SESSION_REVOKE_OTHERS, {
            device_id: currentDeviceId,
        });

        if (apiService.isErrorResponse(result)) {
            setActionError(result.message || 'Failed to revoke other sessions.');
            return;
        }

        setActionMessage(result?.message || 'Other sessions revoked successfully.');
        await loadSessions();
    };

    return (
        <div className="min-h-screen w-full bg-gradient-to-br from-slate-950 to-slate-900">
            <header className="border-b border-slate-800 bg-slate-950/50 backdrop-blur-sm">
                <div className="mx-auto max-w-7xl px-4 py-4 sm:px-6 lg:px-8">
                    <div className="flex items-center justify-between">
                        <div>
                            <h1 className="text-2xl font-bold text-white">Dashboard</h1>
                            <p className="text-sm text-slate-400">Session-aware authentication center</p>
                        </div>
                        <button
                            onClick={handleLogout}
                            disabled={isLoggingOut}
                            className="rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {isLoggingOut ? 'Logging out...' : 'Logout'}
                        </button>
                    </div>
                </div>
            </header>

            <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 space-y-8">
                <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-2xl">
                    <h2 className="mb-4 text-xl font-semibold text-white">Account Information</h2>
                    <p className="text-slate-300">Identifier: <span className="text-white font-medium">{user?.identifier || 'Not available'}</span></p>
                    <p className="text-slate-300">Current Device ID: <span className="text-white font-mono text-sm">{currentDeviceId}</span></p>
                </div>

                <div className="grid gap-6 md:grid-cols-3">
                    <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-2xl">
                        <h3 className="mb-2 text-sm font-medium text-slate-400">Total Sessions</h3>
                        <p className="text-2xl font-bold text-white">{sessionStats.total}</p>
                    </div>
                    <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-2xl">
                        <h3 className="mb-2 text-sm font-medium text-slate-400">Active Sessions</h3>
                        <p className="text-2xl font-bold text-green-400">{sessionStats.active}</p>
                    </div>
                    <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-2xl">
                        <h3 className="mb-2 text-sm font-medium text-slate-400">Revoked Sessions</h3>
                        <p className="text-2xl font-bold text-yellow-400">{sessionStats.revoked}</p>
                    </div>
                </div>

                <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-2xl">
                    <div className="mb-4 flex flex-wrap gap-3">
                        <button
                            onClick={loadSessions}
                            className="rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm text-white hover:bg-slate-800"
                        >
                            Refresh Sessions
                        </button>
                        <button
                            onClick={revokeOthers}
                            className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-500"
                        >
                            Revoke Other Sessions
                        </button>
                    </div>

                    {actionMessage && <p className="mb-3 text-sm text-green-400">{actionMessage}</p>}
                    {actionError && <p className="mb-3 text-sm text-red-400">{actionError}</p>}
                    {sessionsError && <p className="mb-3 text-sm text-red-400">{sessionsError}</p>}

                    {loadingSessions ? (
                        <p className="text-slate-400">Loading sessions...</p>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="min-w-full text-sm text-left text-slate-300">
                                <thead className="text-xs uppercase text-slate-400 border-b border-slate-800">
                                <tr>
                                    <th className="py-3 pr-4">Device ID</th>
                                    <th className="py-3 pr-4">IP</th>
                                    <th className="py-3 pr-4">Last Used</th>
                                    <th className="py-3 pr-4">Expires</th>
                                    <th className="py-3 pr-4">Status</th>
                                    <th className="py-3">Action</th>
                                </tr>
                                </thead>
                                <tbody>
                                {sessions.map((session) => {
                                    const isCurrentDevice = session.device_id === currentDeviceId;
                                    return (
                                        <tr key={session.session_id} className="border-b border-slate-900">
                                            <td className="py-3 pr-4 font-mono text-xs">
                                                {session.device_id}
                                                {isCurrentDevice && <span className="ml-2 rounded bg-blue-600/20 px-2 py-0.5 text-[10px] text-blue-300">current</span>}
                                            </td>
                                            <td className="py-3 pr-4">{session.ip_address || '-'}</td>
                                            <td className="py-3 pr-4">{session.last_used_at ? new Date(session.last_used_at).toLocaleString() : '-'}</td>
                                            <td className="py-3 pr-4">{session.expiry_date ? new Date(session.expiry_date).toLocaleString() : '-'}</td>
                                            <td className="py-3 pr-4">
                                                {session.revoked ? (
                                                    <span className="text-yellow-400">Revoked</span>
                                                ) : (
                                                    <span className="text-green-400">Active</span>
                                                )}
                                            </td>
                                            <td className="py-3">
                                                <button
                                                    onClick={() => revokeSession(session.session_id)}
                                                    disabled={session.revoked || isCurrentDevice}
                                                    className="rounded border border-slate-700 px-3 py-1 text-xs text-white hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed"
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
