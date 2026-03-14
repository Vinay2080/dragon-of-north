import {useEffect, useMemo, useState} from 'react';
import DocsLayout from '../components/DocsLayout';

const tabs = [
    {id: 'context', label: 'System Context'},
    {id: 'pipeline', label: 'Security Pipeline'},
    {id: 'flow', label: 'Authentication Flow'},
    {id: 'state', label: 'State Machine'},
];

const systemNodes = [
    {id: 'browser', label: 'User Browser', tech: 'Web Client', responsibility: 'Initiates secure sign-in and stores session cookie.', col: 0, row: 0},
    {id: 'frontend', label: 'Frontend (Vercel)', tech: 'React + Vercel', responsibility: 'Hosts UI and forwards authenticated API calls.', col: 1, row: 0},
    {id: 'backend', label: 'Backend API (Spring Boot)', tech: 'Java Spring Boot', responsibility: 'Performs auth logic, token issuance, callbacks, and authorization.', col: 2, row: 0},
    {id: 'postgres', label: 'PostgreSQL', tech: 'PostgreSQL', responsibility: 'Stores users, sessions, refresh token hashes, and audit records.', col: 1, row: 1},
    {id: 'redis', label: 'Redis', tech: 'Redis', responsibility: 'Enforces rate limits and short-lived security metadata caching.', col: 2, row: 1},
    {id: 'oauth', label: 'OAuth Provider', tech: 'OAuth2 / OIDC', responsibility: 'Verifies federated identity and returns trusted claims.', col: 0, row: 1},
];

const systemEdges = [
    {from: 'browser', to: 'frontend', label: 'HTTPS'},
    {from: 'frontend', to: 'backend', label: 'API calls'},
    {from: 'backend', to: 'postgres', label: 'SQL'},
    {from: 'backend', to: 'redis', label: 'Rate limit / cache'},
    {from: 'backend', to: 'oauth', label: 'OAuth2'},
];

const pipelineStages = [
    {id: 'request', label: 'Incoming Request', tech: 'HTTP', responsibility: 'Entry point for all authentication and protected resource access.'},
    {id: 'redis', label: 'Redis Rate Limiter', tech: 'Redis', responsibility: 'Blocks abusive traffic and enforces request quotas.'},
    {id: 'filters', label: 'Spring Security Filters', tech: 'Spring Security', responsibility: 'Applies CSRF/CORS checks and authentication filter chain.'},
    {id: 'jwt', label: 'JWT Verification', tech: 'JWT + RSA', responsibility: 'Validates signature, issuer, expiry, and token integrity.'},
    {id: 'roles', label: 'Role Authorization', tech: 'RBAC Policies', responsibility: 'Confirms role-based permissions for requested action.'},
    {id: 'controller', label: 'Controller', tech: 'Spring MVC', responsibility: 'Executes approved business endpoint logic.'},
];

const authActors = ['User', 'Browser', 'Frontend', 'Backend', 'Database', 'OAuth Provider'];
const authSteps = [
    {actor: 'User', text: 'Submits credentials'},
    {actor: 'Browser', text: 'Sends request to Frontend'},
    {actor: 'Frontend', text: 'Calls Backend login API'},
    {actor: 'Backend', text: 'Validates credentials'},
    {actor: 'Database', text: 'Returns account + policy data'},
    {actor: 'Backend', text: 'Returns JWT and refresh token'},
];

const stateNodes = [
    'Unauthenticated',
    'Credentials Submitted',
    'Password Verified',
    'Token Issued',
    'Session Active',
    'Token Expired',
    'Refresh Attempted',
    'Session Revoked',
];

const stateEdges = [
    ['Unauthenticated', 'Credentials Submitted'],
    ['Credentials Submitted', 'Password Verified'],
    ['Password Verified', 'Token Issued'],
    ['Token Issued', 'Session Active'],
    ['Session Active', 'Token Expired'],
    ['Token Expired', 'Refresh Attempted'],
    ['Refresh Attempted', 'Session Active'],
    ['Refresh Attempted', 'Session Revoked'],
    ['Session Revoked', 'Unauthenticated'],
];

const DiagramCanvas = ({children}) => (
    <div className="relative mx-auto h-[500px] w-full max-w-[1100px] overflow-hidden rounded-2xl border border-cyan-400/25 bg-slate-950/55 p-8 shadow-[inset_0_0_80px_rgba(6,182,212,0.08)] backdrop-blur-md">
        {children}
    </div>
);

const NodeTooltip = ({node}) => {
    if (!node) return null;
    return (
        <div className="pointer-events-none absolute right-6 top-6 z-20 max-w-xs rounded-xl border border-cyan-300/35 bg-slate-900/95 p-3 text-xs text-slate-200 shadow-[0_0_20px_rgba(34,211,238,0.2)]">
            <p className="text-sm font-semibold text-cyan-100">{node.label}</p>
            <p className="mt-1 text-cyan-200/90">{node.tech}</p>
            <p className="mt-2 leading-relaxed text-slate-300">{node.responsibility}</p>
        </div>
    );
};

const SystemContextDiagram = () => {
    const [hovered, setHovered] = useState(null);
    const layout = useMemo(() => {
        const paddingX = 100;
        const paddingY = 90;
        const cols = 3;
        const rows = 2;
        const width = 1000 - paddingX * 2;
        const height = 500 - paddingY * 2;
        const colGap = width / (cols - 1);
        const rowGap = height / (rows - 1);

        return systemNodes.reduce((acc, node) => {
            acc[node.id] = {
                x: paddingX + node.col * colGap,
                y: paddingY + node.row * rowGap,
            };
            return acc;
        }, {});
    }, []);

    return (
        <DiagramCanvas>
            <NodeTooltip node={hovered} />
            <svg viewBox="0 0 1000 500" className="absolute inset-0 h-full w-full">
                {systemEdges.map((edge) => {
                    const start = layout[edge.from];
                    const end = layout[edge.to];
                    const mx = (start.x + end.x) / 2;
                    const my = (start.y + end.y) / 2;
                    return (
                        <g key={`${edge.from}-${edge.to}`}>
                            <line x1={start.x} y1={start.y} x2={end.x} y2={end.y} stroke="rgba(34,211,238,0.55)" strokeWidth="2" />
                            <text x={mx} y={my - 8} fill="#a5f3fc" fontSize="12" textAnchor="middle">{edge.label}</text>
                        </g>
                    );
                })}
            </svg>

            <div className="relative grid h-full grid-cols-3 grid-rows-2 gap-x-16 gap-y-20">
                {systemNodes.map((node) => (
                    <button
                        type="button"
                        key={node.id}
                        onMouseEnter={() => setHovered(node)}
                        onMouseLeave={() => setHovered(null)}
                        className="z-10 rounded-xl border border-cyan-300/35 bg-slate-900/80 px-4 py-3 text-sm text-cyan-100 shadow-[0_0_18px_rgba(34,211,238,0.12)] transition hover:border-cyan-200 hover:shadow-[0_0_24px_rgba(34,211,238,0.35)]"
                    >
                        {node.label}
                    </button>
                ))}
            </div>
        </DiagramCanvas>
    );
};

const SecurityPipelineDiagram = () => {
    const [hovered, setHovered] = useState(null);

    return (
        <DiagramCanvas>
            <NodeTooltip node={hovered} />
            <div className="grid h-full grid-cols-6 items-center gap-4">
                {pipelineStages.map((stage, idx) => (
                    <div key={stage.id} className="relative flex items-center justify-center gap-3">
                        <button
                            type="button"
                            onMouseEnter={() => setHovered(stage)}
                            onMouseLeave={() => setHovered(null)}
                            className="w-full rounded-xl border border-cyan-400/25 bg-slate-900/80 p-3 text-xs text-cyan-100 transition hover:border-cyan-300 hover:shadow-[0_0_22px_rgba(34,211,238,0.35)]"
                        >
                            {stage.label}
                        </button>
                        {idx < pipelineStages.length - 1 && <span className="absolute -right-3 text-cyan-300">→</span>}
                    </div>
                ))}
            </div>
        </DiagramCanvas>
    );
};

const AuthenticationFlowDiagram = () => {
    const [activeStep, setActiveStep] = useState(0);

    useEffect(() => {
        const timer = setInterval(() => {
            setActiveStep((prev) => (prev + 1) % authSteps.length);
        }, 1700);

        return () => clearInterval(timer);
    }, []);

    return (
        <DiagramCanvas>
            <div className="grid h-full grid-cols-6 gap-4">
                {authActors.map((actor) => (
                    <div key={actor} className="rounded-lg border border-slate-600/70 bg-slate-900/70 px-3 py-2 text-center text-xs text-slate-200">
                        {actor}
                    </div>
                ))}
            </div>

            <div className="absolute inset-x-12 top-24 bottom-10 space-y-3">
                {authSteps.map((step, idx) => {
                    const actorIndex = authActors.findIndex((actor) => actor === step.actor);
                    return (
                        <div key={step.text} className="relative h-[54px]">
                            <div
                                className={`absolute h-full w-[16%] rounded-lg border px-3 py-2 text-xs transition-all ${idx === activeStep ? 'border-cyan-200 bg-cyan-400/20 text-cyan-100 shadow-[0_0_22px_rgba(34,211,238,0.35)]' : 'border-cyan-500/25 bg-slate-900/60 text-slate-300'}`}
                                style={{left: `calc(${actorIndex * 16.66}% + 3px)`}}
                            >
                                {step.text}
                            </div>
                        </div>
                    );
                })}
            </div>
        </DiagramCanvas>
    );
};

const StateMachineDiagram = () => {
    const [activeState, setActiveState] = useState(stateNodes[0]);

    useEffect(() => {
        const timer = setInterval(() => {
            const currentIndex = stateNodes.indexOf(activeState);
            setActiveState(stateNodes[(currentIndex + 1) % stateNodes.length]);
        }, 1800);

        return () => clearInterval(timer);
    }, [activeState]);

    const positionMap = useMemo(() => {
        const centerX = 500;
        const centerY = 250;
        const radiusX = 320;
        const radiusY = 165;
        return stateNodes.reduce((acc, state, index) => {
            const angle = (Math.PI * 2 * index) / stateNodes.length - Math.PI / 2;
            acc[state] = {
                x: centerX + radiusX * Math.cos(angle),
                y: centerY + radiusY * Math.sin(angle),
            };
            return acc;
        }, {});
    }, []);

    return (
        <DiagramCanvas>
            <svg viewBox="0 0 1000 500" className="absolute inset-0 h-full w-full">
                {stateEdges.map(([from, to]) => {
                    const start = positionMap[from];
                    const end = positionMap[to];
                    const highlighted = from === activeState;
                    return (
                        <line
                            key={`${from}-${to}`}
                            x1={start.x}
                            y1={start.y}
                            x2={end.x}
                            y2={end.y}
                            stroke={highlighted ? '#67e8f9' : 'rgba(148,163,184,0.55)'}
                            strokeWidth={highlighted ? 2.6 : 1.6}
                            strokeDasharray="7 6"
                            className={highlighted ? 'animate-pulse' : ''}
                        />
                    );
                })}
            </svg>

            <div className="relative h-full">
                {stateNodes.map((state) => {
                    const point = positionMap[state];
                    const active = state === activeState;
                    return (
                        <div
                            key={state}
                            className={`absolute -translate-x-1/2 -translate-y-1/2 rounded-lg border px-3 py-2 text-xs transition-all ${active ? 'border-cyan-200 bg-cyan-400/20 text-cyan-100 shadow-[0_0_20px_rgba(34,211,238,0.35)]' : 'border-cyan-500/30 bg-slate-900/75 text-slate-300'}`}
                            style={{left: `${(point.x / 1000) * 100}%`, top: `${(point.y / 500) * 100}%`}}
                        >
                            {state}
                        </div>
                    );
                })}
            </div>
        </DiagramCanvas>
    );
};

const diagramByTab = {
    context: {
        title: 'System Context',
        description: 'This diagram shows how external actors interact with the authentication platform. Key components include the frontend client, backend API, Redis rate limiter, PostgreSQL database, and OAuth providers.',
        component: <SystemContextDiagram />,
    },
    pipeline: {
        title: 'Security Pipeline',
        description: 'This pipeline illustrates progressive validation from request intake to authorized controller execution, ensuring every call is evaluated before business logic is reached.',
        component: <SecurityPipelineDiagram />,
    },
    flow: {
        title: 'Authentication Flow',
        description: 'This sequence emphasizes credential exchange, backend validation, database verification, and secure token issuance with continuous visibility over each stage.',
        component: <AuthenticationFlowDiagram />,
    },
    state: {
        title: 'Authentication State Machine',
        description: 'This state machine maps the full session lifecycle from unauthenticated state through token expiry and refresh/revocation transitions.',
        component: <StateMachineDiagram />,
    },
};

const ArchitectureDocsPage = () => {
    const [activeTab, setActiveTab] = useState('context');
    const activeDiagram = diagramByTab[activeTab];

    return (
        <DocsLayout
            title="System Architecture"
            subtitle="Interactive exploration of authentication flows, token lifecycle, and system components."
        >
            <section className="space-y-6 rounded-2xl border border-cyan-500/20 bg-white/[0.03] p-6 backdrop-blur-md">
                <div className="flex flex-wrap gap-3">
                    {tabs.map((tab) => (
                        <button
                            type="button"
                            key={tab.id}
                            onClick={() => setActiveTab(tab.id)}
                            className={`rounded-lg border px-4 py-2 text-sm transition ${activeTab === tab.id ? 'border-cyan-300 bg-cyan-400/20 text-cyan-100 shadow-[0_0_18px_rgba(34,211,238,0.2)]' : 'border-slate-600 bg-slate-900/65 text-slate-300 hover:border-cyan-400/50 hover:text-cyan-100'}`}
                        >
                            {tab.label}
                        </button>
                    ))}
                </div>

                <div className="min-h-[580px] space-y-5">
                    <div key={activeTab} className="animate-[fadeIn_280ms_ease-out]">
                        {activeDiagram.component}
                    </div>

                    <div className="rounded-xl border border-cyan-400/20 bg-slate-900/60 p-4 text-sm text-slate-300">
                        <p className="font-semibold text-cyan-100">{activeDiagram.title}</p>
                        <p className="mt-2">{activeDiagram.description}</p>
                    </div>
                </div>
            </section>

            <style>{`@keyframes fadeIn { from {opacity: 0.2;} to {opacity: 1;} }`}</style>
        </DocsLayout>
    );
};

export default ArchitectureDocsPage;
