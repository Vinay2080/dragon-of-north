import {useEffect, useMemo, useRef, useState} from 'react';
import DocsLayout from '../components/DocsLayout';

const ACCESS_TTL_SECONDS = 15 * 60;
const EVENT_GAP = 90;
const ARROW_DRAW_MS = 1100;
const CANVAS_WIDTH = 900;
const MAX_X = CANVAS_WIDTH - 40;

const ACTORS = [
    {id: 'USER', label: 'User', x: 200},
    {id: 'BACKEND', label: 'Backend', x: 450},
    {id: 'ATTACKER', label: 'Attacker', x: 700},
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

const clampX = (value) => Math.max(40, Math.min(value, MAX_X));
const actorX = (id) => clampX(ACTORS.find((a) => a.id === id)?.x ?? 0);
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
    const simulationStartedRef = useRef(false);

    const addLog = (msg) => {
        const ts = new Date().toLocaleTimeString();
        setLogs((prev) => [...prev.slice(-45), `[${ts}] ${msg}`]);
    };

    useEffect(() => {
        const container = timelineRef.current;
        if (!container || events.length === 0) return;

        if (!simulationStartedRef.current) {
            simulationStartedRef.current = true;
            return;
        }

        const contentHeight = container.scrollHeight;
        const viewportHeight = container.clientHeight;

        if (contentHeight > viewportHeight) {
            container.scrollTo({
                top: container.scrollHeight,
                behavior: 'smooth',
            });
        }
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

        simulationStartedRef.current = false;
        if (timelineRef.current) {
            timelineRef.current.scrollTop = 0;
        }

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
        simulationStartedRef.current = false;
        if (timelineRef.current) {
            timelineRef.current.scrollTop = 0;
        }
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
        if (event.tone === 'response') return 'var(--don-success)';
        if (event.tone === 'security') return 'var(--don-warning)';
        if (event.tone === 'attack') return 'var(--don-danger)';
        return 'var(--don-accent)';
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
                    width: 900px;
                    min-height: 800px;
                    padding-top: 50px;
                    margin: 0 auto;
                    position: relative;
                    display: block;
                }
                .actor-header-row {
                    position: sticky;
                    top: 0;
                    z-index: 10;
                    background: inherit;
                    padding-top: 8px;
                    padding-bottom: 8px;
                    width: 900px;
                    margin: 0 auto;
                }
                .actor-header-card {
                    position: absolute;
                    width: 150px;
                    height: 50px;
                    transform: translateX(-50%);
                }
                .arrow-line {
                    stroke-dasharray: 260;
                    stroke-dashoffset: 260;
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

            <section className="space-y-4 dashboard-card p-5">
                <div className="grid gap-3 md:grid-cols-[1fr_auto_auto] md:items-center">
                    <div className="rounded-xl p-4"
                         style={{border: '1px solid var(--don-accent-border)', background: 'var(--don-accent-dim)'}}>
                        <p className="text-sm font-semibold" style={{color: 'var(--don-accent-text)'}}>Current State</p>
                        <p className="font-mono text-lg" style={{color: 'var(--don-accent-text)'}}>{simState}</p>
                    </div>
                    <button
                        type="button"
                        disabled={running}
                        onClick={startSimulation}
                        className="btn-primary px-5 py-3 text-sm font-semibold disabled:opacity-50"
                    >
                        {running ? 'Running Simulation...' : 'Start Simulation'}
                    </button>
                    <button type="button" onClick={resetSimulation} className="btn-subtle px-5 py-3 text-sm">Reset
                    </button>
                </div>

                <div className="rounded-2xl p-4" style={{
                    minHeight: 780,
                    border: '1px solid var(--don-border-default)',
                    background: 'var(--don-bg-card)'
                }}>
                    <div ref={timelineRef} className="simulation-frame rounded-xl p-2" style={{
                        border: '1px solid var(--don-border-subtle)',
                        background: 'color-mix(in srgb, var(--don-bg-base) 22%, transparent)'
                    }}>
                        <div className="actor-header-row">
                            {ACTORS.map((actor) => (
                                <div
                                    key={actor.id}
                                    className="actor-header-card rounded-xl"
                                    style={{
                                        left: actor.x,
                                        top: 0,
                                        border: '1px solid var(--don-border-default)',
                                        background: 'var(--don-bg-surface)'
                                    }}
                                >
                                    <p className="pt-3 text-center text-[17px] font-semibold"
                                       style={{color: 'var(--don-text-primary)'}}>{actor.label}</p>
                                </div>
                            ))}
                            <div style={{height: 66}} />
                        </div>

                        <svg width={CANVAS_WIDTH} height={diagramHeight} className="simulation-canvas">
                            {ACTORS.map((actor) => (
                                <g key={actor.id}>
                                    <line x1={actor.x} y1={70} x2={actor.x} y2={diagramHeight - 30}
                                          stroke="var(--don-border-strong)" strokeDasharray="8 8"/>
                                </g>
                            ))}

                            {events.map((event, index) => {
                                const y = 112 + index * EVENT_GAP;
                                const isActive = activeEventId === event.id;

                                if (event.kind === 'process') {
                                    const x = actorX(event.actor);
                                    const processStyle = event.tone === 'attack'
                                        ? {
                                            fill: 'color-mix(in srgb, var(--don-danger) 15%, transparent)',
                                            stroke: 'color-mix(in srgb, var(--don-danger) 68%, transparent)'
                                        }
                                        : event.tone === 'response'
                                            ? {
                                                fill: 'color-mix(in srgb, var(--don-success) 14%, transparent)',
                                                stroke: 'color-mix(in srgb, var(--don-success) 65%, transparent)'
                                            }
                                            : event.tone === 'security'
                                                ? {
                                                    fill: 'color-mix(in srgb, var(--don-warning) 16%, transparent)',
                                                    stroke: 'color-mix(in srgb, var(--don-warning) 70%, transparent)'
                                                }
                                                : {
                                                    fill: 'color-mix(in srgb, var(--don-bg-surface) 85%, transparent)',
                                                    stroke: 'var(--don-border-strong)'
                                                };

                                    return (
                                        <g key={event.id}>
                                            <rect
                                                x={x - 110}
                                                y={y - 22}
                                                width="220"
                                                height="44"
                                                rx="8"
                                                style={isActive
                                                    ? {
                                                        fill: 'var(--don-accent-dim)',
                                                        stroke: 'var(--don-accent-border)',
                                                        transition: 'all 220ms ease'
                                                    }
                                                    : {...processStyle, transition: 'all 220ms ease'}}
                                            />
                                            <text x={x} y={y + 8} textAnchor="middle" fill="var(--don-text-primary)"
                                                  className="text-[15px]">[{event.label}]
                                            </text>
                                        </g>
                                    );
                                }

                                const startX = clampX(actorX(event.from));
                                const endX = clampX(actorX(event.to));
                                const direction = startX < endX ? 'right' : 'left';
                                const arrowColor = strokeColor(event);
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
                                            stroke={arrowColor}
                                            strokeWidth="3"
                                        />
                                        <polygon points={headPoints} fill={arrowColor} className={isActive ? 'arrow-head' : undefined} />
                                        {isActive ? (
                                            <circle r="4" fill="var(--don-info)" className="packet">
                                                <animateMotion dur="1.1s" fill="freeze" path={`M ${startX} ${y} L ${endX} ${y}`} />
                                            </circle>
                                        ) : null}
                                        <text x={(startX + endX) / 2} y={y - 10} textAnchor="middle"
                                              fill="var(--don-text-secondary)"
                                              className="text-[15px]">{event.label}</text>
                                    </g>
                                );
                            })}
                        </svg>
                    </div>
                </div>

                <div className="grid gap-4 xl:grid-cols-2">
                    <section className="rounded-xl border p-4 text-sm" style={{
                        borderColor: 'var(--don-border-default)',
                        background: 'color-mix(in srgb, var(--don-bg-surface) 90%, transparent)',
                        color: 'var(--don-text-secondary)'
                    }}>
                        <h4 className="mb-2 font-semibold">Session State</h4>
                        <p>Session: <span
                            style={{color: sessionStatus === 'active' ? 'var(--don-success)' : sessionStatus === 'revoked' ? 'var(--don-danger)' : 'var(--don-text-secondary)'}}>{sessionStatus}</span>
                        </p>
                        <p>Access token: <span
                            style={{color: accessStatus === 'valid' ? 'var(--don-success)' : 'var(--don-danger)'}}>{accessStatus}</span>
                        </p>
                        <p>TTL: <span className="font-mono"
                                      style={{color: 'var(--don-accent-text)'}}>{fmt(accessTtl)}</span></p>
                        <p>Issued: {accessIssuedAt.toLocaleTimeString()}</p>
                        <p>Refresh token: <span
                            style={{color: refreshBadge === 'active' ? 'var(--don-success)' : 'var(--don-danger)'}}>{refreshBadge}</span>
                        </p>
                        <p>Refresh id: {refreshToken.id}</p>
                        <p>Refresh version: v{refreshToken.version}</p>
                        <p>Old token ref: {refreshToken.oldRef}</p>
                    </section>

                    <section className="rounded-xl border p-4" style={{
                        borderColor: 'var(--don-border-default)',
                        background: 'color-mix(in srgb, var(--don-bg-base) 35%, transparent)'
                    }}>
                        <h4 className="mb-2 font-semibold">Security Log Console</h4>
                        <div className="h-52 overflow-auto rounded-lg border p-3 font-mono text-xs" style={{
                            borderColor: 'var(--don-border-subtle)',
                            background: 'color-mix(in srgb, var(--don-bg-base) 55%, transparent)',
                            color: 'var(--don-success)'
                        }}>
                            {logs.map((line, idx) => <p key={`${idx}-${line}`}>{line}</p>)}
                        </div>
                    </section>
                </div>
            </section>
        </DocsLayout>
    );
};

export default SecurityDemoPage;
