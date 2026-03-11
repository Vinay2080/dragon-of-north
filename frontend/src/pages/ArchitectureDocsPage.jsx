import DocsLayout from '../components/DocsLayout';
import SystemOverviewDiagram from '../components/diagrams/SystemOverviewDiagram';
import AuthSequenceDiagram from '../components/diagrams/AuthSequenceDiagram';
import AuthStateMachineDiagram from '../components/diagrams/AuthStateMachineDiagram';
import SecurityPipelineDiagram from '../components/diagrams/SecurityPipelineDiagram';
import DatabaseErDiagram from '../components/diagrams/DatabaseErDiagram';

const ArchitectureDocsPage = () => (
    <DocsLayout title="Architecture" subtitle="Interactive architecture exploration for identity, token lifecycle, and defensive controls.">
        <section className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
            <h3 className="mb-4 text-xl font-semibold">System Overview (C4 Context)</h3>
            <SystemOverviewDiagram />
        </section>

        <section className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
            <h3 className="mb-4 text-xl font-semibold">Authentication Flows (Sequence Diagram)</h3>
            <AuthSequenceDiagram />
        </section>

        <section className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
            <h3 className="mb-4 text-xl font-semibold">Authentication State Machine</h3>
            <AuthStateMachineDiagram />
        </section>

        <section className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
            <h3 className="mb-4 text-xl font-semibold">Security Pipeline</h3>
            <SecurityPipelineDiagram />
        </section>

        <section className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
            <h3 className="mb-4 text-xl font-semibold">Database Model (ER)</h3>
            <DatabaseErDiagram />
        </section>
    </DocsLayout>
);

export default ArchitectureDocsPage;
