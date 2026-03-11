import {useEffect, useRef, useState} from 'react';
import DocsLayout from '../components/DocsLayout';

const ACCESS_TTL_SECONDS = 15 * 60;
const SIMULATION_SPEED = 30;

const formatTtl = (seconds) => {
    const safe = Math.max(seconds, 0);
    const mm = String(Math.floor(safe / 60)).padStart(2, '0');
    const ss = String(safe % 60).padStart(2, '0');
    return `${mm}:${ss}`;
};

const tokenId = (prefix) => `${prefix}-${Math.random().toString(36).slice(2, 8)}`;

const SecurityDemoPage = () => {
    const [sessionActive, setSessionActive] = useState(true);
    const [accessTtl, setAccessTtl] = useState(ACCESS_TTL_SECONDS);
    const [refreshToken, setRefreshToken] = useState({id: tokenId('rt'), status: 'active'});
    const [revokedRefreshToken, setRevokedRefreshToken] = useState(null);
    const [requestState, setRequestState] = useState('idle');
    const [flowSignal, setFlowSignal] = useState({link: '', color: 'green'});
    const [logs, setLogs] = useState([]);
    const timersRef = useRef([]);

    useEffect(() => {
        const timer = setInterval(() => {
            setAccessTtl((prev) => {
                if (!sessionActive || prev <= 0) {
                    return 0;
                }
                return Math.max(prev - SIMULATION_SPEED, 0);
            });
        }, 1000);
        return () => clearInterval(timer);
    }, [sessionActive]);

    useEffect(() => () => {
        timersRef.current.forEach(clearTimeout);
    }, []);

    const addLog = (line) => {
        const stamp = new Date().toLocaleTimeString();
        setLogs((prev) => [...prev.slice(-22), `[${stamp}] ${line}`]);
    };

    const schedule = (fn, delay) => {
        const t = setTimeout(fn, delay);
        timersRef.current.push(t);
    };

    const performRotation = () => {
        const oldToken = refreshToken;
        const nextRefresh = {id: tokenId('rt'), status: 'active'};
        const nextAccess = ACCESS_TTL_SECONDS;

        setRevokedRefreshToken({...oldToken, status: 'revoked'});
        setRefreshToken(nextRefresh);
        setAccessTtl(nextAccess);

        addLog(`[Rotation] old refresh token revoked: ${oldToken.id}`);
        addLog(`[Rotation] new refresh token issued: ${nextRefresh.id}`);
        addLog('[Rotation] new access token issued');
    };

    const sendApiRequest = () => {
        if (!sessionActive) {
            addLog('[Request] blocked, session terminated');
            setFlowSignal({link: 'user-backend', color: 'red'});
            return;
        }

        setRequestState('verifying');
        setFlowSignal({link: 'user-backend', color: 'green'});
        addLog('[Request] sending access token to backend');

        schedule(() => {
            if (accessTtl > 0) {
                addLog('[Request] access token verified');
                schedule(() => {
                    addLog('[Request] response 200');
                    setRequestState('success');
                    setFlowSignal({link: 'user-backend', color: 'green'});
                }, 350);
                return;
            }

            addLog('[Request] 401 Unauthorized');
            setRequestState('refreshing');
            setFlowSignal({link: 'user-backend', color: 'yellow'});
            schedule(() => {
                addLog('[Refresh] access expired, requesting token refresh');
                setFlowSignal({link: 'backend-user', color: 'yellow'});
                performRotation();
                schedule(() => {
                    addLog('[Request] retried with rotated token, response 200');
                    setRequestState('success');
                    setFlowSignal({link: 'user-backend', color: 'green'});
                }, 350);
            }, 450);
        }, 450);
    };

    const simulateTokenTheft = () => {
        if (!sessionActive) {
            addLog('[Security] theft simulation skipped: no active session');
            return;
        }

        const stolen = refreshToken.id;
        addLog(`[Attack] attacker stole refresh token ${stolen}`);
        setFlowSignal({link: 'backend-attacker', color: 'yellow'});

        schedule(() => {
            addLog('[User] legitimate refresh happens first (rotation)');
            setFlowSignal({link: 'user-backend', color: 'green'});
            performRotation();
        }, 500);

        schedule(() => {
            addLog(`[Attack] attacker attempts refresh with old token ${stolen}`);
            setFlowSignal({link: 'attacker-backend', color: 'red'});
        }, 1100);

        schedule(() => {
            addLog('[Security] refresh token reuse detected');
            addLog('[Security] token family revoked');
            addLog('[Session] forced logout');
            setSessionActive(false);
            setAccessTtl(0);
            setRefreshToken((prev) => ({...prev, status: 'revoked'}));
            setRequestState('blocked');
            setFlowSignal({link: 'backend-attacker', color: 'red'});
        }, 1650);
    };

    const resetSimulation = () => {
        timersRef.current.forEach(clearTimeout);
        timersRef.current = [];
        setSessionActive(true);
        setAccessTtl(ACCESS_TTL_SECONDS);
        setRefreshToken({id: tokenId('rt'), status: 'active'});
        setRevokedRefreshToken(null);
        setRequestState('idle');
        setFlowSignal({link: '', color: 'green'});
        setLogs([]);
    };

    const accessStatus = sessionActive ? (accessTtl > 0 ? 'valid' : 'expired') : 'expired';

    const arrowColor = (linkName) => {
        if (flowSignal.link !== linkName) return 'stroke-slate-600';
        if (flowSignal.color === 'green') return 'stroke-emerald-400';
        if (flowSignal.color === 'yellow') return 'stroke-amber-300';
        return 'stroke-rose-400';
    };

    return (
        <DocsLayout title="Security Demo" subtitle="Interactive JWT + refresh token rotation lifecycle showing access expiry, automatic rotation, theft detection, and family revocation.">
            <section className="space-y-5 rounded-2xl border border-white/10 bg-white/[0.03] p-6">
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <h3 className="text-xl font-semibold">JWT Session Lifecycle Simulator</h3>
                    <span className="rounded-full border border-cyan-300/30 bg-cyan-300/10 px-3 py-1 text-xs text-cyan-100">TTL simulation speed: {SIMULATION_SPEED}x</span>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                    <article className="rounded-xl border border-emerald-300/20 bg-emerald-300/5 p-4">
                        <h4 className="mb-3 font-semibold text-emerald-100">Access Token</h4>
                        <p className="text-sm text-slate-300">Status: <span className={`${accessStatus === 'valid' ? 'text-emerald-300' : 'text-rose-300'}`}>{accessStatus}</span></p>
                        <p className="text-sm text-slate-300">TTL countdown: <span className="font-mono text-cyan-200">{formatTtl(accessTtl)}</span> / 15:00</p>
                        <div className="mt-3 h-2 overflow-hidden rounded-full bg-black/40">
                            <div className={`h-full transition-all ${accessStatus === 'valid' ? 'bg-emerald-400' : 'bg-rose-400'}`} style={{width: `${(accessTtl / ACCESS_TTL_SECONDS) * 100}%`}} />
                        </div>
                    </article>

                    <article className="rounded-xl border border-violet-300/20 bg-violet-300/5 p-4">
                        <h4 className="mb-3 font-semibold text-violet-100">Refresh Token</h4>
                        <p className="text-sm text-slate-300">Status: <span className={`${refreshToken.status === 'active' ? 'text-emerald-300' : 'text-rose-300'}`}>{refreshToken.status}</span></p>
                        <p className="text-sm text-slate-300">Rotation enabled: <span className="text-cyan-200">true</span></p>
                        <p className="text-sm text-slate-300">Token id: <span className="font-mono text-violet-200">{refreshToken.id}</span></p>
                        {revokedRefreshToken ? (
                            <p className="mt-2 text-sm text-slate-300">Old token: <span className="font-mono text-rose-300 line-through">{revokedRefreshToken.id}</span> <span className="text-rose-300">revoked</span></p>
                        ) : null}
                    </article>
                </div>

                <div className="rounded-2xl border border-white/10 bg-gradient-to-br from-[#0b1020] to-[#161a2f] p-4">
                    <div className="mb-2 flex items-center justify-between text-xs text-slate-400">
                        <span>green = accepted</span>
                        <span>yellow = suspicious</span>
                        <span>red = blocked</span>
                    </div>
                    <svg viewBox="0 0 900 220" className="h-[220px] w-full">
                        <defs>
                            <marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
                                <path d="M0,0 L0,6 L9,3 z" fill="currentColor" />
                            </marker>
                        </defs>

                        <path d="M170 95 C 300 55, 420 55, 550 95" className={`${arrowColor('user-backend')} fill-none stroke-[4] transition-all`} markerEnd="url(#arrow)" />
                        <path d="M550 120 C 430 165, 290 165, 170 120" className={`${arrowColor('backend-user')} fill-none stroke-[4] transition-all`} markerEnd="url(#arrow)" />
                        <path d="M550 130 C 640 165, 735 165, 820 130" className={`${arrowColor('backend-attacker')} fill-none stroke-[4] transition-all`} markerEnd="url(#arrow)" />
                        <path d="M820 95 C 735 55, 640 55, 550 95" className={`${arrowColor('attacker-backend')} fill-none stroke-[4] transition-all`} markerEnd="url(#arrow)" />

                        <g>
                            <circle cx="120" cy="108" r="44" className="fill-sky-500/15 stroke-sky-300" strokeWidth="2" />
                            <text x="120" y="105" textAnchor="middle" className="fill-sky-100 text-[13px]">User</text>
                        </g>
                        <g>
                            <circle cx="600" cy="108" r="50" className="fill-emerald-500/15 stroke-emerald-300" strokeWidth="2" />
                            <text x="600" y="105" textAnchor="middle" className="fill-emerald-100 text-[13px]">Backend</text>
                        </g>
                        <g>
                            <circle cx="860" cy="108" r="42" className="fill-rose-500/15 stroke-rose-300" strokeWidth="2" />
                            <text x="860" y="105" textAnchor="middle" className="fill-rose-100 text-[13px]">Attacker</text>
                        </g>
                    </svg>
                </div>

                <div className="flex flex-wrap gap-3">
                    <button type="button" onClick={sendApiRequest} className="rounded-lg border border-cyan-300/60 bg-cyan-400/10 px-4 py-2 text-sm font-semibold text-cyan-100 transition hover:bg-cyan-400/20">Send API Request</button>
                    <button type="button" onClick={simulateTokenTheft} className="rounded-lg border border-amber-300/60 bg-amber-400/10 px-4 py-2 text-sm font-semibold text-amber-100 transition hover:bg-amber-400/20">Simulate Token Theft</button>
                    <button type="button" onClick={resetSimulation} className="rounded-lg border border-white/20 bg-white/5 px-4 py-2 text-sm font-semibold text-slate-200 transition hover:bg-white/10">Reset</button>
                    <span className="rounded-lg border border-white/15 bg-black/20 px-3 py-2 text-xs text-slate-300">request state: <span className="text-cyan-200">{requestState}</span></span>
                    <span className={`rounded-lg border px-3 py-2 text-xs ${sessionActive ? 'border-emerald-300/35 bg-emerald-400/10 text-emerald-200' : 'border-rose-300/35 bg-rose-400/10 text-rose-200'}`}>session: {sessionActive ? 'active' : 'terminated'}</span>
                </div>

                <div className="rounded-xl border border-white/10 bg-black/45 p-3 font-mono text-xs text-green-300">
                    <p className="mb-2 text-slate-400">security-runtime.log</p>
                    <div className="space-y-1">
                        {logs.length === 0 ? <p className="text-slate-500">Start with "Send API Request" or "Simulate Token Theft".</p> : logs.map((line, index) => <p key={`${index}-${line}`}>{line}</p>)}
                    </div>
                </div>
            </section>
        </DocsLayout>
    );
};

export default SecurityDemoPage;
