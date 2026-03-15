import {useCallback, useEffect, useMemo, useState} from 'react';
import DocsLayout from '../components/DocsLayout';
import ReactFlow, {Background, Controls, ReactFlowProvider, useEdgesState, useNodesState} from 'reactflow';
import 'reactflow/dist/style.css';

const STEP_DELAY_MS = 580;
const STEP_SEQUENCE = ['user_input', 'identifier_api', 'user_exists', 'status_check', 'email_verified'];

const NODE_DEFS = {
    user_input: {label: 'User Input', tooltip: 'Collects an email or phone identifier from the user.'},
    identifier_api: {label: 'Identifier Status API', tooltip: 'Sends identifier payload to the status endpoint.'},
    user_exists: {label: 'User Exists Decision', tooltip: 'Checks if the account exists.'},
    signup_path: {label: 'Signup Path', tooltip: 'Route for new users to create an account.'},
    status_check: {label: 'User Status Check', tooltip: 'Evaluates account status for existing users.'},
    email_verified: {label: 'Email Verified Decision', tooltip: 'Determines if existing user is verified.'},
    login_path: {label: 'Login Path', tooltip: 'Route for existing verified users.'},
    verification_path: {label: 'Email Verification Path', tooltip: 'Route for existing unverified users.'},
};

const BASE_NODE_STYLE = {
    background: '#0f172a',
    color: '#cbd5e1',
    border: '1px solid rgba(71, 85, 105, 0.9)',
    borderRadius: '12px',
    boxShadow: '0 0 0 rgba(0, 234, 255, 0)',
    transform: 'scale(1)',
    transition: 'all 220ms ease',
    width: 210,
    padding: '12px',
    fontSize: '12px',
};

const ACTIVE_NODE_STYLE = {
    ...BASE_NODE_STYLE,
    border: '1px solid #00eaff',
    boxShadow: '0 0 20px rgba(0, 234, 255, 0.75)',
    transform: 'scale(1.05)',
    color: '#e2e8f0',
};

const baseNodes = [
    {id: 'user_input', position: {x: 60, y: 140}, data: NODE_DEFS.user_input, style: BASE_NODE_STYLE},
    {id: 'identifier_api', position: {x: 330, y: 140}, data: NODE_DEFS.identifier_api, style: BASE_NODE_STYLE},
    {id: 'user_exists', position: {x: 600, y: 140}, data: NODE_DEFS.user_exists, style: BASE_NODE_STYLE},
    {id: 'signup_path', position: {x: 870, y: 40}, data: NODE_DEFS.signup_path, style: BASE_NODE_STYLE},
    {id: 'status_check', position: {x: 870, y: 240}, data: NODE_DEFS.status_check, style: BASE_NODE_STYLE},
    {id: 'email_verified', position: {x: 1140, y: 240}, data: NODE_DEFS.email_verified, style: BASE_NODE_STYLE},
    {id: 'login_path', position: {x: 1410, y: 140}, data: NODE_DEFS.login_path, style: BASE_NODE_STYLE},
    {id: 'verification_path', position: {x: 1410, y: 320}, data: NODE_DEFS.verification_path, style: BASE_NODE_STYLE},
];

const baseEdges = [
    {id: 'e-user_input-identifier_api', source: 'user_input', target: 'identifier_api', label: 'submit'},
    {id: 'e-identifier_api-user_exists', source: 'identifier_api', target: 'user_exists', label: 'response'},
    {id: 'e-user_exists-signup_path', source: 'user_exists', target: 'signup_path', label: 'No'},
    {id: 'e-user_exists-status_check', source: 'user_exists', target: 'status_check', label: 'Yes'},
    {id: 'e-status_check-email_verified', source: 'status_check', target: 'email_verified', label: 'ACTIVE'},
    {id: 'e-email_verified-login_path', source: 'email_verified', target: 'login_path', label: 'Yes'},
    {id: 'e-email_verified-verification_path', source: 'email_verified', target: 'verification_path', label: 'No'},
];

const scenarios = {
    new: {exists: false, app_user_status: 'NOT_EXISTS', email_verified: false, next_action: 'SIGNUP'},
    verified: {exists: true, app_user_status: 'ACTIVE', email_verified: true, next_action: 'LOGIN'},
    unverified: {exists: true, app_user_status: 'ACTIVE', email_verified: false, next_action: 'EMAIL_VERIFICATION'},
};

const identifyType = (value) => {
    const trimmed = value.trim();
    if (!trimmed) return {type: 'UNKNOWN', normalized: '', valid: false};

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (emailRegex.test(trimmed)) return {type: 'EMAIL', normalized: trimmed.toLowerCase(), valid: true};

    const phoneCandidate = trimmed.replace(/[\s()-]/g, '');
    const phoneRegex = /^\+?[1-9]\d{7,14}$/;
    if (phoneRegex.test(phoneCandidate)) {
        return {type: 'PHONE', normalized: phoneCandidate.startsWith('+') ? phoneCandidate : `+${phoneCandidate}`, valid: true};
    }

    return {type: 'INVALID', normalized: trimmed, valid: false};
};

const inferScenario = (normalizedIdentifier) => {
    const value = normalizedIdentifier.toLowerCase();
    if (value.includes('new') || value.startsWith('+1555000')) return 'new';
    if (value.includes('unverified') || value.startsWith('+1555999')) return 'unverified';
    return 'verified';
};

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const IdentifierFlowVisualizerContent = () => {
    const [identifier, setIdentifier] = useState('');
    const [isAnimating, setAnimating] = useState(false);
    const [currentStep, setCurrentStep] = useState('');
    const [scenarioName, setScenarioName] = useState('verified');
    const [eventLog, setEventLog] = useState(['Idle. Enter an email or phone and click "Check Identifier".']);

    const [nodes, setNodes, onNodesChange] = useNodesState(baseNodes);
    const [edges, setEdges, onEdgesChange] = useEdgesState(baseEdges);

    const identifierMeta = useMemo(() => identifyType(identifier), [identifier]);
    const simulatedResponse = useMemo(() => scenarios[scenarioName], [scenarioName]);

    const simulatedRequest = useMemo(
        () => ({
            identifier: identifierMeta.normalized || '<enter-email-or-phone>',
            identifier_type: identifierMeta.type === 'INVALID' || identifierMeta.type === 'UNKNOWN' ? 'UNKNOWN' : identifierMeta.type,
        }),
        [identifierMeta],
    );

    useEffect(() => {
        setNodes((prev) =>
            prev.map((node) => ({
                ...node,
                style: node.id === currentStep ? ACTIVE_NODE_STYLE : BASE_NODE_STYLE,
            })),
        );

        setEdges((prev) =>
            prev.map((edge) => {
                const active = edge.source === currentStep || edge.target === currentStep;
                return {
                    ...edge,
                    animated: active,
                    className: active ? 'edge-active' : 'edge-default',
                    style: {
                        stroke: active ? '#00eaff' : '#475569',
                        strokeWidth: active ? 2.6 : 1.2,
                    },
                    labelStyle: {fill: active ? '#e2e8f0' : '#94a3b8', fontSize: 11},
                };
            }),
        );
    }, [currentStep, setEdges, setNodes]);

    const appendLog = useCallback((message) => {
        setEventLog((prev) => [...prev.slice(-10), message]);
    }, []);

    const resetFlowState = useCallback(() => {
        setCurrentStep('');
    }, []);

    const runAnimation = useCallback(async () => {
        if (!identifierMeta.valid || isAnimating) return;

        const pickedScenario = inferScenario(identifierMeta.normalized);
        const response = scenarios[pickedScenario];
        const finalStep = !response.exists ? 'signup_path' : (response.email_verified ? 'login_path' : 'verification_path');

        setAnimating(true);
        setScenarioName(pickedScenario);
        setEventLog([`Detected ${identifierMeta.type}: ${identifierMeta.normalized}`]);

        for (const step of STEP_SEQUENCE) {
            setCurrentStep(step);

            if (step === 'user_input') appendLog('Step user_input: identifier captured.');
            if (step === 'identifier_api') appendLog('Step identifier_api: calling Identifier Status API.');
            if (step === 'user_exists') appendLog(`Step user_exists: exists=${String(response.exists)}.`);
            if (step === 'status_check') appendLog(`Step status_check: ${response.exists ? `status=${response.app_user_status}` : 'skipped'}.`);
            if (step === 'email_verified') appendLog(`Step email_verified: ${response.exists ? `email_verified=${String(response.email_verified)}` : 'skipped'}.`);

            await wait(STEP_DELAY_MS);
        }

        setCurrentStep(finalStep);
        appendLog(`Step final_branch: routing to ${NODE_DEFS[finalStep].label}.`);
        await wait(STEP_DELAY_MS);

        appendLog(`Completed flow: ${response.next_action}.`);
        setAnimating(false);
    }, [appendLog, identifierMeta, isAnimating]);

    return (
        <div className="space-y-5">
            <style>{`
                .react-flow__edge.edge-active path {
                    stroke-dasharray: 6;
                    animation: flow 1s linear infinite;
                }
                .react-flow__edge.edge-default path {
                    stroke-dasharray: none;
                }
                @keyframes flow {
                    from { stroke-dashoffset: 10; }
                    to { stroke-dashoffset: 0; }
                }
            `}</style>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(310px,1fr)]">
                <div className="rounded-2xl border border-white/10 bg-[#0b1220] p-4">
                    <div className="mb-2 flex flex-col gap-3 sm:flex-row">
                        <input
                            value={identifier}
                            onChange={(event) => {
                                setIdentifier(event.target.value);
                                if (!isAnimating) resetFlowState();
                            }}
                            placeholder="Enter email or phone (+1555...)"
                            className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none focus:border-cyan-400"
                        />
                        <button
                            type="button"
                            onClick={runAnimation}
                            disabled={isAnimating || !identifierMeta.valid}
                            className="rounded-lg bg-cyan-500 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-cyan-400 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            {isAnimating ? 'Checking…' : 'Check Identifier'}
                        </button>
                    </div>

                    <p className="mb-4 text-xs text-slate-400">
                        Detected type: <span className={identifierMeta.valid ? 'text-cyan-300' : 'text-red-300'}>{identifierMeta.type}</span>
                        {identifier && !identifierMeta.valid && ' · Enter a valid email or E.164-style phone number.'}
                    </p>

                    <div className="h-[520px] w-full overflow-hidden rounded-xl border border-slate-800/90">
                        <ReactFlow
                            nodes={nodes}
                            edges={edges}
                            onNodesChange={onNodesChange}
                            onEdgesChange={onEdgesChange}
                            nodesDraggable={false}
                            nodesConnectable={false}
                            elementsSelectable={false}
                            panOnDrag
                            fitView
                            fitViewOptions={{padding: 0.12, minZoom: 0.45}}
                            minZoom={0.4}
                            maxZoom={1.2}
                            proOptions={{hideAttribution: true}}
                        >
                            <Background color="#1e293b" gap={24} size={1} />
                            <Controls showInteractive={false} className="!bg-slate-900/80" />
                        </ReactFlow>
                    </div>
                </div>

                <aside className="space-y-4 rounded-2xl border border-white/10 bg-[#0b1220] p-4">
                    <div>
                        <p className="text-xs uppercase tracking-[0.16em] text-cyan-300/80">API Request</p>
                        <pre className="mt-2 overflow-auto rounded-lg border border-slate-800 bg-slate-950 p-3 text-xs text-slate-200">{JSON.stringify(simulatedRequest, null, 2)}</pre>
                    </div>
                    <div>
                        <p className="text-xs uppercase tracking-[0.16em] text-cyan-300/80">Simulated Response</p>
                        <pre className="mt-2 overflow-auto rounded-lg border border-slate-800 bg-slate-950 p-3 text-xs text-slate-200">{JSON.stringify(simulatedResponse, null, 2)}</pre>
                    </div>
                    <div className="rounded-lg border border-slate-800 bg-slate-950 p-3 text-xs text-slate-300">
                        <p className="mb-2 font-semibold">Execution Log</p>
                        <div className="max-h-36 space-y-1 overflow-auto text-slate-400">
                            {eventLog.map((entry) => (
                                <p key={entry}>{entry}</p>
                            ))}
                        </div>
                    </div>
                    <div className="rounded-lg border border-slate-800 bg-slate-950 p-3 text-xs text-slate-400">
                        Current step: <span className="text-cyan-300">{currentStep || 'idle'}</span>
                    </div>
                </aside>
            </div>
        </div>
    );
};

const IdentifierFlowVisualizerPage = () => (
    <DocsLayout title="Identifier Status Check Visualizer" subtitle="Interactive node-based flow for identifier lookup, branching logic, and next-step routing in authentication.">
        <section className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
            <ReactFlowProvider>
                <IdentifierFlowVisualizerContent />
            </ReactFlowProvider>
        </section>
    </DocsLayout>
);

export default IdentifierFlowVisualizerPage;
