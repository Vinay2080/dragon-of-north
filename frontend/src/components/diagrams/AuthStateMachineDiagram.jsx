import {useMemo, useState} from 'react';
import DiagramShell from './DiagramShell';
import {baseEdge} from './edgeStyles';
import {stateMachineDefinition} from '../../diagrams_and_readme/diagramDefinitions';

const positions = {
    Unauthenticated: {x: 20, y: 120},
    'Credentials Submitted': {x: 220, y: 40},
    'Password Verified': {x: 440, y: 40},
    'Token Issued': {x: 650, y: 40},
    'Session Active': {x: 860, y: 120},
    'Token Expired': {x: 650, y: 240},
    'Refresh Attempted': {x: 430, y: 240},
    'Session Revoked': {x: 220, y: 240},
};

const AuthStateMachineDiagram = () => {
    const [currentState, setCurrentState] = useState('Unauthenticated');

    const nodes = useMemo(() => stateMachineDefinition.states.map((state) => ({
        id: state,
        data: {label: state},
        position: positions[state],
        style: {
            borderRadius: 999,
            padding: '10px 16px',
            border: `1px solid ${currentState === state ? '#fb923c' : 'rgba(148,163,184,0.45)'}`,
            background: currentState === state ? 'rgba(251,146,60,0.15)' : '#171f31',
            color: '#e2e8f0',
            boxShadow: currentState === state ? '0 0 20px rgba(251,146,60,0.28)' : 'none',
        },
    })), [currentState]);

    const edges = useMemo(() => stateMachineDefinition.transitions.map((transition, index) => ({
        id: `transition-${index}`,
        source: transition.from,
        target: transition.to,
        label: transition.label,
        ...baseEdge,
        style: {stroke: transition.from === currentState ? '#fb923c' : '#64748b', strokeWidth: transition.from === currentState ? 2.5 : 1.5},
        labelStyle: {fill: '#cbd5e1', fontSize: 10},
    })), [currentState]);

    const available = stateMachineDefinition.transitions.filter((t) => t.from === currentState);

    return (
        <div className="space-y-3">
            <DiagramShell nodes={nodes} edges={edges} className="h-[380px]" />
            <div className="rounded-lg border border-white/15 bg-black/20 p-3 text-sm">
                <span className="font-semibold text-orange-200">Current state:</span> {currentState}
            </div>
            <div className="flex flex-wrap gap-2">
                {available.map((transition) => (
                    <button key={transition.label} onClick={() => setCurrentState(transition.to)} className="rounded-md border border-orange-300/60 bg-orange-300/10 px-3 py-1.5 text-xs">
                        {transition.label} → {transition.to}
                    </button>
                ))}
                <button onClick={() => setCurrentState('Unauthenticated')} className="rounded-md border border-white/20 px-3 py-1.5 text-xs">Reset</button>
            </div>
        </div>
    );
};

export default AuthStateMachineDiagram;
