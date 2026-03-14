import {useEffect, useMemo, useRef, useState} from 'react';
import DocsLayout from '../components/DocsLayout';

const ACCESS_TTL_SECONDS = 15 * 60;
const EVENT_GAP = 90;
const ARROW_DRAW_MS = 1100;

const ACTORS = [
    {id: 'USER', label: 'User', x: 220},
    {id: 'BACKEND', label: 'Backend', x: 680},
    {id: 'ATTACKER', label: 'Attacker', x: 1120},
];

const SIMULATION_STEPS = [
    'LOGIN_REQUEST',
    'SESSION_CREATED',
    'API_CALL',
    'TOKEN_EXPIRED',
    'REFRESH_REQUEST',
    'NEW_TOKEN',
    'ATTACK_ATTEMPT',
];

const initialRefresh = {id: 'rt_100a', version: 1, status: 'active', oldRef: '-'};

const actorX = (id) => ACTORS.find((a) => a.id === id)?.x ?? 0;
const fmt = (s) => `${String(Math.floor(Math.max(0, s) / 60)).padStart(2, '0')}:${String(Math.max(0, s) % 60).padStart(2, '0')}`;
const tokenId = () => `rt_${Math.random().toString(36).slice(2, 6)}`;

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
        timelineRef.current.scrollTop = timelineRef.current.scrollHeight;
    }, [events.length]);

    const sleep = (ms) => new Promise((res) => setTimeout(res, ms));

    const appendEvent = async (event, drawMs = 380, pauseMs = 320) => {
        const id = eventIdRef.current;
        eventIdRef.current += 1;
        setEvents((prev) => [...prev, {...event, id}]);
        setActiveEventId(id);
        await sleep(drawMs);
        await sleep(pauseMs);
        setActiveEventId(null);
    };

    const animateArrow = async ({from, to, label, tone = 'request', messageType = 'request'}) => {
        await appendEvent({kind: 'message', messageType, from, to, label, tone}, ARROW_DRAW_MS, 240);
    };

    const showBackendStep = async ({label, tone = 'security'}) => {
        await appendEvent({kind: 'process', actor: 'BACKEND', label, tone}, 300, 240);
    };

    const runSimulationStep = async (step, ctx) => {
        switch (step) {
            case 'LOGIN_REQUEST':
                setSimState('SESSION_ACTIVE');
                setSessionStatus('active');
                addLog('[Auth] login request initiated');
                await animateArrow({from: 'USER', to: 'BACKEND', label: 'Login Request', tone: 'request'});
                await showBackendStep({label: 'Create Session', tone: 'security'});
                await animateArrow({from: 'BACKEND', to: 'USER', label: '200 OK', tone: 'response', messageType: 'response'});
                addLog('[Auth] session created');
                break;
            case 'SESSION_CREATED':
                await animateArrow({from: 'USER', to: 'BACKEND', label: 'API Request (Access Token)', tone: 'request'});
                await showBackendStep({label: 'Validate JWT', tone: 'security'});
                await showBackendStep({label: 'Check Expiration', tone: 'security'});
                await animateArrow({from: 'BACKEND', to: 'USER', label: '200 OK', tone: 'response', messageType: 'response'});
                addLog('[Request] response 200 OK');
                break;
            case 'API_CALL':
                setSimState('ACCESS_TOKEN_EXPIRED');
                setAccessStatus('expired');
                setAccessTtl(0);
                addLog('[Security] access token expired');
                await animateArrow({from: 'USER', to: 'BACKEND', label: 'API Request (Expired Token)', tone: 'security'});
                await showBackendStep({label: 'Detect Expiration', tone: 'security'});
                await animateArrow({from: 'BACKEND', to: 'USER', label: '401 Unauthorized', tone: 'security', messageType: 'response'});
                addLog('[Request] response 401 Unauthorized');
                break;
            case 'TOKEN_EXPIRED':
                setSimState('REFRESH_FLOW');
                await animateArrow({from: 'USER', to: 'BACKEND', label: 'Refresh Token Request', tone: 'request'});
                await showBackendStep({label: 'Validate Refresh Token', tone: 'security'});
                break;
            case 'REFRESH_REQUEST':
                await showBackendStep({label: 'Issue New Tokens', tone: 'response'});
                setRefreshToken({id: ctx.rotatedToken, version: 2, status: 'active', oldRef: ctx.oldToken});
                setAccessStatus('valid');
                setAccessTtl(ACCESS_TTL_SECONDS);
                setAccessIssuedAt(new Date());
                addLog(`[Token] new token pair issued (${ctx.oldToken} -> ${ctx.rotatedToken})`);
                break;
            case 'NEW_TOKEN':
                await animateArrow({from: 'BACKEND', to: 'USER', label: 'New Access Token', tone: 'response', messageType: 'response'});
                break;
            case 'ATTACK_ATTEMPT':
                setSimState('TOKEN_REUSE_DETECTED');
                await animateArrow({from: 'ATTACKER', to: 'BACKEND', label: 'Stolen Refresh Token Reuse', tone: 'attack', messageType: 'attack'});
                await showBackendStep({label: 'Detect Reuse', tone: 'attack'});

                setSimState('SESSION_REVOKED');
                setSessionStatus('revoked');
                setAccessStatus('expired');
                setAccessTtl(0);
                setRefreshToken((prev) => ({...prev, status: 'revoked'}));
                addLog('[Security] refresh token reuse detected');
                addLog('[Session] session revoked');

                await animateArrow({from: 'BACKEND', to: 'ATTACKER', label: 'Blocked', tone: 'attack', messageType: 'response'});
                break;
            default:
                break;
        }
    };

    const startSimulation = async () => {
        if (running) return;

        const runId = Date.now();
        runRef.current = runId;
        setRunning(true);

        const oldToken = initialRefresh.id;
        const rotatedToken = tokenId();

        setEvents([]);
        eventIdRef.current = 1;
        setLogs(['[boot] simulation started']);
        setSimState('READY');
        setSessionStatus('idle');
        setAccessStatus('valid');
        setAccessTtl(ACCESS_TTL_SECONDS);
        setAccessIssuedAt(new Date());
        setRefreshToken(initialRefresh);

        const context = {oldToken, rotatedToken};

        for (const step of SIMULATION_STEPS) {
            if (runRef.current !== runId) return;
            await runSimulationStep(step, context);
        }

        if (runRef.current === runId) {
            setRunning(false);
        }
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

    const diagramHeight = Math.max(800, 180 + events.length * EVENT_GAP);

    const strokeColor = (event) => {
        if (event.tone === 'response') return '#22c55e';
        if (event.tone === 'security') return '#fb923c';
        if (event.tone === 'attack') return '#ef4444';
        return '#facc15';
    };

    const refreshBadge = useMemo(() => (refreshToken.status === 'revoked' ? 'revoked' : 'active'), [refreshToken.status]);

    return (
        <DocsLayout title="Security Demo" subtitle="Automated authentication trace viewer for session creation, token expiry, refresh, and reuse defense.">
            <style>{`
                .simulation-frame {
                    height: 600px;
                    overflow-y: auto;
                    overflow-x: hidden;
                    position: relative;
                }
                .simulation-canvas {
                    min-height: 800px;
                    width: 100%;
                }
                .arrow-line {
                    stroke-dasharray: 460;
                    stroke-dashoffset: 460;
                    animation: drawLine 1.1s cubic-bezier(0.22, 1, 0.36, 1) forwards;
                }
                .arrow-head {
                    opacity: 0;
                    animation: showHead 0.2s 1.05s forwards;
                }
                .packet {
                    filter: drop-shadow(0 0 5px cyan);
                }
                @keyframes drawLine {
                    to {
                        stroke-dashoffset: 0;
                    }
                }
                @keyframes showHead {
                    to {
                        opacity: 1;
                    }
                }
            `}</style>

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
                    <div ref={timelineRef} className="simulation-frame rounded-xl border border-white/10 bg-black/15 p-2">
                        <svg width="100%" height={diagramHeight} className="simulation-canvas min-w-[980px]">
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
                                    const processClass = event.tone === 'attack'
                                        ? 'fill-rose-400/15 stroke-rose-300'
                                        : event.tone === 'response'
                                            ? 'fill-emerald-400/15 stroke-emerald-300'
                                            : event.tone === 'security'
                                                ? 'fill-orange-400/15 stroke-orange-300'
                                                : 'fill-slate-800/80 stroke-slate-400';

                                    return (
                                        <g key={event.id}>
                                            <rect
                                                x={x - 120}
                                                y={y - 22}
                                                width="240"
                                                height="44"
                                                rx="8"
                                                className={isActive ? 'fill-cyan-400/20 stroke-cyan-300' : processClass}
                                                style={{transition: 'all 220ms ease'}}
                                            />
                                            <text x={x} y={y + 8} textAnchor="middle" className="fill-slate-100 text-[15px]">[{event.label}]</text>
                                        </g>
                                    );
                                }

                                const startX = actorX(event.from);
                                const endX = actorX(event.to);
                                const direction = startX < endX ? 'right' : 'left';
                                const dx = direction === 'right' ? -12 : 12;
                                const staticMarkerEnd = !isActive && direction === 'right' ? 'url(#arrowRight)' : undefined;
                                const staticMarkerStart = !isActive && direction === 'left' ? 'url(#arrowLeft)' : undefined;
                                const headPoints = direction === 'right'
                                    ? `${endX},${y} ${endX - 12},${y - 6} ${endX - 12},${y + 6}`
                                    : `${endX},${y} ${endX + 12},${y - 6} ${endX + 12},${y + 6}`;

                                return (
                                    <g key={event.id}>
                                        <line
                                            x1={startX}
                                            y1={y}
                                            x2={endX}
                                            y2={y}
                                            className={isActive ? 'arrow-line' : undefined}
                                            stroke={strokeColor(event)}
                                            strokeWidth="3"
                                            markerEnd={staticMarkerEnd}
                                            markerStart={staticMarkerStart}
                                        />
                                        {isActive ? (
                                            <>
                                                <polygon points={headPoints} fill={strokeColor(event)} className="arrow-head" />
                                                <circle r="4" fill="#22d3ee" className="packet">
                                                    <animateMotion dur="1.1s" fill="freeze" path={`M ${startX + dx} ${y} L ${endX - dx} ${y}`} />
                                                </circle>
                                            </>
                                        ) : null}
                                        <text x={(startX + endX) / 2} y={y - 10} textAnchor="middle" className="fill-slate-200 text-[15px]">{event.label}</text>
                                    </g>
                                );
                            })}

                            <defs>
                                <marker id="arrowRight" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto">
                                    <path d="M0,0 L12,6 L0,12 z" fill="#e2e8f0" />
                                </marker>
                                <marker id="arrowLeft" markerWidth="12" markerHeight="12" refX="2" refY="6" orient="auto-start-reverse">
                                    <path d="M12,0 L0,6 L12,12 z" fill="#e2e8f0" />
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
