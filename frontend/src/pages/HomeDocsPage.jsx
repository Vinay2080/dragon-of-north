import {Link} from 'react-router-dom';

const featureCards = [
    'Short-lived tokens',
    'Refresh token rotation',
    'Session visibility',
    'Device tracking',
    'Revocation system',
    'Secure by design',
];

const flowSteps = ['User Login', 'JWT', 'Expiry', 'Refresh', 'Session'];

const howItWorks = [
    'Login',
    'Access token issued',
    'Token expires quickly',
    'Refresh token rotates',
    'Session tracked',
];

const risks = [
    'Long-lived access tokens increase blast radius when compromised.',
    'No session inventory means users cannot inspect active devices.',
    'Token revocation gaps keep compromised sessions alive too long.',
    'Weak refresh practices lead to replay and session abuse.',
];

const improvements = [
    'Short-lived access tokens minimize credential exposure windows.',
    'Rotating refresh tokens reduce replay risk on every renewal.',
    'Session-level visibility gives users and teams full control.',
    'Revocation is immediate, auditable, and device-aware.',
];

const HomeDocsPage = () => {
    return (
        <div className="py-20">
            <div className="max-w-6xl mx-auto px-6 space-y-12">
                <section className="text-center py-24">
                    <h1 className="text-4xl md:text-6xl font-bold tracking-tight">
                        Control Sessions. Not Just Logins.
                    </h1>
                    <p className="mt-4 text-muted-foreground max-w-2xl mx-auto">
                        Short-lived tokens, rotation, revocation, and full session visibility — built for modern
                        systems.
                    </p>

                    <div className="mt-8 flex flex-wrap justify-center gap-4">
                        <Link
                            to="/sessions"
                            className="bg-primary text-primary-foreground px-6 py-3 rounded-md"
                        >
                            Explore Sessions
                        </Link>
                        <Link
                            to="/architecture"
                            className="border border-border px-6 py-3 rounded-md"
                        >
                            View Architecture
                        </Link>
                    </div>

                    <div className="mt-8 flex flex-wrap justify-center gap-3">
                        {flowSteps.map((step) => (
                            <div
                                key={step}
                                className="px-4 py-2 rounded-full border border-border bg-card text-sm"
                            >
                                {step}
                            </div>
                        ))}
                    </div>
                </section>

                <section className="py-16 border-t border-border border-opacity-50">
                    <h2 className="text-3xl font-semibold text-center">Core capabilities</h2>
                    <div className="mt-8 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                        {featureCards.map((card) => (
                            <article
                                key={card}
                                className="relative bg-card border border-border rounded-lg p-6 transition-all duration-200 hover:shadow-md hover:-translate-y-1"
                            >
                                <div
                                    className="absolute top-4 left-4 w-9 h-9 rounded-md bg-muted/10 flex items-center justify-center text-sm text-muted-foreground">
                                    {/* icon placeholder */}
                                    <span className="opacity-70">•</span>
                                </div>

                                <h3 className="font-medium pl-12">{card}</h3>
                            </article>
                        ))}
                    </div>
                </section>

                <section className="py-16 border-t border-border border-opacity-50">
                    <h2 className="text-3xl font-semibold text-center">How it works</h2>
                    <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
                        {howItWorks.map((step, index) => (
                            <article
                                key={step}
                                className="bg-card border border-border rounded-lg p-4 text-center"
                            >
                                <p className="text-xs text-muted-foreground">Step {index + 1}</p>
                                <p className="mt-2 text-sm font-medium">{step}</p>
                            </article>
                        ))}
                    </div>
                </section>

                <section className="py-16 border-t border-border border-opacity-50">
                    <h2 className="text-3xl font-semibold text-center">Why traditional auth fails</h2>
                    <div className="mt-8 grid gap-6 lg:grid-cols-2">
                        <article className="bg-card border border-border rounded-lg p-6">
                            <h3 className="text-lg font-semibold">Common risks</h3>
                            <ul className="mt-4 space-y-3 text-sm text-muted-foreground">
                                {risks.map((risk) => (
                                    <li key={risk}>• {risk}</li>
                                ))}
                            </ul>
                        </article>

                        <article className="bg-card border border-border rounded-lg p-6">
                            <h3 className="text-lg font-semibold">Improved solution</h3>
                            <ul className="mt-4 space-y-3 text-sm text-muted-foreground">
                                {improvements.map((item) => (
                                    <li key={item}>• {item}</li>
                                ))}
                            </ul>
                        </article>
                    </div>
                </section>

                <section className="py-16 text-center border-t border-border border-opacity-50">
                    <h2 className="text-3xl md:text-4xl font-semibold">
                        Start building secure systems
                    </h2>
                    <div className="mt-6">
                        <Link
                            to="/signup"
                            className="bg-primary text-primary-foreground px-6 py-3 rounded-md inline-flex"
                        >
                            Get Started
                        </Link>
                    </div>
                </section>
            </div>
        </div>
    );
};

export default HomeDocsPage;
