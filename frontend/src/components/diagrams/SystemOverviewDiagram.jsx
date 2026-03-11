import {useMemo, useState} from 'react';
import DiagramShell from './DiagramShell';
import {baseEdge} from './edgeStyles';
import {systemOverview} from '../../diagrams_and_readme/diagramDefinitions';

const SystemOverviewDiagram = () => {
    const [activeNode, setActiveNode] = useState('backend');

    const nodes = useMemo(() => systemOverview.nodes.map((node) => ({
        id: node.id,
        data: {label: node.label},
        position: node.position,
        style: {
            borderRadius: 12,
            border: `1px solid ${activeNode === node.id ? '#22d3ee' : 'rgba(148,163,184,0.3)'}`,
            background: activeNode === node.id ? 'rgba(34,211,238,0.15)' : '#131a27',
            color: '#e2e8f0',
            boxShadow: activeNode === node.id ? '0 0 20px rgba(34,211,238,0.35)' : 'none',
            padding: 10,
            minWidth: 160,
            textAlign: 'center',
        },
    })), [activeNode]);

    const edges = useMemo(() => systemOverview.edges.map(([source, target, label], idx) => {
        const highlighted = activeNode && (source === activeNode || target === activeNode);
        return {
            id: `edge-${idx}`,
            source,
            target,
            label,
            ...baseEdge,
            animated: highlighted,
            style: {stroke: highlighted ? '#fb923c' : '#818cf8', strokeWidth: highlighted ? 2.6 : 1.6},
            labelStyle: {fill: highlighted ? '#fdba74' : '#cbd5e1', fontSize: 11},
        };
    }), [activeNode]);

    const selected = systemOverview.nodes.find((node) => node.id === activeNode);

    return (
        <div className="space-y-4">
            <DiagramShell nodes={nodes} edges={edges} />
            <div className="grid gap-3 md:grid-cols-2">
                {systemOverview.nodes.map((node) => (
                    <button
                        key={node.id}
                        onMouseEnter={() => setActiveNode(node.id)}
                        onClick={() => setActiveNode(node.id)}
                        className={`rounded-lg border px-3 py-2 text-left text-xs transition ${activeNode === node.id ? 'border-cyan-300/70 bg-cyan-300/10 text-cyan-100' : 'border-white/15 text-slate-300 hover:border-violet-300/50'}`}
                    >
                        {node.label}
                    </button>
                ))}
            </div>
            {selected && (
                <div className="rounded-xl border border-orange-300/30 bg-orange-300/10 p-4 text-sm">
                    <p className="font-semibold text-orange-100">{selected.label} responsibilities</p>
                    <ul className="mt-2 list-inside list-disc text-slate-200">
                        {selected.responsibilities.map((item) => <li key={item}>{item}</li>)}
                    </ul>
                </div>
            )}
        </div>
    );
};

export default SystemOverviewDiagram;
