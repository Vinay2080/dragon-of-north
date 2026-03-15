import {useEffect, useMemo, useRef, useState} from 'react';
import DocsLayout from '../components/DocsLayout';

const EVENT_GAP = 90;
const ARROW_DRAW_MS = 1100;
const CANVAS_WIDTH = 920;

const ACTORS = [
    {id: 'USER', label: 'User', x: 160},
    {id: 'FRONTEND', label: 'Frontend', x: 360},
    {id: 'BACKEND', label: 'Backend API', x: 560},
    {id: 'DATABASE', label: 'PostgreSQL', x: 760},
];

const FLOW_STEPS = [
    'IDENTIFIER_INPUT',
    'API_REQUEST',
    'DB_LOOKUP',
    'DB_RESPONSE',
    'STATUS_EVALUATION',
    'NEXT_ACTION',
    'FRONTEND_ROUTE',
];

const SCENARIO_HELPERS = [
    {
        identifier: 'new@domain.com',
        notes: ['user does not exist', 'SIGNUP_REQUIRED'],
    },
    {
        identifier: 'unverified@domain.com',
        notes: ['user exists but email_verified=false', 'EMAIL_VERIFICATION_REQUIRED'],
    },
    {
        identifier: 'active@domain.com',
        notes: ['user exists and email_verified=true', 'LOGIN_ALLOWED'],
    },
];

const mockResultForIdentifier = (rawIdentifier) => {
    const identifier = rawIdentifier.trim().toLowerCase();
    if (identifier === 'new@domain.com') {
        return {exists: false, status: 'NOT_EXISTS', email_verified: false, providers: [], next_action: 'SIGNUP'};
    }
    if (identifier === 'unverified@domain.com') {
        return {exists: true, status: 'ACTIVE', email_verified: false, providers: ['LOCAL'], next_action: 'EMAIL_VERIFICATION'};
    }
    return {exists: true, status: 'ACTIVE', email_verified: true, providers: ['LOCAL'], next_action: 'LOGIN'};
};

const getIdentifierType = (value) => (value.includes('@') ? 'EMAIL' : 'PHONE');
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const actorX = (id) => ACTORS.find((actor) => actor.id === id)?.x ?? 0;

const toneColor = (tone) => {
    if (tone === 'response') return '#22c55e';
    if (tone === 'decision') return '#fb923c';
    if (tone === 'error') return '#ef4444';
    return '#facc15';
};

const IdentifierFlowPage = () => {
    const [identifier, setIdentifier] = useState('unverified@domain.com');
    const [running, setRunning] = useState(false);
    const [authState, setAuthState] = useState('READY');
    const [events, setEvents] = useState([]);
    const [activeEventId, setActiveEventId] = useState(null);
    const [logs, setLogs] = useState(['[boot] identifier workflow simulator ready']);
    const [result, setResult] = useState(mockResultForIdentifier('unverified@domain.com'));

    const eventIdRef = useRef(1);
    const runRef = useRef(0);
    const timelineRef = useRef(null);
    const simulationStartedRef = useRef(false);

    const addLog = (message) => {
        const ts = new Date().toLocaleTimeString();
        setLogs((prev) => [...prev.slice(-45), `[${ts}] ${message}`]);
    };

    useEffect(() => {
        const container = timelineRef.current;
        if (!container || events.length === 0) return;

        if (!simulationStartedRef.current) {
            simulationStartedRef.current = true;
            return;
        }

        if (container.scrollHeight > container.clientHeight) {
            container.scrollTo({top: container.scrollHeight, behavior: 'smooth'});
        }
    }, [events.length]);

    const appendEvent = async (event, drawMs = 380, pauseMs = 320) => {
        const id = eventIdRef.current;
        eventIdRef.current += 1;
        setEvents((prev) => [...prev, {...event, id}]);
        setActiveEventId(id);
        await sleep(drawMs);
        await sleep(pauseMs);
        setActiveEventId(null);
    };

    const animateArrow = async ({from, to, label, tone = 'request'}) => {
        await appendEvent({kind: 'message', from, to, label, tone}, ARROW_DRAW_MS, 240);
    };

    const showBackendStep = async ({label, actor = 'BACKEND', tone = 'decision'}) => {
        await appendEvent({kind: 'process', actor, label, tone}, 300, 240);
    };

    const runStep = async (step, context) => {
        switch (step) {
            case 'IDENTIFIER_INPUT':
                setAuthState('IDENTIFIER_CHECK');
                addLog('[Auth] identifier submitted');
                addLog(`[Client] detected type: ${getIdentifierType(context.identifier)}`);
                await animateArrow({from: 'USER', to: 'FRONTEND', label: 'Enter Email / Phone', tone: 'request'});
                break;
            case 'API_REQUEST':
                addLog('[Frontend] sending identifier status request');
                await animateArrow({from: 'FRONTEND', to: 'BACKEND', label: 'POST /api/v1/auth/identifier/status', tone: 'request'});
                break;
            case 'DB_LOOKUP':
                addLog('[DB] searching for existing account');
                await animateArrow({from: 'BACKEND', to: 'DATABASE', label: 'Lookup user by identifier', tone: 'request'});
                await showBackendStep({label: 'Query User Table', actor: 'BACKEND', tone: 'decision'});
                break;
            case 'DB_RESPONSE':
                setAuthState(context.response.exists ? 'USER_FOUND' : 'SIGNUP_REQUIRED');
                addLog(context.response.exists ? '[DB] user found' : '[DB] user not found');
                addLog(`status=${context.response.status}`);
                addLog(`email_verified=${String(context.response.email_verified)}`);
                await animateArrow({from: 'DATABASE', to: 'BACKEND', label: 'User Record', tone: 'response'});
                break;
            case 'STATUS_EVALUATION':
                addLog('[Auth] evaluating routing rules');
                await showBackendStep({label: 'Check Account Status'});
                await showBackendStep({label: 'Check Email Verification'});
                await showBackendStep({label: 'Determine Next Step'});
                break;
            case 'NEXT_ACTION':
                addLog('[Backend] response returned');
                addLog(`next_action=${context.response.next_action}`);
                await animateArrow({from: 'BACKEND', to: 'FRONTEND', label: `200 OK (${context.response.next_action})`, tone: 'response'});
                break;
            case 'FRONTEND_ROUTE': {
                const routeLabel = context.response.next_action === 'SIGNUP'
                    ? 'Redirect to Signup'
                    : context.response.next_action === 'LOGIN'
                        ? 'Redirect to Login'
                        : 'Redirect to Email Verification';
                const state = context.response.next_action === 'SIGNUP'
                    ? 'SIGNUP_REQUIRED'
                    : context.response.next_action === 'LOGIN'
                        ? 'LOGIN_ALLOWED'
                        : 'EMAIL_VERIFICATION_REQUIRED';
                setAuthState(state);
                addLog(`[Frontend] routing user to ${routeLabel.replace('Redirect to ', '')} page`);
                await animateArrow({from: 'FRONTEND', to: 'USER', label: routeLabel, tone: 'decision'});
                break;
            }
            default:
                break;
        }
    };

    const startSimulation = async () => {
        if (running) return;

        const runId = Date.now();
        runRef.current = runId;
        setRunning(true);

        const response = mockResultForIdentifier(identifier);
        setResult(response);
        setEvents([]);
        eventIdRef.current = 1;
        simulationStartedRef.current = false;
        if (timelineRef.current) timelineRef.current.scrollTop = 0;
        setLogs(['[boot] identifier workflow simulation started']);
        setAuthState('READY');

        const context = {identifier: identifier.trim(), response};
        for (const step of FLOW_STEPS) {
            if (runRef.current !== runId) return;
            await runStep(step, context);
        }

        if (runRef.current === runId) setRunning(false);
    };

    const resetSimulation = () => {
        runRef.current = 0;
        setRunning(false);
        setAuthState('READY');
        setEvents([]);
        setActiveEventId(null);
        setLogs(['[boot] identifier workflow simulator reset']);
        eventIdRef.current = 1;
        simulationStartedRef.current = false;
        if (timelineRef.current) timelineRef.current.scrollTop = 0;
    };

    const diagramHeight = useMemo(() => Math.max(780, 180 + events.length * EVENT_GAP), [events.length]);

    return (
        <DocsLayout title="Identifier Flow" subtitle="Authentication identifier status protocol trace viewer using timeline sequence animation.">
            <style>{`
                .simulation-frame { height: 600px; overflow-y: auto; overflow-x: hidden; position: relative; }
                .simulation-canvas { width: 920px; min-height: 780px; padding-top: 50px; margin: 0 auto; position: relative; display: block; }
                .actor-header-row { position: sticky; top: 0; z-index: 10; background: inherit; padding-top: 8px; padding-bottom: 8px; width: 920px; margin: 0 auto; }
                .actor-header-card { position: absolute; width: 150px; height: 50px; transform: translateX(-50%); }
                .arrow-line { stroke-dasharray: 260; stroke-dashoffset: 260; animation: drawLine 1.1s cubic-bezier(0.22,1,0.36,1) forwards; }
                .arrow-head { opacity: 0; animation: showHead 0.2s 1.05s forwards; }
                .packet { filter: drop-shadow(0 0 5px cyan); }
                @keyframes drawLine { to { stroke-dashoffset: 0; } }
                @keyframes showHead { to { opacity: 1; } }
            `}</style>

            <section className="space-y-4 rounded-2xl border border-white/10 bg-white/[0.03] p-5">
                <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto_auto] md:items-center">
                    <div className="rounded-xl border border-cyan-300/25 bg-cyan-300/5 p-4">
                        <p className="text-sm font-semibold text-cyan-100">Current Authentication State</p>
                        <p className="font-mono text-lg text-cyan-200">{authState}</p>
                    </div>
                    <div className="rounded-xl border border-white/10 bg-white/[0.02] p-4">
                        <label className="mb-2 block text-xs uppercase tracking-[0.15em] text-slate-400">Identifier Input</label>
                        <input
                            value={identifier}
                            onChange={(event) => setIdentifier(event.target.value)}
                            disabled={running}
                            className="w-full rounded-lg border border-slate-600 bg-slate-950 px-3 py-2 text-sm text-slate-100"
                            placeholder="new@domain.com / unverified@domain.com / active@domain.com"
                        />

                        <div className="mt-3 rounded-lg border border-white/10 bg-slate-900/60 p-3">
                            <p className="mb-2 text-xs font-semibold uppercase tracking-[0.14em] text-cyan-200">Test Identifiers (Simulation Scenarios)</p>
                            <div className="space-y-2">
                                {SCENARIO_HELPERS.map((scenario) => (
                                    <div key={scenario.identifier} className="rounded-md border border-white/10 bg-black/20 p-2">
                                        <button
                                            type="button"
                                            onClick={() => setIdentifier(scenario.identifier)}
                                            disabled={running}
                                            className="rounded-full border border-cyan-400/40 bg-cyan-400/10 px-3 py-1 text-xs font-medium text-cyan-100 transition hover:border-cyan-300 hover:bg-cyan-400/20 disabled:cursor-not-allowed disabled:opacity-50"
                                        >
                                            {scenario.identifier}
                                        </button>
                                        <p className="mt-1 text-xs text-slate-300">→ {scenario.notes[0]}</p>
                                        <p className="text-xs text-emerald-300">→ {scenario.notes[1]}</p>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                    <button type="button" disabled={running} onClick={startSimulation} className="rounded-lg border border-cyan-300/60 bg-cyan-400/10 px-5 py-3 text-sm font-semibold text-cyan-100 disabled:opacity-50">
                        {running ? 'Running Simulation...' : 'Run Simulation'}
                    </button>
                    <button type="button" onClick={resetSimulation} className="rounded-lg border border-white/20 bg-white/5 px-5 py-3 text-sm">Reset</button>
                </div>

                <div className="rounded-2xl border border-white/10 bg-gradient-to-br from-[#0d1326] to-[#101b35] p-4" style={{minHeight: 780}}>
                    <div ref={timelineRef} className="simulation-frame rounded-xl border border-white/10 bg-black/15 p-2">
                        <div className="actor-header-row">
                            {ACTORS.map((actor) => (
                                <div key={actor.id} className="actor-header-card rounded-xl border border-slate-500 bg-slate-900/90" style={{left: actor.x, top: 0}}>
                                    <p className="pt-3 text-center text-[15px] font-semibold text-slate-100">{actor.label}</p>
                                </div>
                            ))}
                            <div style={{height: 66}} />
                        </div>

                        <svg width={CANVAS_WIDTH} height={diagramHeight} className="simulation-canvas">
                            {ACTORS.map((actor) => (
                                <line key={actor.id} x1={actor.x} y1={70} x2={actor.x} y2={diagramHeight - 30} className="stroke-slate-500" strokeDasharray="8 8" />
                            ))}

                            {events.map((event, index) => {
                                const y = 112 + index * EVENT_GAP;
                                const isActive = activeEventId === event.id;

                                if (event.kind === 'process') {
                                    const x = actorX(event.actor);
                                    const processClass = event.tone === 'response'
                                        ? 'fill-emerald-400/15 stroke-emerald-300'
                                        : event.tone === 'error'
                                            ? 'fill-rose-400/15 stroke-rose-300'
                                            : 'fill-orange-400/15 stroke-orange-300';

                                    return (
                                        <g key={event.id}>
                                            <rect x={x - 112} y={y - 22} width="224" height="44" rx="8" className={isActive ? 'fill-cyan-400/20 stroke-cyan-300' : processClass} style={{transition: 'all 220ms ease'}} />
                                            <text x={x} y={y + 8} textAnchor="middle" className="fill-slate-100 text-[14px]">[{event.label}]</text>
                                        </g>
                                    );
                                }

                                const startX = actorX(event.from);
                                const endX = actorX(event.to);
                                const isRight = startX < endX;
                                const arrowColor = toneColor(event.tone);
                                const headPoints = isRight
                                    ? `${endX},${y} ${endX - 12},${y - 6} ${endX - 12},${y + 6}`
                                    : `${endX},${y} ${endX + 12},${y - 6} ${endX + 12},${y + 6}`;

                                return (
                                    <g key={event.id}>
                                        <line x1={startX} y1={y} x2={endX} y2={y} className={isActive ? 'arrow-line' : undefined} stroke={arrowColor} strokeWidth="3" />
                                        <polygon points={headPoints} fill={arrowColor} className={isActive ? 'arrow-head' : undefined} />
                                        {isActive ? (
                                            <circle r="4" fill="#22d3ee" className="packet">
                                                <animateMotion dur="1.1s" fill="freeze" path={`M ${startX} ${y} L ${endX} ${y}`} />
                                            </circle>
                                        ) : null}
                                        <text x={(startX + endX) / 2} y={y - 10} textAnchor="middle" className="fill-slate-200 text-[13px]">{event.label}</text>
                                    </g>
                                );
                            })}
                        </svg>
                    </div>
                </div>

                <div className="grid gap-4 xl:grid-cols-2">
                    <section className="rounded-xl border border-white/10 bg-white/[0.02] p-4 text-sm text-slate-300">
                        <h4 className="mb-2 font-semibold">Identifier Status Response</h4>
                        <pre className="rounded-lg border border-white/10 bg-black/45 p-3 text-xs text-cyan-200">{JSON.stringify(result, null, 2)}</pre>
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

export default IdentifierFlowPage;
