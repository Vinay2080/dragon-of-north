import {useEffect, useMemo, useState} from 'react';
import DiagramShell from './DiagramShell';
import {baseEdge} from './edgeStyles';
import {deploymentTopology} from '../../diagrams_and_readme/diagramDefinitions';

const positions = {
    User: {x: 40, y: 160},
    'Vercel CDN': {x: 260, y: 60},
    'DuckDNS Domain': {x: 260, y: 250},
    'Spring Boot Backend': {x: 520, y: 160},
    PostgreSQL: {x: 800, y: 80},
    Redis: {x: 800, y: 250},
};

const pathEdges = [
    ['User', 'Vercel CDN', 'HTTPS'],
    ['Vercel CDN', 'DuckDNS Domain', 'Domain resolve'],
    ['DuckDNS Domain', 'Spring Boot Backend', 'API route'],
    ['Spring Boot Backend', 'PostgreSQL', 'SQL'],
    ['Spring Boot Backend', 'Redis', 'Rate limit/cache'],
];

const DeploymentTopologyDiagram = () => {
    const [activeEdge, setActiveEdge] = useState(0);

    useEffect(() => {
        const timer = setInterval(() => setActiveEdge((prev) => (prev + 1) % pathEdges.length), 1100);
        return () => clearInterval(timer);
    }, []);

    const nodes = useMemo(() => deploymentTopology.nodes.map((node) => ({
        id: node,
        data: {label: node},
        position: positions[node],
        style: {
            borderRadius: 12,
            border: '1px solid rgba(148,163,184,0.45)',
            background: '#151d2e',
            color: '#e2e8f0',
            padding: 10,
            minWidth: 170,
            textAlign: 'center',
        },
    })), []);

    const edges = useMemo(() => pathEdges.map(([source, target, label], index) => ({
        id: `${source}-${target}`,
        source,
        target,
        label,
        ...baseEdge,
        animated: index <= activeEdge,
        style: {stroke: index === activeEdge ? '#22d3ee' : '#a78bfa', strokeWidth: index === activeEdge ? 2.8 : 1.8},
        labelStyle: {fill: '#e2e8f0', fontSize: 10},
    })), [activeEdge]);

    return (
        <div className="space-y-3">
            <DiagramShell nodes={nodes} edges={edges} className="h-[430px]" />
            <p className="text-sm text-slate-300">Current request hop: <span className="text-orange-200">{pathEdges[activeEdge][0]} → {pathEdges[activeEdge][1]}</span></p>
        </div>
    );
};

export default DeploymentTopologyDiagram;
