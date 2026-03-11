import ReactFlow, {Background, Controls} from '@xyflow/react';


const DiagramShell = ({nodes, edges, fitView = true, className = 'h-[420px]'}) => (
    <div className={`rounded-2xl border border-white/10 bg-[#0d111b] p-2 ${className}`}>
        <ReactFlow nodes={nodes} edges={edges} fitView={fitView} proOptions={{hideAttribution: true}}>
            <Background color="#334155" gap={24} size={1} />
            <Controls className="!bg-[#111827] !border !border-white/20" />
        </ReactFlow>
    </div>
);

export default DiagramShell;
