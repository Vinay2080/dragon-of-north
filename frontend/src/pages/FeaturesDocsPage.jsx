import DocsLayout from '../components/DocsLayout';
import {AnimatedFlow} from '../components/FlowDiagram';

const features = [
    {
        name: 'JWT-Based Authentication',
        problem: 'Server-side memory sessions break across replicas and load balancers.',
        why: 'Stateless tokens allow any API node to authenticate without shared session memory.',
        flow: ['User logs in', 'Server verifies credentials', 'Server issues JWT', 'Client sends token on each request'],
    },
    {name: 'RSA Token Signing', problem: 'Symmetric secrets are risky to distribute to many services.', why: 'Private key signs once; public keys verify anywhere without exposing signer secret.', flow: ['Private key signs JWT', 'Gateway/API uses public key', 'Tampered token fails verification']},
    {name: 'Refresh Token Rotation', problem: 'Static refresh tokens can be replayed after theft.', why: 'Every refresh invalidates previous token, making replay detectable and blockable.', flow: ['Client sends refresh token', 'Server validates token family', 'New access + refresh token issued', 'Old refresh token revoked']},
    {name: 'Access Token Expiration', problem: 'Long-lived bearer tokens extend attacker access window.', why: '15-minute access tokens sharply reduce theft impact.', flow: ['Access token minted', 'Token used briefly', 'Token expires quickly', 'Refresh path required']},
    {name: 'Refresh Token Revocation', problem: 'Compromised long-lived sessions persist unseen.', why: 'Immediate revocation cuts off compromised sessions in real-time.', flow: ['Threat detected', 'Token marked revoked', 'Next refresh denied', 'User re-auth required']},
    {name: 'Device-Aware Sessions', problem: 'Users cannot see suspicious active sessions.', why: 'Session inventory enables remote device sign-out.', flow: ['Device fingerprint stored', 'Session listed in dashboard', 'User revokes unknown device']},
    {name: 'Multi-Identifier Login', problem: 'Rigid login identifiers increase account recovery friction.', why: 'Email, username, or phone improve accessibility without lowering security.', flow: ['User provides identifier', 'Backend resolves account', 'Password/OTP challenge continues']},
    {name: 'OAuth2 Integration', problem: 'Password-only login increases credential handling burden.', why: 'Federated login delegates identity proofing to trusted providers (e.g., Google).', flow: ['User chooses provider', 'OAuth consent flow', 'Backend validates provider token', 'Local session created']},
    {name: 'OTP Verification', problem: 'Automated signups and fake accounts abuse resources.', why: 'OTP confirms control of email/phone and deters scripted abuse.', flow: ['Signup initiated', 'OTP generated + sent', 'User submits OTP', 'Account marked verified']},
    {name: 'Role-Based Access Control (RBAC)', problem: 'All authenticated users gain equal privileges.', why: 'Roles and permissions enforce least-privilege access.', flow: ['User authenticated', 'Roles loaded', 'Policy check executed', 'Request allowed or denied']},
    {name: 'Redis Rate Limiting', problem: 'Attackers can brute-force login endpoints at scale.', why: 'Distributed counters throttle abusive clients before credential stuffing succeeds.', flow: ['Request arrives', 'Redis counter incremented', 'Threshold exceeded?', 'Block or continue']},
    {name: 'Security Audit Logging', problem: 'No reliable trail for incident response and forensics.', why: 'Structured auth-event logs reveal anomalies and support investigations.', flow: ['Auth event occurs', 'Context metadata attached', 'Event persisted', 'Alerting/analysis pipeline reads logs']},
    {name: 'Database Migration', problem: 'Manual schema updates cause drift and deployment risk.', why: 'Versioned migrations provide deterministic, rollback-safe schema changes.', flow: ['Migration script committed', 'CI validates', 'Deploy applies migration', 'Schema version recorded']},
    {name: 'Database Indexing', problem: 'Auth lookups degrade under load without targeted indexes.', why: 'Indexes on user identifiers and token hashes maintain low-latency auth checks.', flow: ['Query login identifier', 'Index seek', 'Fast row retrieval', 'Auth decision returned']},
    {name: 'Token Hashing in Database', problem: 'Plain refresh tokens in DB become bearer secrets on breach.', why: 'Hashing stored refresh tokens prevents direct token reuse after data exposure.', flow: ['Token generated', 'Hash persisted', 'Presented token hashed for compare', 'Match grants refresh']},
];

const FeaturesDocsPage = () => (
    <DocsLayout title="Security Features" subtitle="Each feature exists to close a specific failure mode in authentication systems.">
        <section className="grid gap-4">
            {features.map((item) => (
                <article key={item.name} className="rounded-2xl border border-white/10 bg-white/[0.03] p-5">
                    <h3 className="text-lg font-semibold text-cyan-100">{item.name}</h3>
                    <p className="mt-2 text-sm"><span className="font-semibold text-red-200">Problem without it:</span> {item.problem}</p>
                    <p className="mt-1 text-sm"><span className="font-semibold text-emerald-200">Why it exists:</span> {item.why}</p>
                    <div className="mt-4"><AnimatedFlow steps={item.flow} /></div>
                </article>
            ))}
        </section>
    </DocsLayout>
);

export default FeaturesDocsPage;
