import {useEffect, useMemo, useState} from 'react';
import DiagramShell from './DiagramShell';
import {baseEdge} from './edgeStyles';
import {sequenceDefinitions} from '../../diagrams_and_readme/diagramDefinitions';

const flows = Object.keys(sequenceDefinitions.flows);
const scenarios = ['Success', 'Invalid Password', 'User Not Found', 'Token Expired', 'Replay Attack'];

const parseMessage = (message) => {
    const [left, payload] = message.split(': ');
    if (!payload || !left.includes('→')) {
        return {source: 'Frontend', target: 'Backend', payload: message};
    }
    const [source, target] = left.split(' → ');
    return {source, target, payload};
};

const AuthSequenceDiagram = () => {
    const [flow, setFlow] = useState('Password Login');
    const [scenario, setScenario] = useState('Success');
    const [stepIndex, setStepIndex] = useState(0);

    const messages = useMemo(() => sequenceDefinitions.flows[flow][scenario] ?? [], [flow, scenario]);

    useEffect(() => {
        if (messages.length < 2) return undefined;
        const timer = setInterval(() => {
            setStepIndex((prev) => (prev + 1) % messages.length);
        }, 1400);
        return () => clearInterval(timer);
    }, [messages]);

    const nodes = useMemo(() => sequenceDefinitions.actors.map((actor, index) => ({
        id: actor,
        data: {label: actor},
        position: {x: index * 180 + 20, y: 20},
        style: {borderRadius: 8, border: '1px solid rgba(148,163,184,0.4)', background: '#121826', color: '#e2e8f0', minWidth: 130, textAlign: 'center'},
    })), []);

    const edges = useMemo(() => messages.map((message, index) => {
        const parsed = parseMessage(message);
        const active = index <= stepIndex;
        const source = sequenceDefinitions.actors.includes(parsed.source) ? parsed.source : 'Frontend';
        const target = sequenceDefinitions.actors.includes(parsed.target) ? parsed.target : 'Backend';
        return {
            id: `${source}-${target}-${index}`,
            source,
            target,
            label: parsed.payload,
            ...baseEdge,
            animated: active,
            style: {stroke: active ? '#22d3ee' : '#475569', strokeWidth: active ? 2.3 : 1.4},
            labelStyle: {fill: active ? '#67e8f9' : '#94a3b8', fontSize: 10},
        };
    }), [messages, stepIndex]);

    return (
        <div className="space-y-4">
            <div className="flex flex-wrap gap-2">
                {flows.map((item) => (
                    <button
                        key={item}
                        onClick={() => {
                            setFlow(item);
                            setStepIndex(0);
                        }}
                        className={`rounded-md border px-3 py-1.5 text-xs ${flow === item ? 'border-cyan-300 bg-cyan-300/10' : 'border-white/20'}`}
                    >
                        {item}
                    </button>
                ))}
            </div>
            <div className="flex flex-wrap gap-2">
                {scenarios.map((item) => (
                    <button
                        key={item}
                        onClick={() => {
                            setScenario(item);
                            setStepIndex(0);
                        }}
                        className={`rounded-md border px-3 py-1.5 text-xs ${scenario === item ? 'border-violet-300 bg-violet-300/10' : 'border-white/20'}`}
                    >
                        {item}
                    </button>
                ))}
            </div>
            <DiagramShell nodes={nodes} edges={edges} className="h-[460px]" />
            <div className="rounded-lg border border-white/15 bg-black/20 p-3 text-sm text-slate-300">
                Step {Math.min(stepIndex + 1, messages.length)} / {messages.length}: {messages[stepIndex] ?? 'No messages defined'}
            </div>
        </div>
    );
};

export default AuthSequenceDiagram;
