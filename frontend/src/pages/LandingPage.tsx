const legacyIssues = [
    'Session hijacking',
    'Cookie theft',
    'Long-lived tokens',
    'No device visibility',
    'Brute-force login attacks',
];

const securityFeatures = [
    'Token Rotation',
    'Device Tracking',
    'Session Revocation',
    'Rate Limiting',
    'Brute Force Protection',
    'Audit Logs',
];

const architectureChain = ['Frontend', 'API Gateway', 'Auth Service', 'Token Service', 'Database'];
const authLifecycle = [
    'User Login',
    'Credential Verification',
    'JWT Issued',
    'Access Token Expires',
    'Refresh Token Rotates',
    'Session Continues Securely',
];
const tokenFlow = ['User', 'Auth Service', 'JWT Issued', 'API Access', 'Token Expiry', 'Refresh Rotation'];

const LandingPage = () => {
    return (
        <div className="relative min-h-screen overflow-hidden bg-gradient-to-b from-[#04070d] via-[#071220] to-[#03060d] text-slate-100">
            <div className="pointer-events-none absolute inset-0 opacity-25 [background-image:radial-gradient(circle_at_15%_20%,rgba(6,182,212,0.32),transparent_30%),radial-gradient(circle_at_80%_10%,rgba(56,189,248,0.18),transparent_26%),linear-gradient(rgba(14,165,233,0.08)_1px,transparent_1px),linear-gradient(90deg,rgba(14,165,233,0.08)_1px,transparent_1px)] [background-size:100%_100%,100%_100%,42px_42px,42px_42px]" />

            <main className="relative z-10 mx-auto w-full max-w-7xl space-y-14 px-4 py-10 sm:px-6 lg:px-8 lg:py-14">
                <section className="relative overflow-hidden rounded-3xl border border-cyan-400/20 bg-slate-900/45 p-8 shadow-[0_0_40px_rgba(6,182,212,0.15)] backdrop-blur-xl sm:p-10">
                    <div className="absolute -top-8 right-8 text-3xl text-cyan-300/60 animate-pulse">🔐</div>
                    <div className="absolute bottom-8 left-8 text-2xl text-cyan-300/40 [animation:floatLock_6s_ease-in-out_infinite]">🛡️</div>
                    <div className="absolute right-[18%] top-[58%] text-xl text-cyan-200/50 [animation:floatLock_7s_ease-in-out_infinite]">🔑</div>

                    <p className="text-xs uppercase tracking-[0.3em] text-cyan-300">Zero-Trust Identity Infrastructure</p>
                    <h1 className="mt-4 max-w-4xl text-4xl font-semibold leading-tight text-white sm:text-5xl lg:text-6xl">
                        Secure Identity &amp; Authentication Platform
                    </h1>
                    <p className="mt-6 max-w-3xl text-base text-slate-300 sm:text-lg">
                        A hardened authentication architecture for modern distributed systems.
                    </p>

                    <div className="mt-8 flex flex-wrap gap-3">
                        <button className="rounded-lg border border-cyan-300/60 bg-cyan-400/20 px-5 py-2.5 text-sm font-medium text-cyan-100 shadow-[0_0_22px_rgba(34,211,238,0.22)] transition hover:bg-cyan-300/25">View Architecture</button>
                        <button className="rounded-lg border border-cyan-500/50 bg-slate-950/55 px-5 py-2.5 text-sm font-medium text-cyan-100 transition hover:border-cyan-300">Launch Demo</button>
                        <button className="rounded-lg border border-slate-500/70 bg-slate-900/70 px-5 py-2.5 text-sm font-medium text-slate-200 transition hover:border-cyan-300/80">GitHub Repository</button>
                    </div>

                    <div className="mt-10 grid gap-3 rounded-2xl border border-cyan-400/20 bg-black/25 p-4 sm:grid-cols-6">
                        {tokenFlow.map((step, index) => (
                            <div key={step} className="relative rounded-lg border border-cyan-400/20 bg-cyan-950/20 px-3 py-2 text-center text-xs text-cyan-100">
                                {step}
                                <span className="absolute -top-1.5 left-2 h-2 w-2 rounded-full bg-cyan-300 shadow-[0_0_12px_rgba(34,211,238,0.9)] [animation:tokenPulse_2.2s_linear_infinite]" style={{ animationDelay: `${index * 0.3}s` }} />
                            </div>
                        ))}
                    </div>
                </section>

                <section>
                    <h2 className="text-2xl font-semibold text-white sm:text-3xl">Security Problems (Legacy Auth Issues)</h2>
                    <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {legacyIssues.map((issue) => (
                            <article key={issue} className="rounded-2xl border border-red-500/45 bg-slate-900/55 p-5 shadow-[0_0_20px_rgba(239,68,68,0.12)] backdrop-blur">
                                <div className="mb-2 text-sm text-red-300">⚠ Warning</div>
                                <h3 className="text-lg font-medium text-red-100">{issue}</h3>
                            </article>
                        ))}
                    </div>
                </section>

                <section className="grid gap-6 lg:grid-cols-2">
                    <article className="rounded-3xl border border-cyan-400/25 bg-slate-900/45 p-6 backdrop-blur-xl">
                        <h2 className="text-2xl font-semibold text-white">Modern Security Architecture</h2>
                        <p className="mt-2 text-sm text-slate-300">A layered architecture with cryptographic trust and continuous session governance.</p>
                        <div className="mt-5 grid gap-3 sm:grid-cols-2">
                            {['JWT access tokens', 'Refresh token rotation', 'RSA signing', 'Token revocation', 'Audit logging', 'Session telemetry'].map((item, idx) => (
                                <div key={item} className="relative rounded-xl border border-cyan-300/25 bg-slate-950/45 p-3 text-sm text-cyan-100">
                                    {item}
                                    {idx % 2 === 0 && <span className="absolute -right-3 top-1/2 hidden text-cyan-300 sm:block">→</span>}
                                </div>
                            ))}
                        </div>
                    </article>

                    <article className="rounded-3xl border border-cyan-400/25 bg-slate-900/45 p-6 backdrop-blur-xl">
                        <h2 className="text-2xl font-semibold text-white">Authentication Lifecycle Visualization</h2>
                        <div className="mt-5 flex flex-wrap items-center gap-2">
                            {authLifecycle.map((item, index) => (
                                <div key={item} className="flex items-center gap-2">
                                    <span className="rounded-lg border border-cyan-400/30 bg-cyan-950/25 px-3 py-2 text-xs text-cyan-100 shadow-[0_0_16px_rgba(34,211,238,0.18)] [animation:tokenPulse_3.5s_ease-in-out_infinite]" style={{ animationDelay: `${index * 0.4}s` }}>
                                        {item}
                                    </span>
                                    {index < authLifecycle.length - 1 && <span className="text-cyan-300">→</span>}
                                </div>
                            ))}
                        </div>
                    </article>
                </section>

                <section className="rounded-3xl border border-cyan-400/25 bg-slate-900/45 p-6 backdrop-blur-xl">
                    <h2 className="text-2xl font-semibold text-white">Architecture Section</h2>
                    <div className="mt-6 grid gap-3">
                        {architectureChain.map((node, index) => (
                            <div key={node} className="flex flex-col items-center">
                                <div className="w-full max-w-md rounded-xl border border-cyan-400/30 bg-slate-950/45 px-4 py-3 text-center text-cyan-100">{node}</div>
                                {index < architectureChain.length - 1 && <div className="my-1 text-2xl text-cyan-300">↓</div>}
                            </div>
                        ))}
                    </div>
                </section>

                <section>
                    <h2 className="text-2xl font-semibold text-white sm:text-3xl">Security Features</h2>
                    <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {securityFeatures.map((feature) => (
                            <article key={feature} className="rounded-2xl border border-cyan-400/25 bg-slate-900/45 p-5 backdrop-blur-xl transition hover:-translate-y-0.5 hover:border-cyan-300/55">
                                <h3 className="text-lg font-medium text-cyan-100">{feature}</h3>
                            </article>
                        ))}
                    </div>
                </section>

                <section className="rounded-3xl border border-cyan-400/30 bg-slate-900/45 p-8 text-center backdrop-blur-xl">
                    <h2 className="text-2xl font-semibold text-white">Demo Section</h2>
                    <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
                        {['Login Demo', 'Security Dashboard', 'Token Inspector'].map((cta) => (
                            <button key={cta} className="rounded-lg border border-cyan-300/50 bg-cyan-500/15 px-5 py-2.5 text-sm font-medium text-cyan-100 transition hover:bg-cyan-400/25">
                                {cta}
                            </button>
                        ))}
                    </div>
                </section>
            </main>

            <footer className="relative z-10 border-t border-cyan-900/50 bg-black/30 px-4 py-8 text-center text-sm text-slate-300">
                <p>GitHub: github.com/dragon-of-north • Tech Stack: Java, Spring Boot, JWT, RSA, PostgreSQL</p>
                <p className="mt-2 text-slate-400">Author: Dragon of North</p>
            </footer>

            <style>{`
                @keyframes tokenPulse {
                    0%, 100% { opacity: 0.45; transform: translateX(0); }
                    50% { opacity: 1; transform: translateX(6px); }
                }
                @keyframes floatLock {
                    0%, 100% { transform: translateY(0px); }
                    50% { transform: translateY(-10px); }
                }
            `}</style>
        </div>
    );
};

export default LandingPage;
