import {useMemo, useState} from 'react';
import DiagramShell from './DiagramShell';
import {baseEdge} from './edgeStyles';
import {erDefinition} from '../../diagrams_and_readme/diagramDefinitions';

const positions = {
    users: {x: 80, y: 60},
    sessions: {x: 360, y: 60},
    refresh_tokens: {x: 680, y: 60},
    roles: {x: 80, y: 260},
    audit_logs: {x: 360, y: 260},
};

const DatabaseErDiagram = () => {
    const [activeTable, setActiveTable] = useState('users');

    const nodes = useMemo(() => Object.keys(erDefinition.tables).map((table) => ({
        id: table,
        data: {label: table},
        position: positions[table],
        style: {
            borderRadius: 10,
            border: `1px solid ${activeTable === table ? '#22d3ee' : '#475569'}`,
            background: activeTable === table ? 'rgba(34,211,238,0.14)' : '#121a28',
            color: '#e2e8f0',
            padding: 10,
            minWidth: 180,
            textTransform: 'lowercase',
            boxShadow: activeTable === table ? '0 0 14px rgba(34,211,238,0.3)' : 'none',
        },
    })), [activeTable]);

    const edges = useMemo(() => erDefinition.relations.map(([source, target, label], index) => ({
        id: `rel-${index}`,
        source,
        target,
        label,
        ...baseEdge,
        style: {stroke: activeTable === source || activeTable === target ? '#fb923c' : '#64748b', strokeWidth: 2},
        labelStyle: {fill: '#fdba74', fontSize: 10},
    })), [activeTable]);

    return (
        <div className="space-y-4">
            <DiagramShell nodes={nodes} edges={edges} className="h-[430px]" />
            <div className="grid gap-3 md:grid-cols-3">
                {Object.keys(erDefinition.tables).map((table) => (
                    <button key={table} onMouseEnter={() => setActiveTable(table)} onClick={() => setActiveTable(table)} className={`rounded-lg border px-3 py-2 text-left text-xs ${table === activeTable ? 'border-cyan-300/70 bg-cyan-300/10' : 'border-white/20'}`}>
                        {table}
                    </button>
                ))}
            </div>
            <div className="rounded-lg border border-white/15 bg-black/20 p-3 text-sm">
                <p className="font-semibold text-cyan-200">{activeTable} fields</p>
                <ul className="mt-2 list-inside list-disc text-slate-300">
                    {erDefinition.tables[activeTable].map((field) => <li key={field}>{field}</li>)}
                </ul>
            </div>
        </div>
    );
};

export default DatabaseErDiagram;
