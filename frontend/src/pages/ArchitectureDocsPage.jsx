import {useEffect, useMemo, useRef, useState} from 'react';
import {
    CheckCircle,
    CornerDownRight,
    Cpu,
    Database,
    HardDrive,
    Key,
    Monitor,
    Shield,
    Smartphone,
    Tablet,
    XCircle,
} from 'react-feather';
import DocsLayout from '../components/DocsLayout';
import FlowNode from '../components/architecture/FlowNode';
import FlowConnector from '../components/architecture/FlowConnector';
import SimulationController from '../components/architecture/SimulationController';
import {useScrollReveal} from '../hooks/useScrollReveal';
import './ArchitectureDocsPage.css';

const SECTION_REVEAL_OPTIONS = {
    threshold: 0.14,
    rootMargin: '0px 0px -15% 0px',
    once: true,
};

const REQUEST_ACTIONS = {
    login: {
        id: 'login',
        label: 'Login',
        endpoint: 'POST /auth/login',
        tooltips: {
            user: 'User confirms secure sign-in.',
            frontend: 'Initiates API request',
            backend: 'Controller → Service → Repository',
            database: 'Executes indexed query',
            response: 'Returns JWT and session metadata',
        },
    },
    fetchSessions: {
        id: 'fetchSessions',
        label: 'Fetch Sessions',
        endpoint: 'GET /sessions',
        tooltips: {
            user: 'User opens session dashboard.',
            frontend: 'Initiates API request',
            backend: 'Controller → Service → Repository',
            database: 'Executes indexed query',
            response: 'Returns active device sessions',
        },
    },
    revokeSession: {
        id: 'revokeSession',
        label: 'Revoke Session',
        endpoint: 'DELETE /sessions/{id}',
        tooltips: {
            user: 'User selects compromised device.',
            frontend: 'Initiates API request',
            backend: 'Controller → Service → Repository',
            database: 'Executes indexed query',
            response: 'Session is invalidated in real time',
        },
    },
};

const REQUEST_FLOW_NODES = [
    {id: 'user', label: 'User', icon: Shield},
    {id: 'frontend', label: 'Frontend', icon: Monitor, timing: '5ms'},
    {id: 'backend', label: 'Backend', icon: Cpu, timing: '20ms'},
    {id: 'database', label: 'Database', icon: Database, timing: '30ms'},
    {id: 'response', label: 'Response', icon: CornerDownRight},
];

const JWT_STAGES = [
    {id: 'issued', label: 'Issued', icon: Key, detail: 'Token minted with short TTL'},
    {id: 'stored', label: 'Stored', icon: HardDrive, detail: 'Stored by secure client storage'},
    {id: 'attached', label: 'Attached', icon: Monitor, detail: 'Bearer token attached to request'},
    {id: 'verified', label: 'Verified', icon: CheckCircle, detail: 'Backend validates signature and claims'},
];

const DEVICE_SESSIONS = [
    {id: 'laptop', label: 'Laptop - Chrome', icon: Monitor, location: 'Dhaka, BD', lastActive: 'Just now'},
    {id: 'mobile', label: 'Mobile - iOS', icon: Smartphone, location: 'Sylhet, BD', lastActive: '2 min ago'},
    {id: 'tablet', label: 'Tablet - Android', icon: Tablet, location: 'Chittagong, BD', lastActive: '14 min ago'},
];

const C4_GROUPS = [
    {id: 'frontend', title: 'Frontend', subtitle: 'React + Vercel CDN', chips: ['Session UX', 'Real-time Updates']},
    {id: 'backend', title: 'Backend', subtitle: 'Spring Boot REST API', chips: ['Auth Services', 'Session Control']},
    {id: 'data', title: 'Data Layer', subtitle: 'PostgreSQL + Redis', chips: ['Persistent State', 'Cache Layer']},
    {id: 'external', title: 'External', subtitle: 'OAuth Provider', chips: ['Identity Federation']},
];

const speedProfile = {
    slow: {step: 920, network: 820, bridge: 820},
    normal: {step: 620, network: 560, bridge: 620},
    fast: {step: 360, network: 320, bridge: 360},
};

const sleep = (ms) => new Promise((resolve) => window.setTimeout(resolve, ms));

const RevealSection = ({children, className = ''}) => {
    const {ref, isVisible} = useScrollReveal(SECTION_REVEAL_OPTIONS);

    return (
        <section
            ref={ref}
            className={`reveal reveal-section rounded-xl bg-white border border-slate-200 p-6 md:p-8 shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)] dark:bg-slate-900/40 dark:border-slate-700/50 dark:shadow-[0_1px_2px_rgba(0,0,0,0.1)] ${isVisible ? 'revealed' : ''} ${className}`}
        >
            {children}
        </section>
    );
};

const ArchitectureDocsPage = () => {
    const [selectedAction, setSelectedAction] = useState('login');
    const [simulationState, setSimulationState] = useState('success');
    const [speed, setSpeed] = useState('normal');

    const [activeStep, setActiveStep] = useState(-1);
    const [requestRunning, setRequestRunning] = useState(false);

    const [jwtTokenMode, setJwtTokenMode] = useState('valid');
    const [jwtStep, setJwtStep] = useState(-1);
    const [jwtRunning, setJwtRunning] = useState(false);

    const [masterRunning, setMasterRunning] = useState(false);
    const [backendPulse, setBackendPulse] = useState(false);

    const [deviceStates, setDeviceStates] = useState(() =>
        DEVICE_SESSIONS.map((device) => ({
            ...device,
            status: 'active',
            revoking: false,
            revokedAt: null,
        }))
    );

    const activeTimeoutsRef = useRef([]);

    const actionConfig = REQUEST_ACTIONS[selectedAction];
    const requestStepCount = REQUEST_FLOW_NODES.length;
    const currentSpeed = speedProfile[speed] ?? speedProfile.normal;

    useEffect(() => {
        return () => {
            activeTimeoutsRef.current.forEach((timeoutId) => window.clearTimeout(timeoutId));
            activeTimeoutsRef.current = [];
        };
    }, []);

    useEffect(() => {
        if (!requestRunning) {
            return undefined;
        }

        const intervalId = window.setInterval(() => {
            setActiveStep((prev) => {
                const next = prev + 1;
                if (next >= requestStepCount) {
                    window.clearInterval(intervalId);
                    setRequestRunning(false);
                    return requestStepCount - 1;
                }
                return next;
            });
        }, currentSpeed.step);

        return () => window.clearInterval(intervalId);
    }, [requestRunning, requestStepCount, currentSpeed.step]);

    useEffect(() => {
        if (!jwtRunning) {
            return undefined;
        }

        const intervalId = window.setInterval(() => {
            setJwtStep((prev) => {
                const next = prev + 1;
                if (next >= JWT_STAGES.length) {
                    window.clearInterval(intervalId);
                    setJwtRunning(false);
                    return JWT_STAGES.length - 1;
                }
                return next;
            });
        }, currentSpeed.step);

        return () => window.clearInterval(intervalId);
    }, [jwtRunning, currentSpeed.step]);

    const activeRequestNode = REQUEST_FLOW_NODES[activeStep] ?? null;
    const jwtFailure = jwtTokenMode === 'expired' && jwtStep >= JWT_STAGES.length - 1;

    const requestSummary = useMemo(() => {
        if (requestRunning) {
            return 'Flow in progress...';
        }

        if (activeStep < 0) {
            return 'Ready to run';
        }

        return simulationState === 'success' ? 'Completed successfully' : 'Error path complete';
    }, [requestRunning, activeStep, simulationState]);

    const resetRequestFlow = () => {
        setRequestRunning(false);
        setActiveStep(-1);
    };

    const playRequestFlow = () => {
        setActiveStep(-1);
        setRequestRunning(true);
    };

    const playJwtFlow = () => {
        setJwtStep(-1);
        setJwtRunning(true);
    };

    const resetAll = () => {
        setMasterRunning(false);
        setRequestRunning(false);
        setJwtRunning(false);
        setActiveStep(-1);
        setJwtStep(-1);
        setBackendPulse(false);
        setDeviceStates((prev) => prev.map((device) => ({
            ...device,
            status: 'active',
            revoking: false,
            revokedAt: null
        })));
    };

    const runMasterSimulation = async () => {
        if (masterRunning) {
            return;
        }

        setMasterRunning(true);
        setJwtTokenMode(simulationState === 'error' ? 'expired' : 'valid');
        setActiveStep(-1);
        setJwtStep(-1);

        setRequestRunning(true);
        await sleep((requestStepCount + 1) * currentSpeed.step);

        setJwtRunning(true);
        await sleep((JWT_STAGES.length + 1) * currentSpeed.step + currentSpeed.bridge);

        if (selectedAction === 'revokeSession') {
            setBackendPulse(true);
            await sleep(currentSpeed.network);
            setBackendPulse(false);
        }

        setMasterRunning(false);
    };

    const revokeDeviceSession = (deviceId) => {
        const target = deviceStates.find((device) => device.id === deviceId);
        if (!target || target.status !== 'active') {
            return;
        }

        setBackendPulse(true);
        setDeviceStates((prev) => prev.map((device) => device.id === deviceId ? {...device, revoking: true} : device));

        const timeoutId = window.setTimeout(() => {
            setDeviceStates((prev) =>
                prev.map((device) => {
                    if (device.id !== deviceId) {
                        return device;
                    }

                    return {
                        ...device,
                        revoking: false,
                        status: 'revoked',
                        revokedAt: new Date().toLocaleTimeString(),
                        lastActive: 'just revoked',
                    };
                })
            );
            setBackendPulse(false);
        }, currentSpeed.network);

        activeTimeoutsRef.current.push(timeoutId);
    };

    const restoreSessions = () => {
        setDeviceStates((prev) =>
            prev.map((device, index) => ({
                ...device,
                status: 'active',
                revoking: false,
                revokedAt: null,
                lastActive: DEVICE_SESSIONS[index].lastActive,
            }))
        );
    };

    return (
        <DocsLayout
            title="Architecture"
            subtitle="A live system walkthrough that explains Dragon of North: request lifecycle, JWT security, session management, and runtime topology."
        >
            <RevealSection className="architecture-hero-bg overflow-hidden">
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Request
                    Lifecycle</p>
                <h3 className="mt-3 text-2xl font-bold tracking-tight md:text-3xl text-slate-900 dark:text-slate-50">How
                    a Request Flows Through the System</h3>
                <p className="mt-2 max-w-3xl text-sm text-slate-600 dark:text-slate-400 md:text-base leading-relaxed">
                    From user action to database response, each transition is simulated as a live execution path.
                </p>

                <div className="mt-8">
                    <SimulationController
                        actionOptions={Object.values(REQUEST_ACTIONS)}
                        selectedAction={selectedAction}
                        onActionChange={(actionId) => {
                            setSelectedAction(actionId);
                            resetRequestFlow();
                        }}
                        endpoint={actionConfig.endpoint}
                        onPlay={playRequestFlow}
                        onMasterPlay={runMasterSimulation}
                        masterRunning={masterRunning}
                        onReset={resetAll}
                        status={simulationState}
                        onStatusChange={(next) => {
                            setSimulationState(next);
                            setJwtTokenMode(next === 'error' ? 'expired' : 'valid');
                        }}
                        speed={speed}
                        onSpeedChange={setSpeed}
                        running={requestRunning}
                    />
                </div>

                <div className="mt-4 flex flex-wrap items-center gap-2 text-xs">
                    <span
                        className="rounded-md bg-slate-100 px-2.5 py-1.5 text-slate-700 border border-slate-200 dark:bg-slate-700/40 dark:text-slate-300 dark:border-slate-600/50">
                        Step {Math.max(activeStep + 1, 1)} / {requestStepCount}
                    </span>
                    <span
                        className="rounded-md bg-slate-100 px-2.5 py-1.5 text-slate-700 border border-slate-200 dark:bg-slate-700/40 dark:text-slate-300 dark:border-slate-600/50">
                        {requestSummary}
                    </span>
                    <span
                        className={`inline-flex items-center gap-1 rounded-md px-2.5 py-1.5 font-semibold border transition-all duration-300 ${simulationState === 'success' ? 'bg-green-50/80 text-green-700 border-green-400 dark:bg-green-950/40 dark:text-green-300 dark:border-green-600/60' : 'bg-red-50/80 text-red-700 border-red-400 dark:bg-red-950/40 dark:text-red-300 dark:border-red-600/60'}`}
                    >
                        {simulationState === 'success' ? <CheckCircle size={13}/> : <XCircle size={13}/>}
                        {simulationState === 'success' ? 'Success' : 'Error'}
                    </span>
                </div>

                <div className="mt-8 overflow-x-auto pb-3">
                    <div className="flex min-w-[1040px] items-center gap-2">
                        {REQUEST_FLOW_NODES.map((node, index) => {
                            const isActive = activeStep === index;
                            const isCompleted = activeStep > index;
                            const isErrorNode = simulationState === 'error' && index === REQUEST_FLOW_NODES.length - 1 && activeStep >= index;
                            const detail = isErrorNode ? 'Returns 4xx/5xx response to client' : actionConfig.tooltips[node.id];

                            return (
                                <div key={node.id} className="relative flex items-center">
                                    <FlowNode
                                        icon={node.icon}
                                        label={node.label}
                                        detail={detail}
                                        timing={node.timing}
                                        timingVisible={activeStep >= index && activeStep >= 0}
                                        showDetail={isActive}
                                        active={isActive}
                                        completed={isCompleted}
                                        error={isErrorNode}
                                    />
                                    {index < REQUEST_FLOW_NODES.length - 1 && (
                                        <FlowConnector
                                            active={requestRunning && activeStep === index + 1}
                                            completed={activeStep > index + 1}
                                            error={simulationState === 'error' && activeStep >= REQUEST_FLOW_NODES.length - 1}
                                            speed={speed}
                                        />
                                    )}
                                </div>
                            );
                        })}
                    </div>
                </div>

                <div className="mt-4 rounded-lg bg-slate-50 px-3.5 py-2.5 text-sm border border-slate-200 dark:bg-slate-800/40 dark:border-slate-700/50 transition-all duration-300 ${
                    activeStep === 0 ? 'text-blue-700 dark:text-blue-300' :
                    simulationState === 'success' && activeStep >= requestStepCount - 1 ? 'text-green-700 dark:text-green-300' :
                    simulationState === 'error' && activeStep >= requestStepCount - 1 ? 'text-red-700 dark:text-red-300' :
                    'text-slate-700 dark:text-slate-300'
                }">
                    {activeRequestNode
                        ? `${activeRequestNode.label}: ${actionConfig.tooltips[activeRequestNode.id]}`
                        : 'Start simulation to inspect each pipeline step.'}
                </div>
            </RevealSection>

            <RevealSection>
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">JWT
                    Authentication</p>
                <div className="mt-3 flex flex-wrap items-center justify-between gap-4">
                    <div>
                        <h3 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-50">Secure Token
                            Lifecycle</h3>
                        <p className="mt-2 max-w-3xl text-sm text-slate-600 dark:text-slate-400 md:text-base leading-relaxed">
                            Token travels through issuance, storage, request attachment, and verification.
                        </p>
                    </div>
                    <div className="flex items-center gap-2 text-xs flex-shrink-0">
                        <button
                            type="button"
                            onClick={playJwtFlow}
                            disabled={jwtRunning}
                            className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1.5 font-medium text-slate-700 border border-slate-200 transition-all duration-300 ease-in-out hover:bg-slate-200 disabled:opacity-60 dark:bg-slate-700/40 dark:text-slate-300 dark:border-slate-600/50"
                        >
                            {jwtRunning ? 'Playing...' : 'Play'}
                        </button>
                        <div
                            className="inline-flex items-center rounded-full bg-slate-100 border border-slate-200 p-0.5 dark:bg-slate-700/40 dark:border-slate-600/50">
                            <button
                                type="button"
                                onClick={() => {
                                    setJwtTokenMode('valid');
                                    setJwtStep(-1);
                                    setJwtRunning(false);
                                }}
                                className={`rounded-full px-2.5 py-1 text-xs font-medium transition-all duration-300 ease-in-out ${jwtTokenMode === 'valid' ? 'bg-green-50 text-green-700 dark:bg-green-600/20 dark:text-green-200' : 'text-slate-600 dark:text-slate-400'}`}
                            >
                                Valid
                            </button>
                            <button
                                type="button"
                                onClick={() => {
                                    setJwtTokenMode('expired');
                                    setJwtStep(-1);
                                    setJwtRunning(false);
                                }}
                                className={`rounded-full px-2.5 py-1 text-xs font-medium transition-all duration-300 ease-in-out ${jwtTokenMode === 'expired' ? 'bg-red-50 text-red-700 dark:bg-red-600/20 dark:text-red-200' : 'text-slate-600 dark:text-slate-400'}`}
                            >
                                Expired
                            </button>
                        </div>
                    </div>
                </div>

                <div className="mt-7 overflow-x-auto pb-3">
                    <div className="flex min-w-[920px] items-center gap-2">
                        {JWT_STAGES.map((stage, index) => {
                            const isActive = jwtStep === index;
                            const isCompleted = jwtStep > index;
                            const isError = stage.id === 'verified' && jwtFailure;

                            return (
                                <div key={stage.id} className="relative flex items-center">
                                    <FlowNode
                                        icon={stage.icon}
                                        label={stage.label}
                                        detail={isError ? '401 Unauthorized: token expired' : stage.detail}
                                        showDetail={isActive}
                                        active={isActive}
                                        completed={isCompleted}
                                        error={isError}
                                    />
                                    {index < JWT_STAGES.length - 1 && (
                                        <FlowConnector
                                            traveler="token"
                                            active={jwtRunning && jwtStep === index + 1}
                                            completed={jwtStep > index + 1}
                                            error={jwtFailure && index >= JWT_STAGES.length - 2}
                                            speed={speed}
                                        />
                                    )}
                                </div>
                            );
                        })}
                    </div>
                </div>

                <div className="mt-4 flex flex-wrap items-center gap-2 text-xs">
                    <span
                        className="rounded-md bg-slate-100 px-2.5 py-1.5 text-slate-700 border border-slate-200 dark:bg-slate-700/40 dark:text-slate-300 dark:border-slate-600/50">
                        Step {Math.max(jwtStep + 1, 1)} / {JWT_STAGES.length}
                    </span>
                    {jwtFailure ? (
                        <span
                            className="rounded-md bg-red-50/80 px-2.5 py-1.5 font-semibold text-red-700 border border-red-400 transition-all duration-300 dark:bg-red-950/40 dark:text-red-300 dark:border-red-600/60">
                            401 Unauthorized
                        </span>
                    ) : (
                        <span
                            className="rounded-md bg-green-50/80 px-2.5 py-1.5 font-semibold text-green-700 border border-green-400 transition-all duration-300 dark:bg-green-950/40 dark:text-green-300 dark:border-green-600/60">
                            Verified
                        </span>
                    )}
                </div>
            </RevealSection>

            <RevealSection>
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Session
                    Management</p>
                <h3 className="mt-3 text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-50">Real-Time
                    Session Control</h3>
                <p className="mt-2 max-w-3xl text-sm text-slate-600 dark:text-slate-400 md:text-base leading-relaxed">
                    Device sessions update with simulated network delay, matching real backend sync behavior.
                </p>

                <div
                    className="mt-5 inline-flex items-center gap-2 rounded-lg bg-slate-100 px-3 py-1.5 text-xs text-slate-700 border border-slate-200 dark:bg-slate-700/40 dark:text-slate-300 dark:border-slate-600/50">
                    <span
                        className={`h-2 w-2 rounded-full transition-all ${backendPulse ? 'bg-blue-400 shadow-[0_0_8px_rgba(59,130,246,0.5)]' : 'bg-slate-400/70'}`}/>
                    Backend API {backendPulse ? 'processing...' : 'idle'}
                </div>

                <div className="mt-6 grid gap-3 md:grid-cols-3">
                    {deviceStates.map((device) => {
                        const DeviceIcon = device.icon;
                        const isActive = device.status === 'active';

                        return (
                            <article
                                key={device.id}
                                className={`rounded-lg border transition-all duration-300 ease-in-out p-4 shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)] ${isActive ? 'border-green-300 bg-green-50/50 dark:border-green-600/50 dark:bg-green-950/20 dark:shadow-none' : 'border-red-400 bg-red-50/80 dark:border-red-600/60 dark:bg-red-950/40 dark:shadow-none opacity-75'} ${device.revoking ? 'architecture-card-pending' : ''}`}
                            >
                                <div className="flex items-start justify-between gap-3">
                                    <div
                                        className={`inline-flex h-9 w-9 items-center justify-center rounded-lg ${isActive ? 'bg-green-100 dark:bg-green-600/30' : 'bg-red-100 dark:bg-red-600/30'}`}>
                                        <DeviceIcon size={18}
                                                    className={isActive ? 'text-green-700 dark:text-green-300' : 'text-red-700 dark:text-red-300'}/>
                                    </div>
                                    <span
                                        className={`rounded-md px-2 py-0.5 text-[10px] font-semibold border transition-all ${isActive ? 'bg-green-50/80 text-green-700 border-green-400 dark:bg-green-950/40 dark:text-green-300 dark:border-green-600/60' : 'bg-red-50/80 text-red-700 border-red-400 dark:bg-red-950/40 dark:text-red-300 dark:border-red-600/60'}`}
                                    >
                                        {device.revoking ? 'Syncing...' : isActive ? 'Active' : 'Revoked'}
                                    </span>
                                </div>

                                <h4 className="mt-3 text-sm font-semibold text-slate-900 dark:text-slate-50">{device.label}</h4>
                                <p className="mt-1 text-xs text-slate-600 dark:text-slate-400">📍 {device.location}</p>
                                <p className="mt-1 text-xs text-slate-600 dark:text-slate-400">⏱️ {device.lastActive}</p>
                                {device.revokedAt && (
                                    <p className="mt-1 text-[10px] text-slate-500 dark:text-slate-500">Revoked: {device.revokedAt}</p>
                                )}

                                <button
                                    type="button"
                                    disabled={!isActive || device.revoking}
                                    onClick={() => revokeDeviceSession(device.id)}
                                    className="mt-4 w-full rounded-lg bg-red-50/80 px-3 py-2 text-xs font-medium text-red-700 border border-red-400 transition-all duration-300 ease-in-out hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-red-950/40 dark:text-red-300 dark:border-red-600/60 dark:hover:bg-red-950/60"
                                >
                                    Revoke
                                </button>
                            </article>
                        );
                    })}
                </div>

                <button
                    type="button"
                    onClick={restoreSessions}
                    className="mt-4 rounded-lg bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-700 border border-slate-200 transition-all duration-300 ease-in-out hover:bg-slate-200 dark:bg-slate-700/30 dark:text-slate-400 dark:border-slate-600/50 dark:hover:bg-slate-700/50"
                >
                    Restore Sessions
                </button>
            </RevealSection>

            <RevealSection>
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">System
                    Overview</p>
                <h3 className="mt-3 text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-50">Architecture
                    Blocks</h3>
                <p className="mt-2 max-w-3xl text-sm text-slate-600 dark:text-slate-400 md:text-base leading-relaxed">
                    Hover the board to reveal service dependencies and protocol labels.
                </p>

                <div
                    className="group relative mt-6 rounded-lg bg-white border border-slate-200 p-4 shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)] dark:bg-slate-800/50 dark:border-slate-700/50 dark:shadow-none">
                    <svg
                        className="pointer-events-none absolute inset-0 hidden h-full w-full opacity-0 transition-opacity duration-300 ease-in-out group-hover:opacity-100 md:block"
                        viewBox="0 0 1000 320"
                        fill="none"
                        aria-hidden="true"
                    >
                        <path className="architecture-dash-line" d="M250 86 C 370 86, 420 86, 500 86"
                              stroke="rgba(79, 70, 229, 0.5)" strokeWidth="1.5"/>
                        <path className="architecture-dash-line" d="M500 86 C 640 86, 710 86, 815 86"
                              stroke="rgba(79, 70, 229, 0.5)" strokeWidth="1.5"/>
                        <path className="architecture-dash-line" d="M500 86 C 640 168, 730 214, 815 246"
                              stroke="rgba(79, 70, 229, 0.4)" strokeWidth="1.5"/>
                        <text x="428" y="72" fill="rgba(79, 70, 229, 0.7)" fontSize="11" fontWeight="600">REST API
                        </text>
                        <text x="640" y="72" fill="rgba(79, 70, 229, 0.7)" fontSize="11" fontWeight="600">Cache</text>
                        <text x="665" y="228" fill="rgba(79, 70, 229, 0.7)" fontSize="11" fontWeight="600">OAuth</text>
                    </svg>

                    <div className="grid gap-3 md:grid-cols-2">
                        {C4_GROUPS.map((groupCard, index) => (
                            <article
                                key={groupCard.id}
                                className={`relative rounded-lg border transition-all duration-300 ease-in-out p-3.5 border-slate-200 bg-white text-slate-900 shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)] dark:border-slate-600/50 dark:bg-slate-800/40 dark:text-slate-50 dark:shadow-none ${index % 2 === 0 ? 'md:translate-y-0' : 'md:translate-y-1'}`}
                            >
                                <h4 className="text-sm font-semibold">{groupCard.title}</h4>
                                <p className="mt-1 text-xs text-slate-600 dark:text-slate-400">{groupCard.subtitle}</p>
                                <div className="mt-2.5 flex flex-wrap gap-1">
                                    {groupCard.chips.map((chip) => (
                                        <span key={chip}
                                              className="rounded-md bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-700 border border-slate-200 dark:bg-slate-700/30 dark:text-slate-300 dark:border-slate-600/50">
                                            {chip}
                                        </span>
                                    ))}
                                </div>
                            </article>
                        ))}
                    </div>
                </div>
            </RevealSection>
        </DocsLayout>
    );
};

export default ArchitectureDocsPage;
