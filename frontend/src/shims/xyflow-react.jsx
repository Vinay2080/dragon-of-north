/* eslint-disable react-refresh/only-export-components */
import React from 'react';

export const MarkerType = {ArrowClosed: 'arrowclosed'};

const getCenter = (node) => ({
    x: (node.position?.x ?? 0) + ((node.style?.minWidth ? Number.parseInt(node.style.minWidth, 10) : 140) / 2),
    y: (node.position?.y ?? 0) + 20,
});

const ReactFlow = ({nodes = [], edges = [], children}) => {
    const nodeMap = Object.fromEntries(nodes.map((n) => [n.id, n]));

    return (
        <div className="relative h-full w-full overflow-auto rounded-xl bg-[#0b1220]">
            <svg className="pointer-events-none absolute inset-0 h-full w-full">
                {edges.map((edge) => {
                    const source = nodeMap[edge.source];
                    const target = nodeMap[edge.target];
                    if (!source || !target) return null;
                    const s = getCenter(source);
                    const t = getCenter(target);
                    return (
                        <g key={edge.id}>
                            <line x1={s.x} y1={s.y} x2={t.x} y2={t.y} stroke={edge.style?.stroke ?? '#64748b'} strokeWidth={edge.style?.strokeWidth ?? 1.5} strokeDasharray={edge.animated ? '6 4' : '0'} />
                            {edge.label && <text x={(s.x + t.x) / 2} y={(s.y + t.y) / 2 - 6} fill={edge.labelStyle?.fill ?? '#cbd5e1'} fontSize={edge.labelStyle?.fontSize ?? 10} textAnchor="middle">{edge.label}</text>}
                        </g>
                    );
                })}
            </svg>
            {nodes.map((node) => (
                <div key={node.id} className="absolute" style={{left: node.position?.x ?? 0, top: node.position?.y ?? 0, ...(node.style ?? {})}}>
                    {node.data?.label}
                </div>
            ))}
            {children}
        </div>
    );
};

export const Background = () => null;
export const Controls = () => null;

export default ReactFlow;
