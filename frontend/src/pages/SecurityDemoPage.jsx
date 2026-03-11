import {useEffect, useMemo, useRef, useState} from 'react';
import DocsLayout from '../components/DocsLayout';

const ACCESS_TTL_SECONDS = 15 * 60;
const JWT_ALGORITHM = 'HS256';
const USER_ID = 'user_1024';

const SIM_STATES = {
    LOGIN_COMPLETE: 'LOGIN_COMPLETE',
    API_REQUEST: 'API_REQUEST',
    ACCESS_EXPIRED: 'ACCESS_EXPIRED',
    REFRESH_REQUEST: 'REFRESH_REQUEST',
    TOKEN_ROTATED: 'TOKEN_ROTATED',
    TOKEN_STOLEN: 'TOKEN_STOLEN',
    TOKEN_REUSE_DETECTED: 'TOKEN_REUSE_DETECTED',
    SESSION_REVOKED: 'SESSION_REVOKED',
};

const COMPONENTS = {
    USER: 'USER',
    AUTH_FILTER: 'AUTH_FILTER',
    JWT_VALIDATOR: 'JWT_VALIDATOR',
    REDIS: 'REDIS',
    CONTROLLER: 'CONTROLLER',
    ATTACKER: 'ATTACKER',
};

const COMPONENT_POSITIONS = {
    USER: {x: 100, y: 170},
    AUTH_FILTER: {x: 430, y: 90},
    JWT_VALIDATOR: {x: 620, y: 90},
    REDIS: {x: 430, y: 250},
    CONTROLLER: {x: 620, y: 250},
    ATTACKER: {x: 100, y: 300},
};

const fmtTtl = (s) => `${String(Math.floor(Math.max(0, s) / 60)).padStart(2, '0')}:${String(Math.max(0, s) % 60).padStart(2, '0')}`;
const tokenId = (prefix) => `${prefix}_${Math.random().toString(36).slice(2, 8)}`;

const INITIAL_REFRESH_TOKEN = {
    id: 'rt_bootstrap',
    version: 1,
    familyId: 'family_42',
    status: 'active',
    oldRef: '-',
};

const INITIAL_LOGS = [
    '[boot] [Auth] login successful',
    '[boot] [Token] access token issued',
    '[boot] [Token] refresh token issued',
];

const SecurityDemoPage = () => {
    const [simState, setSimState] = useState(SIM_STATES.LOGIN_COMPLETE);
    const [running, setRunning] = useState(false);
    const [accessTtl, setAccessTtl] = useState(ACCESS_TTL_SECONDS);
    const [accessIssuedAt, setAccessIssuedAt] = useState(new Date());
    const [accessStatus, setAccessStatus] = useState('valid');

    const [refreshToken, setRefreshToken] = useState(INITIAL_REFRESH_TOKEN);

    const [redisStore, setRedisStore] = useState({
        activeToken: INITIAL_REFRESH_TOKEN.id,
        revokedTokens: [],
        familyBlacklisted: false,
    });

    const [sessionStatus, setSessionStatus] = useState('active');
    const [activeComponent, setActiveComponent] = useState(COMPONENTS.USER);
    const [packet, setPacket] = useState({x: COMPONENT_POSITIONS.USER.x, y: COMPONENT_POSITIONS.USER.y, color: 'green', visible: false});
    const [activePath, setActivePath] = useState('');
    const [alertRed, setAlertRed] = useState(false);
    const [logs, setLogs] = useState(INITIAL_LOGS);

    const runIdRef = useRef(0);
    const expiredLoggedRef = useRef(false);


    const addLog = (line) => {
        const ts = new Date().toLocaleTimeString();
        setLogs((prev) => [...prev.slice(-30), `[${ts}] ${line}`]);
    };

    useEffect(() => {
        const timer = setInterval(() => {
            setAccessTtl((prev) => {
                if (sessionStatus !== 'active' || prev <= 0) return Math.max(prev, 0);
                const next = prev - 1;
                if (next <= 0 && !expiredLoggedRef.current) {
                    expiredLoggedRef.current = true;
                    setAccessStatus('expired');
                    setSimState(SIM_STATES.ACCESS_EXPIRED);
                    addLog('[Security] access token expired');
                }
                return Math.max(next, 0);
            });
        }, 1000);
        return () => clearInterval(timer);
    }, [sessionStatus]);

    const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

    const movePacket = (from, to, color = 'yellow') => {
        setActivePath(`${from}-${to}`);
        setPacket({
            x: COMPONENT_POSITIONS[from].x,
            y: COMPONENT_POSITIONS[from].y,
            color,
            visible: true,
        });

        requestAnimationFrame(() => {
            setPacket((prev) => ({
                ...prev,
                x: COMPONENT_POSITIONS[to].x,
                y: COMPONENT_POSITIONS[to].y,
            }));
        });
    };

    const runSequence = async (steps, nextState) => {
        if (running) return;
        const runId = Date.now();
        runIdRef.current = runId;
        setRunning(true);

        for (const step of steps) {
            if (runIdRef.current !== runId) return;
            if (step.state) setSimState(step.state);
            if (step.active) setActiveComponent(step.active);
            if (step.path) movePacket(step.path.from, step.path.to, step.path.color);
            if (step.redAlert) setAlertRed(true);
            if (step.log) addLog(step.log);
            if (step.after) step.after();
            await delay(step.delay ?? 420);
        }

        if (runIdRef.current === runId) {
            setPacket((prev) => ({...prev, visible: false}));
            setActivePath('');
            if (nextState) setSimState(nextState);
            setRunning(false);
        }
    };

    const rotateTokens = () => {
        const old = refreshToken;
        const newId = tokenId('rt');
        const newVersion = old.version + 1;

        setRefreshToken({
            id: newId,
            version: newVersion,
            familyId: old.familyId,
            status: 'active',
            oldRef: old.id,
        });

        setRedisStore((prev) => ({
            ...prev,
            activeToken: newId,
            revokedTokens: [...prev.revokedTokens, old.id],
        }));

        setAccessTtl(ACCESS_TTL_SECONDS);
        setAccessIssuedAt(new Date());
        setAccessStatus('valid');
        expiredLoggedRef.current = false;

        addLog(`[Token] old refresh token revoked: ${old.id}`);
        addLog(`[Token] new refresh token issued: ${newId}`);
        addLog('[Token] new access token issued');
    };

    const sendApiRequest = async () => {
        if (running) return;
        if (sessionStatus !== 'active') {
            addLog('[Request] blocked: session revoked');
            return;
        }

        if (accessTtl <= 0 || accessStatus === 'expired') {
            setSimState(SIM_STATES.ACCESS_EXPIRED);
            addLog('[Request] 401 Unauthorized');
            return;
        }

        await runSequence([
            {state: SIM_STATES.API_REQUEST, active: COMPONENTS.USER, path: {from: 'USER', to: 'AUTH_FILTER', color: 'yellow'}, log: '[Request] sending access token to backend'},
            {active: COMPONENTS.AUTH_FILTER, path: {from: 'AUTH_FILTER', to: 'JWT_VALIDATOR', color: 'yellow'}, log: '[Auth Filter] bearer token extracted'},
            {active: COMPONENTS.JWT_VALIDATOR, log: '[Security] jwt signature verified'},
            {active: COMPONENTS.JWT_VALIDATOR, path: {from: 'JWT_VALIDATOR', to: 'REDIS', color: 'yellow'}, log: '[Security] expiration check passed'},
            {active: COMPONENTS.REDIS, path: {from: 'REDIS', to: 'CONTROLLER', color: 'yellow'}, log: '[Redis] token not blacklisted'},
            {active: COMPONENTS.CONTROLLER, path: {from: 'CONTROLLER', to: 'USER', color: 'green'}, log: '[Request] response 200'},
        ], SIM_STATES.LOGIN_COMPLETE);
    };

    const forceAccessExpiration = () => {
        if (running) return;
        setAccessTtl(0);
        setAccessStatus('expired');
        expiredLoggedRef.current = true;
        setSimState(SIM_STATES.ACCESS_EXPIRED);
        addLog('[Security] access token forcibly expired for simulation');
    };

    const runRefreshFlow = async () => {
        if (running || sessionStatus !== 'active') return;

        await runSequence([
            {state: SIM_STATES.REFRESH_REQUEST, active: COMPONENTS.USER, path: {from: 'USER', to: 'AUTH_FILTER', color: 'yellow'}, log: '[Refresh] user sent refresh token cookie'},
            {active: COMPONENTS.AUTH_FILTER, path: {from: 'AUTH_FILTER', to: 'JWT_VALIDATOR', color: 'yellow'}, log: '[Auth] backend reads refresh token from cookie'},
            {active: COMPONENTS.JWT_VALIDATOR, path: {from: 'JWT_VALIDATOR', to: 'REDIS', color: 'yellow'}, log: '[Redis] checking token version and family state'},
            {active: COMPONENTS.REDIS, log: '[Security] refresh token accepted for rotation'},
            {state: SIM_STATES.TOKEN_ROTATED, active: COMPONENTS.CONTROLLER, path: {from: 'REDIS', to: 'CONTROLLER', color: 'green'}, after: rotateTokens, log: '[Token] refresh rotation complete'},
            {active: COMPONENTS.CONTROLLER, path: {from: 'CONTROLLER', to: 'USER', color: 'green'}, log: '[Refresh] session continued with rotated credentials'},
        ], SIM_STATES.LOGIN_COMPLETE);
    };

    const simulateTokenTheft = async () => {
        if (running || sessionStatus !== 'active') return;
        const stolen = refreshToken.id;

        await runSequence([
            {state: SIM_STATES.TOKEN_STOLEN, active: COMPONENTS.ATTACKER, path: {from: 'USER', to: 'ATTACKER', color: 'yellow'}, log: `[Alert] refresh token stolen (${stolen})`},
            {active: COMPONENTS.USER, path: {from: 'USER', to: 'AUTH_FILTER', color: 'yellow'}, log: '[User] legitimate refresh executes first'},
            {active: COMPONENTS.REDIS, path: {from: 'AUTH_FILTER', to: 'REDIS', color: 'yellow'}, log: '[Redis] valid refresh found, rotating token'},
            {state: SIM_STATES.TOKEN_ROTATED, active: COMPONENTS.CONTROLLER, path: {from: 'REDIS', to: 'CONTROLLER', color: 'green'}, after: rotateTokens, log: '[Token] rotation produced new refresh token'},
            {active: COMPONENTS.ATTACKER, path: {from: 'ATTACKER', to: 'AUTH_FILTER', color: 'red'}, log: `[Attack] attacker reused old refresh token ${stolen}`},
            {state: SIM_STATES.TOKEN_REUSE_DETECTED, active: COMPONENTS.JWT_VALIDATOR, path: {from: 'AUTH_FILTER', to: 'JWT_VALIDATOR', color: 'red'}, log: '[Security] refresh token reuse detected', redAlert: true},
            {
                state: SIM_STATES.SESSION_REVOKED,
                active: COMPONENTS.REDIS,
                path: {from: 'JWT_VALIDATOR', to: 'REDIS', color: 'red'},
                log: '[Redis] token family blacklisted',
                after: () => {
                    setRedisStore((prev) => ({
                        ...prev,
                        activeToken: '-',
                        familyBlacklisted: true,
                        revokedTokens: [...new Set([...prev.revokedTokens, stolen, refreshToken.id])],
                    }));
                },
            },
            {
                active: COMPONENTS.CONTROLLER,
                path: {from: 'REDIS', to: 'USER', color: 'red'},
                log: '[Session] user session revoked',
                after: () => {
                    setSessionStatus('revoked');
                    setAccessStatus('expired');
                    setAccessTtl(0);
                    setRefreshToken((prev) => ({...prev, status: 'revoked'}));
                },
            },
        ], SIM_STATES.SESSION_REVOKED);

        setTimeout(() => setAlertRed(false), 500);
    };

    const resetSimulation = () => {
        runIdRef.current = 0;
        setRunning(false);
        setSimState(SIM_STATES.LOGIN_COMPLETE);
        setAccessTtl(ACCESS_TTL_SECONDS);
        setAccessIssuedAt(new Date());
        setAccessStatus('valid');

        const familyId = `family_${Math.floor(Math.random() * 90 + 10)}`;
        const rt = tokenId('rt');
        setRefreshToken({id: rt, version: 1, familyId, status: 'active', oldRef: '-'});
        setRedisStore({activeToken: rt, revokedTokens: [], familyBlacklisted: false});

        setSessionStatus('active');
        setActiveComponent(COMPONENTS.USER);
        setPacket({x: COMPONENT_POSITIONS.USER.x, y: COMPONENT_POSITIONS.USER.y, color: 'green', visible: false});
        setActivePath('');
        setAlertRed(false);
        setLogs([]);
        expiredLoggedRef.current = false;
        addLog('[Auth] login successful');
        addLog('[Token] access token issued');
        addLog('[Token] refresh token issued');
    };


    const pathClass = (name, color) => {
        if (activePath !== name) return 'stroke-slate-600';
        if (color === 'green') return 'stroke-emerald-400';
        if (color === 'red') return 'stroke-rose-400';
        return 'stroke-amber-300';
    };

    const componentClass = (component) => {
        const isActive = activeComponent === component;
        if (alertRed) {
            return 'border-rose-400/80 bg-rose-500/20 shadow-[0_0_20px_rgba(251,113,133,0.45)]';
        }
        return isActive
            ? 'border-cyan-300/80 bg-cyan-400/15 shadow-[0_0_20px_rgba(34,211,238,0.45)]'
            : 'border-white/15 bg-slate-900/80';
    };

    const redisRevokedPreview = useMemo(() => redisStore.revokedTokens.slice(-3), [redisStore.revokedTokens]);

    return (
        <DocsLayout title="Security Demo" subtitle="Interactive security lab: JWT validation, refresh token rotation, and token reuse detection under attack.">
            <div className="grid gap-5 xl:grid-cols-2">
                <section className="space-y-4 rounded-2xl border border-white/10 bg-white/[0.03] p-5">
                    <h3 className="text-lg font-semibold">Panel 1 — Session State</h3>
                    <article className="rounded-xl border border-emerald-300/25 bg-emerald-300/5 p-4">
                        <p className="mb-2 font-semibold text-emerald-100">Access Token</p>
                        <p className="text-sm text-slate-300">status: <span className={accessStatus === 'valid' ? 'text-emerald-300' : 'text-rose-300'}>{accessStatus}</span></p>
                        <p className="text-sm text-slate-300">TTL countdown: <span className="font-mono text-cyan-200">{fmtTtl(accessTtl)}</span></p>
                        <p className="text-sm text-slate-300">issued: <span className="text-slate-200">{accessIssuedAt.toLocaleTimeString()}</span></p>
                        <p className="text-sm text-slate-300">algorithm: <span className="text-cyan-200">{JWT_ALGORITHM}</span></p>
                        <p className="text-sm text-slate-300">user id: <span className="text-cyan-200">{USER_ID}</span></p>
                    </article>

                    <article className="rounded-xl border border-violet-300/25 bg-violet-300/5 p-4">
                        <p className="mb-2 font-semibold text-violet-100">Refresh Token</p>
                        <p className="text-sm text-slate-300">status: <span className={refreshToken.status === 'active' ? 'text-emerald-300' : 'text-rose-300'}>{refreshToken.status}</span></p>
                        <p className="text-sm text-slate-300">token version: <span className="font-mono text-violet-200">v{refreshToken.version}</span></p>
                        <p className="text-sm text-slate-300">token family id: <span className="font-mono text-violet-200">{refreshToken.familyId}</span></p>
                        <p className="text-sm text-slate-300">rotation enabled: <span className="text-cyan-200">true</span></p>
                        <p className="text-sm text-slate-300">token id: <span className="font-mono text-violet-200">{refreshToken.id}</span></p>
                        <p className="text-sm text-slate-300">old token ref: <span className="font-mono text-rose-300">{refreshToken.oldRef}</span></p>
                    </article>

                    <article className="rounded-xl border border-amber-300/25 bg-amber-300/5 p-4">
                        <p className="mb-2 font-semibold text-amber-100">Redis Token Store</p>
                        <p className="text-sm text-slate-300">{refreshToken.familyId}</p>
                        <p className="text-sm text-slate-300">active_token: <span className="font-mono text-emerald-300">{redisStore.activeToken}</span></p>
                        <p className="text-sm text-slate-300">revoked_token: <span className="font-mono text-rose-300">{redisRevokedPreview.join(', ') || '-'}</span></p>
                        <p className="text-sm text-slate-300">family_blacklisted: <span className={redisStore.familyBlacklisted ? 'text-rose-300' : 'text-emerald-300'}>{String(redisStore.familyBlacklisted)}</span></p>
                    </article>
                </section>

                <section className="space-y-4 rounded-2xl border border-white/10 bg-white/[0.03] p-5">
                    <h3 className="text-lg font-semibold">Panel 2 — System Architecture Diagram</h3>
                    <div className="rounded-xl border border-white/10 bg-gradient-to-br from-[#0d1325] to-[#131a33] p-3">
                        <div className="mb-2 flex items-center gap-3 text-xs text-slate-400">
                            <span className="text-emerald-300">green = accepted</span>
                            <span className="text-amber-300">yellow = processing</span>
                            <span className="text-rose-300">red = blocked</span>
                            <span className="rounded border border-white/15 px-2 py-0.5 text-slate-300">state: {simState}</span>
                        </div>
                        <svg viewBox="0 0 900 360" className="h-[340px] w-full">
                            <rect x="320" y="30" width="520" height="300" rx="16" className="fill-slate-900/60 stroke-slate-600" strokeWidth="2" />
                            <text x="338" y="52" className="fill-slate-300 text-[12px]">Backend</text>

                            <path d="M130 170 H 390 V 90 H 410" className={`${pathClass('USER-AUTH_FILTER', packet.color)} fill-none stroke-[4] transition-all`} />
                            <path d="M450 90 H 600" className={`${pathClass('AUTH_FILTER-JWT_VALIDATOR', packet.color)} fill-none stroke-[4] transition-all`} />
                            <path d="M640 110 V 250 H 470" className={`${pathClass('JWT_VALIDATOR-REDIS', packet.color)} fill-none stroke-[4] transition-all`} />
                            <path d="M470 250 H 600" className={`${pathClass('REDIS-CONTROLLER', packet.color)} fill-none stroke-[4] transition-all`} />
                            <path d="M640 250 V 170 H 130" className={`${pathClass('CONTROLLER-USER', packet.color)} fill-none stroke-[4] transition-all`} />
                            <path d="M130 300 H 390 V 90 H 410" className={`${pathClass('ATTACKER-AUTH_FILTER', packet.color)} fill-none stroke-[4] transition-all`} />
                            <path d="M130 170 H 130 V 300" className={`${pathClass('USER-ATTACKER', packet.color)} fill-none stroke-[4] transition-all`} />

                            {packet.visible && (
                                <circle
                                    cx={packet.x}
                                    cy={packet.y}
                                    r="6"
                                    className={packet.color === 'green' ? 'fill-emerald-300' : packet.color === 'red' ? 'fill-rose-300' : 'fill-amber-300'}
                                    style={{transition: 'all 360ms linear'}}
                                />
                            )}

                            <g transform="translate(65,135)">
                                <rect width="70" height="70" rx="12" className={`${componentClass(COMPONENTS.USER)} stroke`} />
                                <text x="35" y="40" textAnchor="middle" className="fill-slate-100 text-[11px]">User</text>
                            </g>
                            <g transform="translate(65,265)">
                                <rect width="80" height="70" rx="12" className={`${componentClass(COMPONENTS.ATTACKER)} stroke`} />
                                <text x="40" y="40" textAnchor="middle" className="fill-slate-100 text-[11px]">Attacker</text>
                            </g>
                            <g transform="translate(410,55)">
                                <rect width="170" height="70" rx="12" className={`${componentClass(COMPONENTS.AUTH_FILTER)} stroke`} />
                                <text x="85" y="40" textAnchor="middle" className="fill-slate-100 text-[11px]">Auth Filter</text>
                            </g>
                            <g transform="translate(600,55)">
                                <rect width="170" height="70" rx="12" className={`${componentClass(COMPONENTS.JWT_VALIDATOR)} stroke`} />
                                <text x="85" y="40" textAnchor="middle" className="fill-slate-100 text-[11px]">JWT Validator</text>
                            </g>
                            <g transform="translate(410,215)">
                                <rect width="170" height="70" rx="12" className={`${componentClass(COMPONENTS.REDIS)} stroke`} />
                                <text x="85" y="40" textAnchor="middle" className="fill-slate-100 text-[11px]">Redis Token Store</text>
                            </g>
                            <g transform="translate(600,215)">
                                <rect width="170" height="70" rx="12" className={`${componentClass(COMPONENTS.CONTROLLER)} stroke`} />
                                <text x="85" y="40" textAnchor="middle" className="fill-slate-100 text-[11px]">Controller</text>
                            </g>
                        </svg>
                    </div>
                </section>
            </div>

            <div className="mt-5 grid gap-5 xl:grid-cols-2">
                <section className="space-y-4 rounded-2xl border border-white/10 bg-white/[0.03] p-5">
                    <h3 className="text-lg font-semibold">Panel 3 — Interactive Controls</h3>
                    <div className="flex flex-wrap gap-3">
                        <button type="button" onClick={sendApiRequest} disabled={running} className="rounded-lg border border-cyan-300/60 bg-cyan-400/10 px-4 py-2 text-sm font-semibold text-cyan-100 hover:bg-cyan-400/20 disabled:opacity-60">Send API Request</button>
                        <button type="button" onClick={simulateTokenTheft} disabled={running} className="rounded-lg border border-amber-300/60 bg-amber-400/10 px-4 py-2 text-sm font-semibold text-amber-100 hover:bg-amber-400/20 disabled:opacity-60">Simulate Token Theft</button>
                        <button type="button" onClick={forceAccessExpiration} disabled={running} className="rounded-lg border border-orange-300/60 bg-orange-400/10 px-4 py-2 text-sm font-semibold text-orange-100 hover:bg-orange-400/20 disabled:opacity-60">Force Access Expiration</button>
                        <button type="button" onClick={runRefreshFlow} disabled={running || sessionStatus !== 'active'} className="rounded-lg border border-emerald-300/60 bg-emerald-400/10 px-4 py-2 text-sm font-semibold text-emerald-100 hover:bg-emerald-400/20 disabled:opacity-60">Run Refresh Flow</button>
                        <button type="button" onClick={resetSimulation} className="rounded-lg border border-white/20 bg-white/5 px-4 py-2 text-sm font-semibold text-slate-200 hover:bg-white/10">Reset Simulation</button>
                    </div>
                    <div className="text-xs text-slate-300">
                        <p>session: <span className={sessionStatus === 'active' ? 'text-emerald-300' : 'text-rose-300'}>{sessionStatus}</span></p>
                        <p>simulation running: <span className="text-cyan-200">{String(running)}</span></p>
                    </div>
                </section>

                <section className="space-y-3 rounded-2xl border border-white/10 bg-black/35 p-5">
                    <h3 className="text-lg font-semibold">Panel 4 — Security Log Console</h3>
                    <div className="h-56 overflow-auto rounded-xl border border-white/10 bg-black/55 p-3 font-mono text-xs text-green-300">
                        {logs.length === 0 ? <p className="text-slate-500">No events yet.</p> : logs.map((line, idx) => <p key={`${idx}-${line}`}>{line}</p>)}
                    </div>
                </section>
            </div>
        </DocsLayout>
    );
};

export default SecurityDemoPage;
