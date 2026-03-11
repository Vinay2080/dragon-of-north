import {useEffect, useMemo, useState} from 'react';
import DiagramShell from './DiagramShell';
import {baseEdge} from './edgeStyles';
import {securityPipeline} from '../../diagrams_and_readme/diagramDefinitions';

const SecurityPipelineDiagram = () => {
    const [active, setActive] = useState(0);

    useEffect(() => {
        const timer = setInterval(() => setActive((prev) => (prev + 1) % securityPipeline.length), 900);
        return () => clearInterval(timer);
    }, []);

    const nodes = useMemo(() => securityPipeline.map((stage, index) => ({
        id: stage,
        data: {label: stage},
        position: {x: 40 + index * 200, y: 120},
        style: {
            borderRadius: 10,
            padding: 10,
            minWidth: 160,
            border: `1px solid ${index === active ? '#22d3ee' : '#475569'}`,
            background: index === active ? 'rgba(34,211,238,0.15)' : '#141c2f',
            color: '#e2e8f0',
            boxShadow: index === active ? '0 0 16px rgba(34,211,238,0.3)' : 'none',
            textAlign: 'center',
        },
    })), [active]);

    const edges = useMemo(() => securityPipeline.slice(0, -1).map((stage, index) => ({
        id: `edge-${index}`,
        source: stage,
        target: securityPipeline[index + 1],
        ...baseEdge,
        animated: index < active,
        style: {stroke: index < active ? '#a78bfa' : '#475569', strokeWidth: index < active ? 2.4 : 1.4},
    })), [active]);

    return (
        <div className="space-y-3">
            <DiagramShell nodes={nodes} edges={edges} className="h-[300px]" />
            <p className="text-sm text-slate-300">Animated request position: <span className="text-cyan-200">{securityPipeline[active]}</span></p>
        </div>
    );
};

export default SecurityPipelineDiagram;
