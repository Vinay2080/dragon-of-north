import DocsLayout from '../components/DocsLayout';
import {DeploymentPipelineSimulator} from '../components/FlowDiagram';
import DeploymentTopologyDiagram from '../components/diagrams/DeploymentTopologyDiagram';

const DeploymentDocsPage = () => (
    <DocsLayout title="Deployment" subtitle="Interactive deployment topology and delivery pipeline for secure identity services.">
        <section className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
            <h3 className="mb-4 text-xl font-semibold">CI/CD pipeline simulation</h3>
            <DeploymentPipelineSimulator steps={['Developer pushes code', 'Build', 'Test', 'Database migration', 'Backend deploy', 'Frontend deploy']} />
        </section>
        <section className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
            <h3 className="mb-4 text-xl font-semibold">Deployment Topology</h3>
            <DeploymentTopologyDiagram />
        </section>
    </DocsLayout>
);

export default DeploymentDocsPage;
