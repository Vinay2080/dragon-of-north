import { Reveal, RevealList, RevealItem } from './Reveal';

const features = [
    {
        title: 'Session Tracking',
        description: 'Monitor every device logged into your account.',
    },
    {
        title: 'Secure Tokens',
        description: 'JWT authentication with refresh token rotation.',
    },
    {
        title: 'Instant Revocation',
        description: 'Terminate suspicious sessions instantly.',
    },
];

const FeaturesSection = () => {
    return (
        <section className="relative mx-auto mt-14 w-full max-w-7xl px-2 sm:px-0">
            <Reveal className="mb-8 text-center">
                <h2 className="text-2xl font-semibold text-white sm:text-3xl">Core Security Features</h2>
                <p className="mt-2 text-sm text-slate-300 sm:text-base">Purpose-built controls for session-first account security.</p>
            </Reveal>

            <RevealList className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                {features.map((feature) => (
                    <RevealItem
                        key={feature.title}
                        className="rounded-2xl border border-cyan-300/20 bg-white/[0.05] p-5 shadow-[0_0_0_1px_rgba(34,211,238,0.08)] backdrop-blur-xl transform transition-all duration-300 ease-out hover:-translate-y-1 hover:shadow-lg hover:bg-gradient-to-r hover:from-primary/10 hover:to-primary/5"
                    >
                        <div className="mb-4 h-1.5 w-14 rounded-full bg-gradient-to-r from-cyan-300/80 to-indigo-300/80" />
                        <h3 className="text-lg font-semibold text-slate-100">{feature.title}</h3>
                        <p className="mt-2 text-sm leading-relaxed text-slate-300">{feature.description}</p>
                    </RevealItem>
                ))}
            </RevealList>
        </section>
    );
};

export default FeaturesSection;
