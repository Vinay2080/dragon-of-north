import {useEffect, useMemo, useRef, useState} from 'react';

const nodes = [
    {id: 'User', x: 80, y: 180, color: '#38bdf8'},
    {id: 'Vercel CDN', x: 250, y: 90, color: '#a78bfa'},
    {id: 'DuckDNS Domain', x: 250, y: 270, color: '#22d3ee'},
    {id: 'Spring Boot Backend', x: 470, y: 180, color: '#4ade80'},
    {id: 'Redis', x: 700, y: 120, color: '#fb7185'},
    {id: 'PostgreSQL', x: 700, y: 260, color: '#facc15'},
];

const pathOrder = ['User', 'Vercel CDN', 'DuckDNS Domain', 'Spring Boot Backend', 'Redis', 'PostgreSQL'];

const logStages = [
    '[CDN] Request received',
    '[DuckDNS] Domain resolved',
    '[Spring Boot] JWT validation',
    '[Redis] Rate limit check',
    '[PostgreSQL] Fetch user record',
];

const latencyStages = [
    'CDN edge hit: 7ms',
    'DNS resolution: 11ms',
    'Backend auth filter: 18ms',
    'Redis response: 2ms',
    'PostgreSQL query: 14ms',
];

const tooltipInfo = {
    Redis: {
        role: 'Fast in-memory layer for request shaping.',
        operations: ['Rate limiting', 'Session caching', 'Token blacklist checks'],
        technology: 'Redis 7',
        latency: '~1-3ms',
    },
    PostgreSQL: {
        role: 'Primary relational store for identity data.',
        operations: ['Fetch user profile', 'Persist auth audit entries', 'Read refresh token metadata'],
        technology: 'PostgreSQL',
        latency: '~10-20ms',
    },
    'Spring Boot Backend': {
        role: 'API and security gateway for auth flows.',
        operations: ['JWT validation', 'OTP verification', 'Policy enforcement'],
        technology: 'Spring Boot 3',
        latency: '~15-30ms',
    },
};

const getNode = (id) => nodes.find((n) => n.id === id);
const edgeId = (from, to) => `${from}->${to}`;

const quadraticPath = (a, b) => {
    const mx = (a.x + b.x) / 2;
    const my = (a.y + b.y) / 2;
    const curve = a.y === b.y ? 50 : 20;
    const cx = mx;
    const cy = my - curve;
    return `M ${a.x} ${a.y} Q ${cx} ${cy} ${b.x} ${b.y}`;
};

const pointOnQuadratic = (a, b, t) => {
    const mx = (a.x + b.x) / 2;
    const my = (a.y + b.y) / 2;
    const curve = a.y === b.y ? 50 : 20;
    const cx = mx;
    const cy = my - curve;
    const x = (1 - t) * (1 - t) * a.x + 2 * (1 - t) * t * cx + t * t * b.x;
    const y = (1 - t) * (1 - t) * a.y + 2 * (1 - t) * t * cy + t * t * b.y;
    return {x, y};
};

const DeploymentTopologyDiagram = () => {
    const [simulations, setSimulations] = useState([]);
    const [now, setNow] = useState(0);
    const [logs, setLogs] = useState([]);
    const [latency, setLatency] = useState('');
    const [hoveredNode, setHoveredNode] = useState(null);
    const timeoutsRef = useRef([]);

    const edges = useMemo(() => pathOrder.slice(0, -1).map((node, index) => [node, pathOrder[index + 1]]), []);

    useEffect(() => {
        let raf;
        const tick = () => {
            setNow(Date.now());
            raf = requestAnimationFrame(tick);
        };
        raf = requestAnimationFrame(tick);
        return () => cancelAnimationFrame(raf);
    }, []);

    useEffect(() => () => {
        timeoutsRef.current.forEach(clearTimeout);
    }, []);

    const appendLog = (line) => {
        const stamp = new Date().toLocaleTimeString();
        setLogs((prev) => [...prev.slice(-15), `[${stamp}] ${line}`]);
    };

    const hopDuration = 700;
    const totalDuration = edges.length * hopDuration;

    const simulateRequest = () => {
        const startedAt = Date.now();
        const id = `${startedAt}-${Math.random()}`;
        setSimulations((prev) => [...prev, {id, startedAt}]);
        appendLog('--- New request simulation started ---');

        logStages.forEach((stage, index) => {
            const timer = setTimeout(() => {
                appendLog(stage);
                setLatency(latencyStages[index]);
            }, (index + 1) * hopDuration);
            timeoutsRef.current.push(timer);
        });

        const clearTimer = setTimeout(() => {
            setLatency('Request completed successfully');
        }, totalDuration + 120);
        timeoutsRef.current.push(clearTimer);
    };

    const activeSims = simulations.filter((sim) => now - sim.startedAt <= totalDuration + 200);

    const activeNodes = new Set();
    const activeEdges = new Set();
    const particles = [];

    activeSims.forEach((sim) => {
        const elapsed = now - sim.startedAt;
        const hopIndex = Math.min(Math.floor(elapsed / hopDuration), edges.length - 1);
        const hopProgress = (elapsed % hopDuration) / hopDuration;

        const [fromId, toId] = edges[hopIndex] || [];
        if (!fromId || !toId) {
            return;
        }

        activeNodes.add(fromId);
        activeNodes.add(toId);
        activeEdges.add(edgeId(fromId, toId));

        const point = pointOnQuadratic(getNode(fromId), getNode(toId), Math.min(Math.max(hopProgress, 0), 1));
        particles.push({id: sim.id, ...point});
    });

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between rounded-xl border border-cyan-300/30 bg-white/[0.03] px-4 py-3">
                <p className="text-sm text-slate-300">Simulate live request traffic across CDN, DNS, backend, cache, and database nodes.</p>
                <button
                    type="button"
                    onClick={simulateRequest}
                    className="rounded-lg border border-cyan-300/70 bg-cyan-400/10 px-4 py-2 text-sm font-semibold text-cyan-100 transition hover:bg-cyan-400/20"
                >
                    Simulate Request
                </button>
            </div>

            <div className="relative overflow-hidden rounded-2xl border border-white/10 bg-gradient-to-br from-[#0a1020] to-[#15182f] p-4 shadow-[0_0_40px_rgba(34,211,238,0.08)]">
                <svg viewBox="0 0 820 360" className="h-[360px] w-full">
                    <defs>
                        <filter id="nodeGlow"><feDropShadow dx="0" dy="0" stdDeviation="6" floodColor="#67e8f9" floodOpacity="0.5" /></filter>
                    </defs>

                    {edges.map(([fromId, toId]) => {
                        const from = getNode(fromId);
                        const to = getNode(toId);
                        const active = activeEdges.has(edgeId(fromId, toId));
                        return (
                            <path
                                key={edgeId(fromId, toId)}
                                d={quadraticPath(from, to)}
                                fill="none"
                                stroke={active ? '#22d3ee' : '#475569'}
                                strokeWidth={active ? 3 : 2}
                                strokeDasharray={active ? '6 8' : '0'}
                                className="transition-all duration-300"
                            />
                        );
                    })}

                    {particles.map((particle) => (
                        <circle key={particle.id} cx={particle.x} cy={particle.y} r="5" fill="#67e8f9" filter="url(#nodeGlow)">
                            <animate attributeName="r" values="4;6;4" dur="0.7s" repeatCount="indefinite" />
                        </circle>
                    ))}

                    {nodes.map((node) => {
                        const active = activeNodes.has(node.id);
                        return (
                            <g
                                key={node.id}
                                onMouseEnter={() => setHoveredNode(node.id)}
                                onMouseLeave={() => setHoveredNode(null)}
                                className="cursor-default"
                            >
                                <circle
                                    cx={node.x}
                                    cy={node.y}
                                    r="42"
                                    fill="#0f172a"
                                    stroke={active ? node.color : '#64748b'}
                                    strokeWidth={active ? 3 : 2}
                                    style={{filter: active ? `drop-shadow(0 0 18px ${node.color})` : 'none'}}
                                >
                                    {active && <animate attributeName="r" values="40;44;40" dur="0.9s" repeatCount="indefinite" />}
                                </circle>
                                <text x={node.x} y={node.y} textAnchor="middle" className="fill-slate-200 text-[10px]" dy="3">{node.id}</text>
                            </g>
                        );
                    })}
                </svg>

                {hoveredNode && tooltipInfo[hoveredNode] && (
                    <div className="pointer-events-none absolute right-4 top-4 max-w-xs rounded-xl border border-cyan-300/30 bg-slate-900/90 p-3 text-xs text-slate-200 shadow-[0_0_22px_rgba(34,211,238,0.2)] backdrop-blur">
                        <p className="mb-1 text-sm font-semibold text-cyan-100">{hoveredNode}</p>
                        <p className="mb-2 text-slate-300">{tooltipInfo[hoveredNode].role}</p>
                        <p className="text-cyan-200">Used for:</p>
                        <ul className="mb-2 list-disc space-y-0.5 pl-4 text-slate-300">
                            {tooltipInfo[hoveredNode].operations.map((operation) => <li key={operation}>{operation}</li>)}
                        </ul>
                        <p><span className="text-cyan-200">Technology:</span> {tooltipInfo[hoveredNode].technology}</p>
                        <p><span className="text-cyan-200">Typical latency:</span> {tooltipInfo[hoveredNode].latency}</p>
                    </div>
                )}
            </div>

            <div className="rounded-lg border border-emerald-300/30 bg-emerald-300/5 p-3 text-sm text-emerald-100">
                {latency || 'Latency details appear here during simulation.'}
            </div>

            <div className="rounded-xl border border-white/10 bg-black/40 p-3 font-mono text-xs text-green-300">
                <p className="mb-2 text-slate-400">runtime.log</p>
                <div className="space-y-1">
                    {logs.length === 0 ? <p className="text-slate-500">Run a simulation to stream live backend logs.</p> : logs.map((log, index) => <p key={`${index}-${log}`}>{log}</p>)}
                </div>
            </div>
        </div>
    );
};

export default DeploymentTopologyDiagram;
