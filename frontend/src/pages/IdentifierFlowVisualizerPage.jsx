import {useCallback, useMemo, useState} from 'react';
import {motion} from 'framer-motion';
import DocsLayout from '../components/DocsLayout';
import ReactFlow, {Background, Controls, ReactFlowProvider, useEdgesState, useNodesState} from 'reactflow';
import 'reactflow/dist/style.css';

const STEP_DELAY_MS = 320;

const NODE_META = {
    input: {label: 'User Input', tooltip: 'Collects an email address or E.164 phone number.'},
    api: {label: 'Identifier Status API', tooltip: 'Submits identifier payload and receives account status.'},
    existsDecision: {label: 'User Exists Decision', tooltip: 'Determines whether an account already exists.'},
    signup: {label: 'Signup Path', tooltip: 'New user route for account creation.'},
    status: {label: 'User Status Check', tooltip: 'Evaluates ACTIVE/LOCKED/CREATED style user state.'},
    verifiedDecision: {label: 'Email Verified Decision', tooltip: 'Checks verification state for existing user.'},
    login: {label: 'Login Path', tooltip: 'Existing verified users continue to login.'},
    verify: {label: 'Email Verification Path', tooltip: 'Existing unverified users continue to verification.'},
};

const baseNodes = [
    {id: 'input', position: {x: 80, y: 120}, data: {...NODE_META.input, active: false}, type: 'workflowNode'},
    {id: 'api', position: {x: 350, y: 120}, data: {...NODE_META.api, active: false}, type: 'workflowNode'},
    {id: 'existsDecision', position: {x: 620, y: 120}, data: {...NODE_META.existsDecision, active: false}, type: 'workflowNode'},
    {id: 'signup', position: {x: 880, y: 30}, data: {...NODE_META.signup, active: false}, type: 'workflowNode'},
    {id: 'status', position: {x: 880, y: 210}, data: {...NODE_META.status, active: false}, type: 'workflowNode'},
    {id: 'verifiedDecision', position: {x: 1140, y: 210}, data: {...NODE_META.verifiedDecision, active: false}, type: 'workflowNode'},
    {id: 'login', position: {x: 1400, y: 130}, data: {...NODE_META.login, active: false}, type: 'workflowNode'},
    {id: 'verify', position: {x: 1400, y: 290}, data: {...NODE_META.verify, active: false}, type: 'workflowNode'},
];

const baseEdges = [
    {id: 'e-input-api', source: 'input', target: 'api', label: 'submit'},
    {id: 'e-api-exists', source: 'api', target: 'existsDecision', label: 'status payload'},
    {id: 'e-exists-signup', source: 'existsDecision', target: 'signup', label: 'No'},
    {id: 'e-exists-status', source: 'existsDecision', target: 'status', label: 'Yes'},
    {id: 'e-status-verified', source: 'status', target: 'verifiedDecision', label: 'ACTIVE'},
    {id: 'e-verified-login', source: 'verifiedDecision', target: 'login', label: 'Yes'},
    {id: 'e-verified-email', source: 'verifiedDecision', target: 'verify', label: 'No'},
];

const scenarios = {
    new: {
        response: {exists: false, app_user_status: 'NOT_EXISTS', email_verified: false, next_action: 'SIGNUP'},
        nodes: ['input', 'api', 'existsDecision', 'signup'],
        edges: ['e-input-api', 'e-api-exists', 'e-exists-signup'],
    },
    verified: {
        response: {exists: true, app_user_status: 'ACTIVE', email_verified: true, next_action: 'LOGIN'},
        nodes: ['input', 'api', 'existsDecision', 'status', 'verifiedDecision', 'login'],
        edges: ['e-input-api', 'e-api-exists', 'e-exists-status', 'e-status-verified', 'e-verified-login'],
    },
    unverified: {
        response: {exists: true, app_user_status: 'ACTIVE', email_verified: false, next_action: 'EMAIL_VERIFICATION'},
        nodes: ['input', 'api', 'existsDecision', 'status', 'verifiedDecision', 'verify'],
        edges: ['e-input-api', 'e-api-exists', 'e-exists-status', 'e-status-verified', 'e-verified-email'],
    },
};

const identifyType = (value) => {
    const trimmed = value.trim();
    if (!trimmed) return {type: 'UNKNOWN', normalized: '', valid: false};

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (emailRegex.test(trimmed)) {
        return {type: 'EMAIL', normalized: trimmed.toLowerCase(), valid: true};
    }

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
const MotionNode = motion.div;

const WorkflowNode = ({data}) => (
    <MotionNode
        animate={{
            boxShadow: data.active ? '0 0 0 1px rgba(56, 189, 248, 0.85), 0 0 28px rgba(56, 189, 248, 0.6)' : '0 0 0 1px rgba(100, 116, 139, 0.55)',
            scale: data.active ? 1.025 : 1,
            borderColor: data.active ? 'rgba(56, 189, 248, 0.95)' : 'rgba(51, 65, 85, 0.8)',
        }}
        transition={{duration: 0.26}}
        className="group relative w-52 rounded-xl border bg-slate-900/95 px-4 py-3 text-left"
    >
        <p className="text-[11px] uppercase tracking-[0.16em] text-cyan-300/75">Node</p>
        <p className="mt-1 text-sm font-semibold text-slate-100">{data.label}</p>
        <div className="pointer-events-none absolute -top-12 left-1/2 z-20 hidden -translate-x-1/2 whitespace-nowrap rounded-md border border-slate-600 bg-slate-950 px-2 py-1 text-xs text-slate-200 shadow-xl group-hover:block">
            {data.tooltip}
        </div>
    </MotionNode>
);

const nodeTypes = {workflowNode: WorkflowNode};

const IdentifierFlowVisualizerContent = () => {
    const [identifier, setIdentifier] = useState('');
    const [isAnimating, setAnimating] = useState(false);
    const [scenarioName, setScenarioName] = useState('verified');
    const [eventLog, setEventLog] = useState(['Idle. Enter an email or phone and click "Check Identifier".']);

    const [nodes, setNodes, onNodesChange] = useNodesState(baseNodes);
    const [edges, setEdges, onEdgesChange] = useEdgesState(baseEdges);

    const identifierMeta = useMemo(() => identifyType(identifier), [identifier]);

    const simulatedRequest = useMemo(
        () => ({
            identifier: identifierMeta.normalized || '<enter-email-or-phone>',
            identifier_type: identifierMeta.type === 'INVALID' || identifierMeta.type === 'UNKNOWN' ? 'UNKNOWN' : identifierMeta.type,
        }),
        [identifierMeta],
    );

    const simulatedResponse = useMemo(() => scenarios[scenarioName].response, [scenarioName]);

    const repaintGraph = useCallback((nodeId, edgeId) => {
        setNodes((prev) => prev.map((node) => ({...node, data: {...node.data, active: node.id === nodeId}})));
        setEdges((prev) =>
            prev.map((edge) => {
                const active = edge.id === edgeId;
                return {
                    ...edge,
                    animated: active,
                    className: active ? 'edge-active' : 'edge-default',
                    style: {stroke: active ? '#22d3ee' : '#475569', strokeWidth: active ? 2.8 : 1.3},
                    labelStyle: {fill: active ? '#e2e8f0' : '#94a3b8', fontSize: 11},
                };
            }),
        );
    }, [setEdges, setNodes]);

    const resetGraph = useCallback(() => {
        repaintGraph('', '');
    }, [repaintGraph]);

    const appendLog = useCallback((message) => {
        setEventLog((prev) => [...prev.slice(-7), message]);
    }, []);

    const runAnimation = useCallback(async () => {
        if (!identifierMeta.valid || isAnimating) return;

        setAnimating(true);
        setEventLog([`Identifier detected as ${identifierMeta.type}.`]);
        resetGraph();

        const pickedScenario = inferScenario(identifierMeta.normalized);
        setScenarioName(pickedScenario);
        appendLog(`Calling Identifier Status API for ${identifierMeta.normalized}.`);

        const {nodes: pathNodes, edges: pathEdges} = scenarios[pickedScenario];

        for (let i = 0; i < pathNodes.length; i += 1) {
            const nodeId = pathNodes[i];
            const edgeId = pathEdges[i - 1] ?? '';
            repaintGraph(nodeId, edgeId);
            appendLog(`Step ${i + 1}/${pathNodes.length}: ${NODE_META[nodeId].label}`);
            await wait(STEP_DELAY_MS);
        }

        appendLog(`Completed path: ${scenarios[pickedScenario].response.next_action}`);
        setAnimating(false);
    }, [appendLog, identifierMeta, isAnimating, repaintGraph, resetGraph]);

    return (
        <div className="space-y-5">
            <style>{`
                .react-flow__edge.edge-active path {
                    stroke-dasharray: 10;
                    animation: flow-dash 0.36s linear infinite;
                }
                .react-flow__edge.edge-default path {
                    stroke-dasharray: none;
                }
                @keyframes flow-dash {
                    from { stroke-dashoffset: 22; }
                    to { stroke-dashoffset: 0; }
                }
            `}</style>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(310px,1fr)]">
                <div className="rounded-2xl border border-white/10 bg-[#0b1220] p-4">
                    <div className="mb-2 flex flex-col gap-3 sm:flex-row">
                        <input
                            value={identifier}
                            onChange={(event) => setIdentifier(event.target.value)}
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
                            nodeTypes={nodeTypes}
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
                        <div className="max-h-28 space-y-1 overflow-auto text-slate-400">
                            {eventLog.map((entry) => (
                                <p key={entry}>{entry}</p>
                            ))}
                        </div>
                    </div>
                    <div className="rounded-lg border border-slate-800 bg-slate-950 p-3 text-xs text-slate-400">
                        Use <code className="text-cyan-300">new@domain.com</code> for Signup, <code className="text-cyan-300">unverified@domain.com</code> for Email Verification, or any other valid identifier for Login.
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
