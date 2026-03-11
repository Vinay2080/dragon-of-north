import {useEffect, useMemo, useRef, useState} from 'react';
import DocsLayout from '../components/DocsLayout';

const ACCESS_TTL_SECONDS = 15 * 60;
const EVENT_GAP = 90;

const ACTORS = [
    {id: 'USER', label: 'User', x: 220},
    {id: 'BACKEND', label: 'Backend', x: 680},
    {id: 'ATTACKER', label: 'Attacker', x: 1120},
];

const actorX = (id) => ACTORS.find((a) => a.id === id)?.x ?? 0;
const fmt = (s) => `${String(Math.floor(Math.max(0, s) / 60)).padStart(2, '0')}:${String(Math.max(0, s) % 60).padStart(2, '0')}`;
const tokenId = () => `rt_${Math.random().toString(36).slice(2, 6)}`;

const initialRefresh = {id: 'rt_100a', version: 1, status: 'active', oldRef: '-'};

const SecurityDemoPage = () => {
    const [running, setRunning] = useState(false);
    const [simState, setSimState] = useState('READY');

    const [accessStatus, setAccessStatus] = useState('valid');
    const [accessTtl, setAccessTtl] = useState(ACCESS_TTL_SECONDS);
    const [accessIssuedAt, setAccessIssuedAt] = useState(new Date());
    const [sessionStatus, setSessionStatus] = useState('idle');
    const [refreshToken, setRefreshToken] = useState(initialRefresh);

    const [events, setEvents] = useState([]);
    const [activeEventId, setActiveEventId] = useState(null);
    const [logs, setLogs] = useState(['[boot] authentication trace viewer ready']);

    const eventIdRef = useRef(1);
    const runRef = useRef(0);
    const timelineRef = useRef(null);

    const addLog = (msg) => {
        const ts = new Date().toLocaleTimeString();
        setLogs((prev) => [...prev.slice(-45), `[${ts}] ${msg}`]);
    };

    useEffect(() => {
        if (!timelineRef.current) return;
        timelineRef.current.scrollTo({top: timelineRef.current.scrollHeight, behavior: 'smooth'});
    }, [events.length]);

    const sleep = (ms) => new Promise((res) => setTimeout(res, ms));

    const appendEvent = async (event) => {
        const id = eventIdRef.current;
        eventIdRef.current += 1;
        setEvents((prev) => [...prev, {...event, id}]);
        setActiveEventId(id);
        await sleep(300); // draw animation
        await sleep(400); // readability pause
        setActiveEventId(null);
    };

    const runSequence = async (sequence) => {
        if (running) return;
        const runId = Date.now();
        runRef.current = runId;
        setRunning(true);

        for (const step of sequence) {
            if (runRef.current !== runId) return;
            if (step.state) setSimState(step.state);
            if (step.action) step.action();
            if (step.event) await appendEvent(step.event);
        }

        if (runRef.current === runId) {
            setRunning(false);
        }
    };

    const startSimulation = async () => {
        const oldToken = initialRefresh.id;
        const rotatedToken = tokenId();

        setEvents([]);
        eventIdRef.current = 1;
        setLogs(['[boot] simulation started']);
        setSessionStatus('idle');
        setAccessStatus('valid');
        setAccessTtl(ACCESS_TTL_SECONDS);
        setAccessIssuedAt(new Date());
        setRefreshToken(initialRefresh);

        await runSequence([
            {state: 'SESSION_ACTIVE', action: () => { setSessionStatus('active'); addLog('[Auth] login request initiated'); }},
            {event: {kind: 'message', messageType: 'request', from: 'USER', to: 'BACKEND', label: 'Login Request'}},
            {event: {kind: 'process', actor: 'BACKEND', label: 'Create Session'}},
            {action: () => addLog('[Auth] session created')},

            {event: {kind: 'message', messageType: 'request', from: 'USER', to: 'BACKEND', label: 'API Request (Access Token)'}},
            {event: {kind: 'process', actor: 'BACKEND', label: 'Validate JWT'}},
            {event: {kind: 'process', actor: 'BACKEND', label: 'Check Expiration'}},
            {event: {kind: 'message', messageType: 'response', from: 'BACKEND', to: 'USER', label: '200 OK'}},
            {action: () => addLog('[Request] response 200 OK')},

            {state: 'ACCESS_TOKEN_EXPIRED', action: () => { setAccessStatus('expired'); setAccessTtl(0); addLog('[Security] access token expired'); }},
            {event: {kind: 'message', messageType: 'request', from: 'USER', to: 'BACKEND', label: 'API Request (Expired Token)', color: 'red'}},
            {event: {kind: 'process', actor: 'BACKEND', label: 'Detect Expiration', color: 'red'}},
            {event: {kind: 'message', messageType: 'response', from: 'BACKEND', to: 'USER', label: '401 Unauthorized', color: 'red'}},
            {action: () => addLog('[Request] response 401 Unauthorized')},

            {state: 'REFRESH_FLOW', event: {kind: 'message', messageType: 'request', from: 'USER', to: 'BACKEND', label: 'Refresh Token Request'}},
            {event: {kind: 'process', actor: 'BACKEND', label: 'Validate Refresh Token'}},
            {event: {kind: 'process', actor: 'BACKEND', label: 'Issue New Tokens', color: 'green'}},
            {
                action: () => {
                    setRefreshToken({id: rotatedToken, version: 2, status: 'active', oldRef: oldToken});
                    setAccessStatus('valid');
                    setAccessTtl(ACCESS_TTL_SECONDS);
                    setAccessIssuedAt(new Date());
                    addLog(`[Token] new token pair issued (${oldToken} -> ${rotatedToken})`);
                },
            },
            {event: {kind: 'message', messageType: 'response', from: 'BACKEND', to: 'USER', label: 'New Access Token'}},

            {state: 'TOKEN_REUSE_DETECTED', event: {kind: 'message', messageType: 'attack', from: 'ATTACKER', to: 'BACKEND', label: 'Stolen Refresh Token Reuse', color: 'red'}},
            {event: {kind: 'process', actor: 'BACKEND', label: 'Detect Reuse', color: 'red'}},

            {
                state: 'SESSION_REVOKED',
                action: () => {
                    setSessionStatus('revoked');
                    setAccessStatus('expired');
                    setAccessTtl(0);
                    setRefreshToken((prev) => ({...prev, status: 'revoked'}));
                    addLog('[Security] refresh token reuse detected');
                    addLog('[Session] session revoked');
                },
            },
            {event: {kind: 'message', messageType: 'response', from: 'BACKEND', to: 'ATTACKER', label: 'Blocked', color: 'red'}},
        ]);
    };

    const resetSimulation = () => {
        runRef.current = 0;
        setRunning(false);
        setSimState('READY');
        setSessionStatus('idle');
        setAccessStatus('valid');
        setAccessTtl(ACCESS_TTL_SECONDS);
        setAccessIssuedAt(new Date());
        setRefreshToken(initialRefresh);
        setEvents([]);
        setActiveEventId(null);
        setLogs(['[boot] sequence simulator reset']);
        eventIdRef.current = 1;
    };

    const diagramHeight = Math.max(780, 180 + events.length * EVENT_GAP);

    const strokeColor = (event) => {
        if (event.messageType === 'response' && event.color !== 'red') return '#4ade80';
        if (event.color === 'red') return '#fb7185';
        if (event.color === 'green') return '#4ade80';
        return '#fbbf24';
    };

    const refreshBadge = useMemo(() => (refreshToken.status === 'revoked' ? 'revoked' : 'active'), [refreshToken.status]);

    return (
        <DocsLayout title="Security Demo" subtitle="Automated authentication trace viewer for session creation, token expiry, refresh, and reuse defense.">
            <section className="space-y-4 rounded-2xl border border-white/10 bg-white/[0.03] p-5">
                <div className="grid gap-3 md:grid-cols-[1fr_auto_auto] md:items-center">
                    <div className="rounded-xl border border-cyan-300/25 bg-cyan-300/5 p-4">
                        <p className="text-sm font-semibold text-cyan-100">Current State</p>
                        <p className="font-mono text-lg text-cyan-200">{simState}</p>
                    </div>
                    <button
                        type="button"
                        disabled={running}
                        onClick={startSimulation}
                        className="rounded-lg border border-cyan-300/60 bg-cyan-400/10 px-5 py-3 text-sm font-semibold text-cyan-100 disabled:opacity-50"
                    >
                        {running ? 'Running Simulation...' : 'Start Simulation'}
                    </button>
                    <button type="button" onClick={resetSimulation} className="rounded-lg border border-white/20 bg-white/5 px-5 py-3 text-sm">Reset</button>
                </div>

                <div className="rounded-2xl border border-white/10 bg-gradient-to-br from-[#0d1326] to-[#101b35] p-4" style={{minHeight: 780}}>
                    <div ref={timelineRef} className="max-h-[900px] overflow-auto rounded-xl border border-white/10 bg-black/15 p-2">
                        <svg viewBox={`0 0 1300 ${diagramHeight}`} className="h-[820px] min-h-[820px] w-full min-w-[980px]">
                            {ACTORS.map((actor) => (
                                <g key={actor.id}>
                                    <rect x={actor.x - 80} y={20} width="160" height="50" rx="12" className="fill-slate-900/90 stroke-slate-500" />
                                    <text x={actor.x} y={52} textAnchor="middle" className="fill-slate-100 text-[17px] font-semibold">{actor.label}</text>
                                    <line x1={actor.x} y1={70} x2={actor.x} y2={diagramHeight - 30} className="stroke-slate-500" strokeDasharray="8 8" />
                                </g>
                            ))}

                            {events.map((event, index) => {
                                const y = 112 + index * EVENT_GAP;
                                const isActive = activeEventId === event.id;

                                if (event.kind === 'process') {
                                    const x = actorX(event.actor);
                                    return (
                                        <g key={event.id}>
                                            <rect
                                                x={x - 120}
                                                y={y - 22}
                                                width="240"
                                                height="44"
                                                rx="8"
                                                className={isActive ? 'fill-cyan-400/20 stroke-cyan-300' : event.color === 'red' ? 'fill-rose-400/15 stroke-rose-300' : 'fill-slate-800/80 stroke-slate-400'}
                                                style={{transition: 'all 220ms ease'}}
                                            />
                                            <text x={x} y={y + 8} textAnchor="middle" className="fill-slate-100 text-[15px]">[{event.label}]</text>
                                        </g>
                                    );
                                }

                                const startX = actorX(event.from);
                                const endX = actorX(event.to);
                                const isResponse = event.messageType === 'response';
                                const marker = startX < endX ? 'url(#arrowRight)' : 'url(#arrowLeft)';

                                return (
                                    <g key={event.id}>
                                        <line
                                            x1={startX}
                                            y1={y}
                                            x2={endX}
                                            y2={y}
                                            stroke={strokeColor(event)}
                                            strokeWidth="3"
                                            strokeDasharray={isResponse ? '9 8' : isActive ? '260' : '0'}
                                            strokeDashoffset={isResponse ? '0' : isActive ? '260' : '0'}
                                            markerEnd={marker}
                                        >
                                            {!isResponse && isActive ? <animate attributeName="stroke-dashoffset" from="260" to="0" dur="0.3s" fill="freeze" /> : null}
                                        </line>
                                        <text x={(startX + endX) / 2} y={y - 10} textAnchor="middle" className="fill-slate-200 text-[15px]">{event.label}</text>
                                    </g>
                                );
                            })}

                            <defs>
                                <marker id="arrowRight" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto">
                                    <path d="M0,0 L10,4 L0,8 z" fill="#e2e8f0" />
                                </marker>
                                <marker id="arrowLeft" markerWidth="10" markerHeight="8" refX="1" refY="4" orient="auto">
                                    <path d="M10,0 L0,4 L10,8 z" fill="#e2e8f0" />
                                </marker>
                            </defs>
                        </svg>
                    </div>
                </div>

                <div className="grid gap-4 xl:grid-cols-2">
                    <section className="rounded-xl border border-white/10 bg-white/[0.02] p-4 text-sm text-slate-300">
                        <h4 className="mb-2 font-semibold">Session State</h4>
                        <p>Session: <span className={sessionStatus === 'active' ? 'text-emerald-300' : sessionStatus === 'revoked' ? 'text-rose-300' : 'text-slate-300'}>{sessionStatus}</span></p>
                        <p>Access token: <span className={accessStatus === 'valid' ? 'text-emerald-300' : 'text-rose-300'}>{accessStatus}</span></p>
                        <p>TTL: <span className="font-mono text-cyan-200">{fmt(accessTtl)}</span></p>
                        <p>Issued: {accessIssuedAt.toLocaleTimeString()}</p>
                        <p>Refresh token: <span className={refreshBadge === 'active' ? 'text-emerald-300' : 'text-rose-300'}>{refreshBadge}</span></p>
                        <p>Refresh id: {refreshToken.id}</p>
                        <p>Refresh version: v{refreshToken.version}</p>
                        <p>Old token ref: {refreshToken.oldRef}</p>
                    </section>

                    <section className="rounded-xl border border-white/10 bg-black/45 p-4">
                        <h4 className="mb-2 font-semibold">Security Log Console</h4>
                        <div className="h-52 overflow-auto rounded-lg border border-white/10 bg-black/55 p-3 font-mono text-xs text-green-300">
                            {logs.map((line, idx) => <p key={`${idx}-${line}`}>{line}</p>)}
                        </div>
                    </section>
                </div>
            </section>
        </DocsLayout>
    );
};

export default SecurityDemoPage;
