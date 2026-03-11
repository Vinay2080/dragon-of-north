import DocsLayout from '../components/DocsLayout';
import {AnimatedFlow} from '../components/FlowDiagram';

const risks = [
    'Session hijacking through stolen browser state',
    'Cookie theft from vulnerable clients',
    'Long-lived tokens with broad blast radius',
    'No visibility into active sessions/devices',
    'Brute-force attacks against login endpoints',
];

const HomeDocsPage = () => (
    <DocsLayout
        title="Secure Identity & Authentication Platform"
        subtitle="A technical walkthrough of a hardened authentication architecture designed for modern distributed systems."
    >
        <section className="grid gap-6 lg:grid-cols-2">
            <article className="rounded-2xl border border-red-300/20 bg-red-500/5 p-6">
                <h3 className="text-xl font-semibold text-red-200">Why legacy auth fails</h3>
                <ul className="mt-4 space-y-2 text-sm text-slate-200">
                    {risks.map((risk) => <li key={risk}>• {risk}</li>)}
                </ul>
            </article>
            <article className="rounded-2xl border border-emerald-300/20 bg-emerald-500/5 p-6">
                <h3 className="text-xl font-semibold text-emerald-200">Modern solution</h3>
                <p className="mt-3 text-sm text-slate-200">This platform combines JWT, RSA signing, token rotation, revocation, and audit telemetry so every auth event is short-lived, observable, and revocable.</p>
                <p className="mt-3 text-sm text-slate-300">Goal: reduce credential abuse impact and maintain horizontal scalability.</p>
            </article>
        </section>
        <section className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
            <h3 className="mb-4 text-xl font-semibold">Authentication lifecycle demo</h3>
            <AnimatedFlow steps={['User login', 'Credential verification', 'JWT + refresh token issued', 'Short access token expires', 'Refresh rotates token', 'Session remains secure']} />
        </section>
    </DocsLayout>
);

export default HomeDocsPage;
