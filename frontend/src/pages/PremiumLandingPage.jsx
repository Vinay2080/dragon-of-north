import {useMemo, useState} from 'react';
import {Link} from 'react-router-dom';
// eslint-disable-next-line no-unused-vars
import {motion} from 'framer-motion';
import {AlertCircle, CheckCircle, ChevronRight, Eye, Lock, Shield, Smartphone, Zap} from 'react-feather';

const containerVariants = {
    hidden: {opacity: 0},
    visible: {
        opacity: 1,
        transition: {staggerChildren: 0.08, delayChildren: 0.12},
    },
};

const itemVariants = {
    hidden: {opacity: 0, y: 14},
    visible: {opacity: 1, y: 0, transition: {duration: 0.35}},
};

const slideInLeft = {
    hidden: {opacity: 0, x: -30},
    visible: {opacity: 1, x: 0, transition: {duration: 0.4}},
};

const slideInRight = {
    hidden: {opacity: 0, x: 30},
    visible: {opacity: 1, x: 0, transition: {duration: 0.4}},
};

const STEPS = [
    {
        id: 'login',
        title: 'Secure Login',
        description: 'Policies and validation run before any token is created.',
        details: 'Credentials are verified and security checks are applied before issuing session material.',
    },
    {
        id: 'access-token',
        title: 'Access Token Issued',
        description: 'A short-lived JWT is issued for API access.',
        details: 'The token is intentionally minimal and expires quickly to reduce risk exposure.',
    },
    {
        id: 'expiry',
        title: 'Token Expires',
        description: 'Short expiry keeps leaked credentials low-impact.',
        details: 'Tokens expire in minutes so compromised access windows stay small and manageable.',
    },
    {
        id: 'refresh',
        title: 'Refresh Token Rotates',
        description: 'Every refresh invalidates the previous token.',
        details: 'Rotation blocks replay attacks by replacing refresh material on every successful exchange.',
    },
    {
        id: 'session',
        title: 'Session Tracked',
        description: 'Each device session can be observed and revoked.',
        details: 'Session metadata remains visible so teams can investigate and terminate suspicious activity fast.',
    },
];

const FEATURES = [
    {
        icon: Zap,
        title: 'Short-lived Tokens',
        description: 'Reduce attack surface with automatic expiration.',
        cta: 'Learn more',
        to: '/features',
    },
    {
        icon: Lock,
        title: 'Refresh Token Rotation',
        description: 'Every refresh creates a new token and invalidates the old one.',
        cta: 'View flow',
        to: '/identifier-flow',
    },
    {
        icon: Eye,
        title: 'Session Visibility',
        description: 'Track active sessions across every connected device.',
        cta: 'Explore sessions',
        to: '/sessions',
    },
    {
        icon: AlertCircle,
        title: 'Instant Revocation',
        description: 'Terminate compromised sessions immediately.',
        cta: 'Learn more',
        to: '/features',
    },
    {
        icon: Smartphone,
        title: 'Device Awareness',
        description: 'Manage sessions per device with clear audit context.',
        cta: 'Explore sessions',
        to: '/sessions',
    },
    {
        icon: Shield,
        title: 'Policy Control',
        description: 'Apply session and expiry controls for stricter environments.',
        cta: 'View docs',
        to: '/architecture',
    },
];

const USE_CASES = [
    'Enterprise security posture',
    'Multi-device session control',
    'Real-time suspicious session response',
    'Compliance and audit visibility',
    'Fast incident response',
    'Developer-friendly session APIs',
];

function HeroSection() {
    return (
        <section className="relative overflow-hidden py-20 md:py-28 lg:py-32 bg-white dark:bg-[#020617]">
            {/* Light mode background */}
            <div
                className="pointer-events-none absolute inset-0 dark:hidden"
                style={{
                    background: 'radial-gradient(circle at 20% 20%, rgba(139,92,246,0.08), transparent 40%), radial-gradient(circle at 80% 30%, rgba(99,102,241,0.06), transparent 40%)',
                }}
            />
            {/* Dark mode background */}
            <div
                className="pointer-events-none absolute inset-0 hidden dark:block"
                style={{
                    background: 'linear-gradient(135deg, #020617 0%, #1a1632 50%, #0f1a2e 100%)',
                }}
            />
            <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 text-center">
                <motion.div variants={containerVariants} initial="hidden" animate="visible" className="space-y-8">
                    <motion.span variants={itemVariants}
                                 className="inline-flex rounded-full border border-violet-200 bg-violet-50 px-4 py-2 text-sm font-medium text-violet-700 dark:border-violet-500/30 dark:bg-violet-500/15 dark:text-violet-300">
                        Authentication Platform
                    </motion.span>
                    <motion.h1 variants={itemVariants}
                               className="text-4xl md:text-5xl lg:text-6xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
                        Control Sessions.<br/>Not Just Logins.
                    </motion.h1>
                    <motion.p variants={itemVariants}
                              className="mx-auto max-w-3xl text-base md:text-xl text-slate-600 dark:text-slate-300">
                        Short-lived tokens, refresh rotation, and full session visibility for modern systems.
                    </motion.p>
                    <motion.div variants={itemVariants} className="flex flex-wrap items-center justify-center gap-3">
                        <Link
                            to="/sessions"
                            className="rounded-lg bg-violet-600 px-7 py-3 font-medium text-white transition hover:bg-violet-500"
                        >
                            Explore Sessions
                        </Link>
                        <Link
                            to="/architecture"
                            className="rounded-lg border border-slate-300 px-7 py-3 font-medium text-slate-900 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-100 dark:hover:bg-slate-800"
                        >
                            View Flow <ChevronRight className="ml-1 inline h-4 w-4"/>
                        </Link>
                    </motion.div>
                </motion.div>
            </div>
        </section>
    );
}

function TrustSection() {
    return (
        <section className="bg-slate-50 py-20 md:py-28 lg:py-32 dark:bg-slate-950/40">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                <motion.div variants={containerVariants} initial="hidden" whileInView="visible" viewport={{once: true}}
                            className="mb-14 text-center">
                    <motion.h2 variants={itemVariants}
                               className="mb-4 text-3xl md:text-5xl lg:text-6xl font-bold text-slate-900 dark:text-slate-100">
                        Built for real-world security
                    </motion.h2>
                </motion.div>
                <motion.div variants={containerVariants} initial="hidden" whileInView="visible" viewport={{once: true}}
                            className="grid grid-cols-1 gap-6 md:grid-cols-3">
                    {[
                        {icon: Zap, title: 'Short-lived tokens', text: 'Short lifetimes reduce exposure windows.'},
                        {
                            icon: Lock,
                            title: 'Session-level control',
                            text: 'Track and revoke device sessions instantly.'
                        },
                        {
                            icon: AlertCircle,
                            title: 'Immediate revocation',
                            text: 'Respond to suspicious activity in milliseconds.'
                        },
                    ].map((card) => {
                        const Icon = card.icon;
                        return (
                            <motion.div key={card.title} variants={itemVariants}
                                        className="rounded-2xl border border-slate-200 bg-white p-7 dark:border-slate-800 dark:bg-slate-900">
                                <div
                                    className="mb-4 inline-flex rounded-lg bg-violet-100 p-2 text-violet-700 dark:bg-violet-500/20 dark:text-violet-300">
                                    <Icon className="h-5 w-5"/>
                                </div>
                                <h3 className="mb-2 text-xl font-semibold text-slate-900 dark:text-slate-100">{card.title}</h3>
                                <p className="text-slate-600 dark:text-slate-300">{card.text}</p>
                            </motion.div>
                        );
                    })}
                </motion.div>
            </div>
        </section>
    );
}

function HowItWorksSection({activeStep, onStepChange}) {
    const activeStepData = useMemo(() => STEPS.find((step) => step.id === activeStep) ?? STEPS[0], [activeStep]);

    return (
        <section className="py-20 md:py-28 lg:py-32">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                <motion.div variants={containerVariants} initial="hidden" whileInView="visible" viewport={{once: true}}
                            className="mb-12">
                    <motion.h2 variants={itemVariants}
                               className="mb-3 text-3xl md:text-5xl lg:text-6xl font-bold text-slate-900 dark:text-slate-100">
                        How It Works
                    </motion.h2>
                    <motion.p variants={itemVariants} className="text-slate-600 dark:text-slate-300">
                        Click each step to inspect the flow.
                    </motion.p>
                </motion.div>

                <motion.div variants={containerVariants} initial="hidden" whileInView="visible" viewport={{once: true}}
                            className="mb-8 grid grid-cols-1 gap-3 md:grid-cols-5">
                    {STEPS.map((step, index) => (
                        <motion.button
                            key={step.id}
                            type="button"
                            variants={itemVariants}
                            onClick={() => onStepChange(step.id)}
                            className={`rounded-xl border p-4 text-left transition ${activeStep === step.id
                                ? 'border-violet-400 bg-violet-50 dark:border-violet-500/40 dark:bg-violet-500/10'
                                : 'border-slate-200 bg-white hover:border-slate-300 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-slate-700'}`}
                        >
                            <p className="text-xs font-medium text-slate-500 dark:text-slate-400">Step {index + 1}</p>
                            <p className="mt-2 text-sm font-semibold text-slate-900 dark:text-slate-100">{step.title}</p>
                            <p className="mt-2 text-xs text-slate-600 dark:text-slate-300">{step.description}</p>
                        </motion.button>
                    ))}
                </motion.div>

                <motion.div key={activeStep} initial={{opacity: 0, y: 8}} animate={{opacity: 1, y: 0}}
                            transition={{duration: 0.2}}
                            className="rounded-2xl border border-violet-200 bg-gradient-to-br from-violet-50 to-transparent p-7 dark:border-violet-500/30 dark:from-violet-500/10">
                    <h3 className="mb-3 text-2xl font-bold text-slate-900 dark:text-slate-100">{activeStepData.title}</h3>
                    <p className="mb-5 text-slate-700 dark:text-slate-300">{activeStepData.details}</p>
                    <div className="flex flex-wrap gap-3">
                        <Link to="/features"
                              className="rounded-lg bg-violet-600 px-5 py-2 text-sm font-medium text-white transition hover:bg-violet-500">
                            Learn more
                        </Link>
                        <Link to="/identifier-flow"
                              className="rounded-lg border border-slate-300 px-5 py-2 text-sm font-medium text-slate-900 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-100 dark:hover:bg-slate-800">
                            View flow
                        </Link>
                        <Link to="/sessions"
                              className="rounded-lg border border-slate-300 px-5 py-2 text-sm font-medium text-slate-900 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-100 dark:hover:bg-slate-800">
                            Explore sessions
                        </Link>
                    </div>
                </motion.div>
            </div>
        </section>
    );
}

function FeatureSection() {
    return (
        <section className="bg-slate-50 py-20 md:py-28 lg:py-32 dark:bg-slate-950/40">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                <motion.h2 variants={itemVariants} initial="hidden" whileInView="visible" viewport={{once: true}}
                           className="mb-12 text-3xl md:text-5xl lg:text-6xl font-bold text-slate-900 dark:text-slate-100">
                    Core Features
                </motion.h2>
                <motion.div variants={containerVariants} initial="hidden" whileInView="visible" viewport={{once: true}}
                            className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                    {FEATURES.map((feature) => {
                        const Icon = feature.icon;
                        return (
                            <motion.div key={feature.title} variants={itemVariants} whileHover={{y: -4}}
                                        className="rounded-2xl border border-slate-200 bg-white p-7 transition hover:shadow-lg dark:border-slate-800 dark:bg-slate-900">
                                <div
                                    className="mb-4 inline-flex rounded-lg bg-violet-100 p-2 text-violet-700 dark:bg-violet-500/20 dark:text-violet-300">
                                    <Icon className="h-5 w-5"/>
                                </div>
                                <h3 className="mb-2 text-xl font-semibold text-slate-900 dark:text-slate-100">{feature.title}</h3>
                                <p className="mb-4 text-slate-600 dark:text-slate-300">{feature.description}</p>
                                <Link to={feature.to}
                                      className="inline-flex items-center text-sm font-medium text-violet-700 hover:text-violet-600 dark:text-violet-300 dark:hover:text-violet-200">
                                    {feature.cta} <ChevronRight className="ml-1 h-4 w-4"/>
                                </Link>
                            </motion.div>
                        );
                    })}
                </motion.div>
            </div>
        </section>
    );
}

function ComparisonSection() {
    return (
        <section className="py-20 md:py-28 lg:py-32">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                <h2 className="mb-12 text-center text-3xl md:text-5xl lg:text-6xl font-bold text-slate-900 dark:text-slate-100">
                    Why traditional auth fails
                </h2>
                <div className="grid grid-cols-1 gap-8 md:grid-cols-2 md:gap-12">
                    <motion.div variants={slideInLeft} initial="hidden" whileInView="visible" viewport={{once: true}}
                                className="rounded-2xl border border-red-200 bg-red-50/60 p-7 dark:border-red-500/30 dark:bg-red-500/10">
                        <h3 className="mb-4 text-2xl font-bold text-slate-900 dark:text-slate-100">Common risks</h3>
                        {['Long-lived tokens', 'No session tracking', 'Weak revocation', 'Replay exposure'].map((item) => (
                            <p key={item} className="mb-2 text-slate-700 dark:text-slate-300">- {item}</p>
                        ))}
                    </motion.div>
                    <motion.div variants={slideInRight} initial="hidden" whileInView="visible" viewport={{once: true}}
                                className="rounded-2xl border border-green-200 bg-green-50/60 p-7 dark:border-green-500/30 dark:bg-green-500/10">
                        <h3 className="mb-4 text-2xl font-bold text-slate-900 dark:text-slate-100">Improved
                            solution</h3>
                        {[
                            {text: 'Short-lived tokens'},
                            {text: 'Rotation-based refresh'},
                            {text: 'Full session control'},
                            {text: 'Instant revocation'},
                        ].map((item) => (
                            <div key={item.text}
                                 className="mb-2 flex items-center gap-2 text-slate-700 dark:text-slate-300">
                                <CheckCircle className="h-4 w-4 text-green-600 dark:text-green-300"/> {item.text}
                            </div>
                        ))}
                    </motion.div>
                </div>
            </div>
        </section>
    );
}

function UseCasesSection() {
    return (
        <section className="bg-slate-50 py-20 md:py-28 lg:py-32 dark:bg-slate-950/40">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                <h2 className="mb-10 text-3xl md:text-5xl lg:text-6xl font-bold text-slate-900 dark:text-slate-100">Use
                    Cases</h2>
                <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                    {USE_CASES.map((useCase) => (
                        <div key={useCase}
                             className="rounded-2xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900">
                            <p className="font-medium text-slate-800 dark:text-slate-200">{useCase}</p>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
}

function FinalCtaSection() {
    return (
        <section className="relative overflow-hidden py-20 md:py-28 lg:py-32">
            <div className="absolute inset-0 bg-gradient-to-b from-violet-600 to-violet-700"/>
            <div className="relative mx-auto max-w-4xl px-4 text-center sm:px-6 lg:px-8">
                <h2 className="mb-4 text-4xl md:text-5xl lg:text-6xl font-bold text-white">Start building secure
                    systems</h2>
                <p className="mx-auto mb-8 max-w-2xl text-violet-100">Session control, token rotation, and real-time
                    visibility in one flow.</p>
                <div className="flex flex-wrap justify-center gap-3">
                    <Link to="/sessions"
                          className="rounded-lg bg-white px-7 py-3 font-medium text-violet-700 transition hover:bg-slate-100">
                        Get Started
                    </Link>
                    <Link to="/features"
                          className="rounded-lg border border-white px-7 py-3 font-medium text-white transition hover:bg-white/10">
                        Explore Docs <ChevronRight className="ml-1 inline h-4 w-4"/>
                    </Link>
                </div>
            </div>
        </section>
    );
}

export default function PremiumLandingPage() {
    const [activeStep, setActiveStep] = useState('login');

    return (
        <div className="min-h-screen bg-white text-slate-900 dark:bg-[#020617] dark:text-slate-100">
            <HeroSection/>
            <TrustSection/>
            <HowItWorksSection activeStep={activeStep} onStepChange={setActiveStep}/>
            <FeatureSection/>
            <ComparisonSection/>
            <UseCasesSection/>
            <FinalCtaSection/>
        </div>
    );
}

