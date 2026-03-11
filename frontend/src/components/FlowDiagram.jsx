import {useEffect, useRef, useState} from 'react';

export const AnimatedFlow = ({steps}) => {
    const [active, setActive] = useState(0);

    useEffect(() => {
        const timer = setInterval(() => {
            setActive((prev) => (prev + 1) % steps.length);
        }, 1400);
        return () => clearInterval(timer);
    }, [steps.length]);

    return (
        <div className="rounded-xl border border-cyan-400/20 bg-[#0b1220] p-4">
            <div className="flex flex-wrap items-center gap-2">
                {steps.map((step, index) => (
                    <div key={step} className="flex items-center gap-2">
                        <div className={`rounded-lg border px-3 py-2 text-xs transition-all duration-500 ${index === active ? 'border-cyan-300 bg-cyan-300/15 text-cyan-100 shadow-[0_0_16px_rgba(103,232,249,0.35)]' : 'border-white/15 text-slate-300'}`}>
                            {step}
                        </div>
                        {index < steps.length - 1 && <span className="text-slate-500">→</span>}
                    </div>
                ))}
            </div>
        </div>
    );
};

export const DeploymentPipelineSimulator = ({steps}) => {
    const [currentStep, setCurrentStep] = useState(-1);
    const [completedSteps, setCompletedSteps] = useState([]);
    const [running, setRunning] = useState(false);
    const [logs, setLogs] = useState([]);
    const timersRef = useRef([]);

    const resetTimers = () => {
        timersRef.current.forEach(clearTimeout);
        timersRef.current = [];
    };

    const appendLog = (message) => {
        const stamp = new Date().toLocaleTimeString();
        setLogs((prev) => [...prev.slice(-10), `[${stamp}] ${message}`]);
    };

    const runDeployment = () => {
        resetTimers();
        setRunning(true);
        setCurrentStep(0);
        setCompletedSteps([]);
        setLogs([]);

        steps.forEach((step, index) => {
            const startDelay = index * 1200;
            const startTimer = setTimeout(() => {
                setCurrentStep(index);
                appendLog(`${step}: started`);
            }, startDelay);

            const finishTimer = setTimeout(() => {
                setCompletedSteps((prev) => [...prev, index]);
                appendLog(`${step}: success`);
                if (index === steps.length - 1) {
                    setCurrentStep(-1);
                    setRunning(false);
                }
            }, startDelay + 850);

            timersRef.current.push(startTimer, finishTimer);
        });
    };

    return (
        <div className="space-y-4 rounded-xl border border-cyan-400/30 bg-gradient-to-b from-[#0a1220]/90 to-[#0f172a]/90 p-4 shadow-[0_0_25px_rgba(34,211,238,0.08)]">
            <div className="flex items-center justify-between">
                <p className="text-sm text-slate-300">Run the delivery flow stage-by-stage with live status updates.</p>
                <button
                    type="button"
                    onClick={runDeployment}
                    disabled={running}
                    className="rounded-lg border border-cyan-300/60 bg-cyan-400/10 px-4 py-2 text-sm font-medium text-cyan-100 transition hover:bg-cyan-400/20 disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {running ? 'Deploying...' : 'Run Deployment'}
                </button>
            </div>

            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                {steps.map((step, index) => {
                    const done = completedSteps.includes(index);
                    const active = currentStep === index;
                    return (
                        <div key={step} className={`rounded-lg border px-3 py-3 text-sm transition-all ${done ? 'border-emerald-400/40 bg-emerald-400/10 text-emerald-100' : active ? 'border-cyan-300/60 bg-cyan-300/15 text-cyan-100 shadow-[0_0_18px_rgba(103,232,249,0.3)]' : 'border-white/10 bg-white/[0.02] text-slate-300'}`}>
                            <div className="mb-2 flex items-center justify-between gap-2">
                                <span>{step}</span>
                                <span className={`h-2.5 w-2.5 rounded-full ${done ? 'bg-emerald-300 shadow-[0_0_10px_rgba(110,231,183,0.9)]' : active ? 'bg-cyan-300 shadow-[0_0_10px_rgba(103,232,249,0.9)]' : 'bg-slate-500'}`} />
                            </div>
                            <div className="h-1.5 overflow-hidden rounded-full bg-black/30">
                                <div className={`h-full transition-all duration-500 ${done ? 'w-full bg-emerald-300' : active ? 'w-2/3 animate-pulse bg-cyan-300' : 'w-0 bg-transparent'}`} />
                            </div>
                        </div>
                    );
                })}
            </div>

            <div className="rounded-lg border border-white/10 bg-black/40 p-3 font-mono text-xs text-green-300">
                <p className="mb-2 text-slate-400">pipeline.log</p>
                <div className="space-y-1">
                    {logs.length === 0 ? <p className="text-slate-500">Press "Run Deployment" to stream stage logs.</p> : logs.map((log) => <p key={log}>{log}</p>)}
                </div>
            </div>
        </div>
    );
};

export const VerticalFlow = ({steps}) => (
    <div className="space-y-2 rounded-xl border border-violet-400/20 bg-[#100f22] p-4">
        {steps.map((step, index) => (
            <div key={step} className="flex items-center gap-2 text-sm">
                <span className="inline-flex h-6 w-6 items-center justify-center rounded-full border border-violet-300/40 bg-violet-300/10 text-xs text-violet-100">{index + 1}</span>
                <span>{step}</span>
            </div>
        ))}
    </div>
);
