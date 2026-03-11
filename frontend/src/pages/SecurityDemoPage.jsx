import {useEffect, useMemo, useRef, useState} from 'react';
import DocsLayout from '../components/DocsLayout';

const ACCESS_TTL_SECONDS = 15 * 60;
const TRAVEL_MS = 600;
const PROCESS_MS = 300;

const NODES = {
    USER: {x: 190, y: 320, label: 'User', size: 80},
    ATTACKER: {x: 990, y: 320, label: 'Attacker', size: 80},
    BACKEND: {x: 590, y: 90, label: 'Backend'},
    AUTH_FILTER: {x: 470, y: 260, label: 'Auth Filter', size: 120},
    JWT_VALIDATOR: {x: 590, y: 205, label: 'JWT Validator', size: 120},
    REDIS: {x: 710, y: 260, label: 'Redis Token Store', size: 120},
    CONTROLLER: {x: 590, y: 355, label: 'Controller', size: 120},
};

const STEPS = ['Start Session', 'Send API Request', 'Expire Access Token', 'Run Refresh Flow', 'Simulate Token Theft'];

const initialRefresh = {id: 'rt_100a', version: 1, familyId: 'token_family_42', status: 'active', oldRef: '-'};

const fmt = (s) => `${String(Math.floor(Math.max(0, s) / 60)).padStart(2, '0')}:${String(Math.max(0, s) % 60).padStart(2, '0')}`;
const tokenId = () => `rt_${Math.random().toString(36).slice(2, 6)}`;

const stepDescriptions = {
    idle: ['Current Step: Waiting', 'Description: Start a session to initialize token lifecycle simulation.'],
    session: ['Current Step: Session Started', 'Description: Access and refresh tokens are issued and Redis family state is initialized.'],
    api: ['Current Step: API Request Validation', 'Description: Backend validates JWT signature, expiry, Redis blacklist status, then controller responds.'],
    expired: ['Current Step: Access Token Expired', 'Description: Access token reached TTL and API requests now return 401 until refresh flow runs.'],
    refresh: ['Current Step: Refresh Token Rotation', 'Description: Old refresh token is revoked and replaced with a new one; new access token is issued.'],
    theft: ['Current Step: Token Reuse Detection', 'Description: Attacker reuses stale refresh token; backend detects reuse and revokes token family/session.'],
};

const SecurityDemoPage = () => {
    const [simKey, setSimKey] = useState('idle');
    const [running, setRunning] = useState(false);
    const [unlocked, setUnlocked] = useState(0);
    const [accessTtl, setAccessTtl] = useState(ACCESS_TTL_SECONDS);
    const [accessIssuedAt, setAccessIssuedAt] = useState(new Date());
    const [accessStatus, setAccessStatus] = useState('valid');
    const [sessionStatus, setSessionStatus] = useState('idle');
    const [refreshToken, setRefreshToken] = useState(initialRefresh);
    const [redisState, setRedisState] = useState({activeToken: initialRefresh.id, revokedTokens: [], familyBlacklisted: false});

    const [activeNode, setActiveNode] = useState('USER');
    const [activeEdge, setActiveEdge] = useState('');
    const [packet, setPacket] = useState({visible: false, x: NODES.USER.x, y: NODES.USER.y, color: 'yellow', label: ''});
    const [logs, setLogs] = useState(['[boot] simulator ready']);

    const runRef = useRef(0);
    const expiredLogRef = useRef(false);

    const [title, description] = stepDescriptions[simKey] || stepDescriptions.idle;

    const addLog = (msg) => {
        const ts = new Date().toLocaleTimeString();
        setLogs((prev) => [...prev.slice(-35), `[${ts}] ${msg}`]);
    };

    useEffect(() => {
        const timer = setInterval(() => {
            setAccessTtl((prev) => {
                if (sessionStatus !== 'active' || prev <= 0) return Math.max(prev, 0);
                const next = prev - 1;
                if (next === 0 && !expiredLogRef.current) {
                    expiredLogRef.current = true;
                    setAccessStatus('expired');
                    setSimKey('expired');
                    setUnlocked((v) => Math.max(v, 2));
                    addLog('[Security] access token expired');
                }
                return Math.max(next, 0);
            });
        }, 1000);
        return () => clearInterval(timer);
    }, [sessionStatus]);

    const sleep = (ms) => new Promise((res) => setTimeout(res, ms));

    const hop = async ({from, to, color = 'yellow', label, log}) => {
        setActiveNode(from);
        setActiveEdge(`${from}-${to}`);
        setPacket({visible: true, x: NODES[from].x, y: NODES[from].y, color, label});
        if (log) addLog(log);
        requestAnimationFrame(() => {
            setPacket((p) => ({...p, x: NODES[to].x, y: NODES[to].y}));
        });
        await sleep(TRAVEL_MS);
        setActiveNode(to);
        await sleep(PROCESS_MS);
    };

    const run = async (sequence, key) => {
        if (running) return;
        const runId = Date.now();
        runRef.current = runId;
        setRunning(true);
        if (key) setSimKey(key);

        for (const step of sequence) {
            if (runRef.current !== runId) return;
            if (step.action) step.action();
            if (step.hop) await hop(step.hop);
            if (step.wait) await sleep(step.wait);
        }

        if (runRef.current === runId) {
            setPacket((p) => ({...p, visible: false}));
            setActiveEdge('');
            setRunning(false);
        }
    };

    const startSession = async () => {
        await run([
            {action: () => { setSessionStatus('active'); setSimKey('session'); setAccessStatus('valid'); setAccessTtl(ACCESS_TTL_SECONDS); setAccessIssuedAt(new Date()); expiredLogRef.current = false; addLog('[Auth] login successful'); }},
            {action: () => addLog('[Token] access token issued')},
            {action: () => addLog('[Token] refresh token issued')},
        ], 'session');
        setUnlocked(1);
    };

    const sendApiRequest = async () => {
        if (accessStatus === 'expired') {
            addLog('[Request] 401 Unauthorized');
            return;
        }

        await run([
            {hop: {from: 'USER', to: 'AUTH_FILTER', color: 'yellow', label: 'Access Token', log: '[Request] sending access token to backend'}},
            {hop: {from: 'AUTH_FILTER', to: 'JWT_VALIDATOR', color: 'yellow', label: 'JWT Verification', log: '[Auth Filter] request accepted'}},
            {action: () => addLog('[Security] jwt signature verified')},
            {hop: {from: 'JWT_VALIDATOR', to: 'REDIS', color: 'yellow', label: 'JWT Verification', log: '[Security] expiration check passed'}},
            {action: () => addLog('[Redis] token not blacklisted')},
            {hop: {from: 'REDIS', to: 'CONTROLLER', color: 'yellow', label: 'Access Token', log: '[Controller] endpoint executed'}},
            {hop: {from: 'CONTROLLER', to: 'USER', color: 'green', label: '200 OK', log: '[Request] response 200'}},
        ], 'api');
        setUnlocked(2);
    };

    const expireAccess = async () => {
        await run([
            {action: () => { setAccessTtl(0); setAccessStatus('expired'); setSimKey('expired'); expiredLogRef.current = true; addLog('[Security] access token forced to expire'); }},
            {hop: {from: 'USER', to: 'AUTH_FILTER', color: 'red', label: 'Access Token', log: '[Request] sending expired token'}},
            {hop: {from: 'AUTH_FILTER', to: 'JWT_VALIDATOR', color: 'red', label: 'JWT Verification', log: '[Security] expiration check failed'}},
            {hop: {from: 'JWT_VALIDATOR', to: 'USER', color: 'red', label: '401 Unauthorized', log: '[Request] response 401 Unauthorized'}},
        ], 'expired');
        setUnlocked(3);
    };

    const refreshFlow = async () => {
        const oldId = refreshToken.id;
        const nextId = tokenId();
        await run([
            {hop: {from: 'USER', to: 'AUTH_FILTER', color: 'yellow', label: 'Refresh Token', log: '[Refresh] refresh request started'}},
            {hop: {from: 'AUTH_FILTER', to: 'JWT_VALIDATOR', color: 'yellow', label: 'Refresh Token', log: '[Auth] refresh token read from cookie'}},
            {hop: {from: 'JWT_VALIDATOR', to: 'REDIS', color: 'yellow', label: 'Token Rotation', log: '[Redis] token version lookup'}},
            {action: () => {
                setRefreshToken((prev) => ({...prev, id: nextId, version: prev.version + 1, oldRef: oldId, status: 'active'}));
                setRedisState((prev) => ({...prev, activeToken: nextId, revokedTokens: [...prev.revokedTokens, oldId]}));
                setAccessTtl(ACCESS_TTL_SECONDS); setAccessStatus('valid'); setAccessIssuedAt(new Date()); expiredLogRef.current = false;
                addLog(`[Token] old refresh token revoked (${oldId})`); addLog(`[Token] new refresh token issued (${nextId})`); addLog('[Token] new access token issued');
            }},
            {hop: {from: 'REDIS', to: 'CONTROLLER', color: 'green', label: 'Token Rotation', log: '[Security] rotation committed'}},
            {hop: {from: 'CONTROLLER', to: 'USER', color: 'green', label: 'Access Token', log: '[Refresh] session resumed'}},
        ], 'refresh');
        setUnlocked(4);
    };

    const tokenTheft = async () => {
        const stolen = refreshToken.oldRef !== '-' ? refreshToken.oldRef : refreshToken.id;
        await run([
            {hop: {from: 'USER', to: 'ATTACKER', color: 'yellow', label: 'Refresh Token', log: `[Alert] attacker stole token ${stolen}`}},
            {hop: {from: 'ATTACKER', to: 'AUTH_FILTER', color: 'red', label: 'Refresh Token', log: '[Attack] attacker sends refresh request'}},
            {hop: {from: 'AUTH_FILTER', to: 'JWT_VALIDATOR', color: 'red', label: 'Reuse Detection', log: '[Security] refresh token reuse detected'}},
            {hop: {from: 'JWT_VALIDATOR', to: 'REDIS', color: 'red', label: 'Reuse Detection', log: '[Redis] token family blacklisted'}},
            {action: () => {
                setRedisState((prev) => ({...prev, familyBlacklisted: true, activeToken: '-', revokedTokens: [...new Set([...prev.revokedTokens, refreshToken.id, stolen])]}));
                setRefreshToken((prev) => ({...prev, status: 'revoked'}));
                setAccessStatus('expired'); setAccessTtl(0); setSessionStatus('revoked');
                addLog('[Session] forced logout');
            }},
            {hop: {from: 'REDIS', to: 'USER', color: 'red', label: 'Session Revoked', log: '[Security] session terminated'}},
        ], 'theft');
        setUnlocked(5);
    };

    const reset = () => {
        runRef.current = 0;
        setRunning(false);
        setUnlocked(0);
        setSimKey('idle');
        setSessionStatus('idle');
        setAccessTtl(ACCESS_TTL_SECONDS);
        setAccessIssuedAt(new Date());
        setAccessStatus('valid');
        setRefreshToken(initialRefresh);
        setRedisState({activeToken: initialRefresh.id, revokedTokens: [], familyBlacklisted: false});
        setActiveNode('USER');
        setActiveEdge('');
        setPacket({visible: false, x: NODES.USER.x, y: NODES.USER.y, color: 'yellow', label: ''});
        setLogs(['[boot] simulator reset']);
    };

    const nodeCls = (id) => activeNode === id ? 'border-cyan-300/90 bg-cyan-400/20 shadow-[0_0_22px_rgba(34,211,238,.45)]' : 'border-white/15 bg-slate-900/80';
    const edgeCls = (name) => activeEdge === name ? (packet.color === 'green' ? 'stroke-emerald-400' : packet.color === 'red' ? 'stroke-rose-400' : 'stroke-amber-300') : 'stroke-slate-600';
    const revokedPreview = useMemo(() => redisState.revokedTokens.slice(-3), [redisState.revokedTokens]);

    return (
        <DocsLayout title="Security Demo" subtitle="Guided security architecture simulator for JWT validation, refresh rotation, and token reuse defense.">
            <section className="space-y-4 rounded-2xl border border-white/10 bg-white/[0.03] p-5">
                <div className="rounded-xl border border-cyan-300/25 bg-cyan-300/5 p-4">
                    <p className="text-sm font-semibold text-cyan-100">{title}</p>
                    <p className="text-sm text-slate-300">{description}</p>
                </div>

                <div className="rounded-2xl border border-white/10 bg-gradient-to-br from-[#0c1225] to-[#111a35] p-4" style={{minHeight: 600, height: '68vh'}}>
                    <svg viewBox="0 0 1180 660" className="h-full w-full">
                        <circle cx="590" cy="320" r="260" className="fill-none stroke-slate-700/60" strokeDasharray="10 10" />
                        <rect x="365" y="140" width="450" height="280" rx="20" className="fill-slate-900/60 stroke-slate-600" strokeWidth="2" />
                        <text x="380" y="165" className="fill-slate-300 text-[14px]">Backend Internal Pipeline</text>

                        <path d="M230 320 C 300 230, 370 200, 470 200" className={`${edgeCls('USER-AUTH_FILTER')} fill-none stroke-[4]`} />
                        <path d="M530 260 L 570 235" className={`${edgeCls('AUTH_FILTER-JWT_VALIDATOR')} fill-none stroke-[4]`} />
                        <path d="M650 235 L 690 260" className={`${edgeCls('JWT_VALIDATOR-REDIS')} fill-none stroke-[4]`} />
                        <path d="M690 320 L 640 350" className={`${edgeCls('REDIS-CONTROLLER')} fill-none stroke-[4]`} />
                        <path d="M540 355 C 420 430, 320 430, 230 320" className={`${edgeCls('CONTROLLER-USER')} fill-none stroke-[4]`} />
                        <path d="M950 320 C 860 230, 780 200, 700 220" className={`${edgeCls('ATTACKER-AUTH_FILTER')} fill-none stroke-[4]`} />
                        <path d="M230 320 C 400 520, 790 520, 950 320" className={`${edgeCls('USER-ATTACKER')} fill-none stroke-[4]`} />

                        {packet.visible && (
                            <g style={{transition: `all ${TRAVEL_MS}ms linear`}} transform={`translate(${packet.x} ${packet.y})`}>
                                <circle r="10" className={packet.color === 'green' ? 'fill-emerald-300' : packet.color === 'red' ? 'fill-rose-300' : 'fill-amber-300'} />
                                <text y="-16" textAnchor="middle" className="fill-slate-100 text-[10px]">{packet.label}</text>
                            </g>
                        )}

                        <g transform="translate(150 280)"><circle r="40" className={`${nodeCls('USER')} stroke`} /><text textAnchor="middle" className="fill-slate-100 text-[12px]" dy="4">User</text></g>
                        <g transform="translate(1010 280)"><circle r="40" className={`${nodeCls('ATTACKER')} stroke`} /><text textAnchor="middle" className="fill-slate-100 text-[12px]" dy="4">Attacker</text></g>
                        <g transform="translate(500 260)"><rect x="-60" y="-30" width="120" height="60" rx="12" className={`${nodeCls('AUTH_FILTER')} stroke`} /><text textAnchor="middle" className="fill-slate-100 text-[11px]" dy="4">Auth Filter</text></g>
                        <g transform="translate(590 210)"><rect x="-60" y="-30" width="120" height="60" rx="12" className={`${nodeCls('JWT_VALIDATOR')} stroke`} /><text textAnchor="middle" className="fill-slate-100 text-[11px]" dy="4">JWT Validator</text></g>
                        <g transform="translate(700 260)"><rect x="-60" y="-30" width="120" height="60" rx="12" className={`${nodeCls('REDIS')} stroke`} /><text textAnchor="middle" className="fill-slate-100 text-[10px]" dy="4">Redis Token Store</text></g>
                        <g transform="translate(590 355)"><rect x="-60" y="-30" width="120" height="60" rx="12" className={`${nodeCls('CONTROLLER')} stroke`} /><text textAnchor="middle" className="fill-slate-100 text-[11px]" dy="4">Controller</text></g>
                    </svg>
                </div>

                <div className="rounded-xl border border-white/10 bg-black/20 p-4">
                    <h4 className="mb-3 text-sm font-semibold">Simulation Controls (guided)</h4>
                    <div className="flex flex-wrap gap-3">
                        <button type="button" disabled={running || unlocked !== 0} onClick={startSession} className="rounded-lg border border-cyan-300/60 bg-cyan-400/10 px-4 py-2 text-sm disabled:opacity-50">1. Start Session</button>
                        <button type="button" disabled={running || unlocked < 1 || unlocked > 1} onClick={sendApiRequest} className="rounded-lg border border-cyan-300/60 bg-cyan-400/10 px-4 py-2 text-sm disabled:opacity-50">2. Send API Request</button>
                        <button type="button" disabled={running || unlocked < 2 || unlocked > 2} onClick={expireAccess} className="rounded-lg border border-cyan-300/60 bg-cyan-400/10 px-4 py-2 text-sm disabled:opacity-50">3. Expire Access Token</button>
                        <button type="button" disabled={running || unlocked < 3 || unlocked > 3} onClick={refreshFlow} className="rounded-lg border border-cyan-300/60 bg-cyan-400/10 px-4 py-2 text-sm disabled:opacity-50">4. Run Refresh Flow</button>
                        <button type="button" disabled={running || unlocked < 4 || unlocked > 4} onClick={tokenTheft} className="rounded-lg border border-cyan-300/60 bg-cyan-400/10 px-4 py-2 text-sm disabled:opacity-50">5. Simulate Token Theft</button>
                        <button type="button" onClick={reset} className="rounded-lg border border-white/20 bg-white/5 px-4 py-2 text-sm">Reset Simulation</button>
                    </div>
                    <p className="mt-2 text-xs text-slate-400">Next unlocked step: {Math.min(unlocked + 1, 5)} / 5</p>
                </div>

                <div className="grid gap-4 xl:grid-cols-2">
                    <section className="rounded-xl border border-white/10 bg-white/[0.02] p-4 text-sm text-slate-300">
                        <h4 className="mb-2 font-semibold">Session State</h4>
                        <p>Access token: <span className={accessStatus === 'valid' ? 'text-emerald-300' : 'text-rose-300'}>{accessStatus}</span></p>
                        <p>TTL: <span className="font-mono text-cyan-200">{fmt(accessTtl)}</span></p>
                        <p>Issued: {accessIssuedAt.toLocaleTimeString()}</p>
                        <p>Refresh status: <span className={refreshToken.status === 'active' ? 'text-emerald-300' : 'text-rose-300'}>{refreshToken.status}</span></p>
                        <p>Refresh version: v{refreshToken.version}</p>
                        <p>Family: {refreshToken.familyId}</p>
                        <p>Old token ref: {refreshToken.oldRef}</p>
                        <p className="mt-2">Redis active_token: <span className="font-mono text-emerald-300">{redisState.activeToken}</span></p>
                        <p>Redis revoked_token: <span className="font-mono text-rose-300">{revokedPreview.join(', ') || '-'}</span></p>
                        <p>Redis family_blacklisted: <span className={redisState.familyBlacklisted ? 'text-rose-300' : 'text-emerald-300'}>{String(redisState.familyBlacklisted)}</span></p>
                        <p>Session status: <span className={sessionStatus === 'active' ? 'text-emerald-300' : 'text-rose-300'}>{sessionStatus}</span></p>
                    </section>

                    <section className="rounded-xl border border-white/10 bg-black/45 p-4">
                        <h4 className="mb-2 font-semibold">Security Log Console</h4>
                        <div className="h-52 overflow-auto rounded-lg border border-white/10 bg-black/55 p-3 font-mono text-xs text-green-300">
                            {logs.map((l, i) => <p key={`${i}-${l}`}>{l}</p>)}
                        </div>
                    </section>
                </div>
            </section>
        </DocsLayout>
    );
};

export default SecurityDemoPage;
