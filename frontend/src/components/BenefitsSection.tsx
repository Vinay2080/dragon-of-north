import {Reveal, RevealList, RevealItem} from './Reveal';

/**
 * Example: Alternating Featured Card Pattern
 * This demonstrates the second pattern (featured card on RIGHT, supporting on LEFT)
 * Use this as a template for future sections to maintain visual variety
 */

const BenefitsSection = () => {
    // Example benefits data
    const benefits = [
        {
            title: 'Multi-Device Control',
            description: 'Manage sessions across all user devices seamlessly.',
        },
        {
            title: 'Refresh Token Rotation',
            description: 'Automatic token rotation on every refresh.',
        },
        {
            title: 'Session Visibility',
            description: 'Real-time insights into active sessions.',
            featured: true, // This one is featured
        },
    ];

    const featuredBenefit = benefits.find(b => b.featured);
    const otherBenefits = benefits.filter(b => !b.featured);

    return (
        <section className="relative mx-auto mt-14 w-full max-w-7xl px-2 sm:px-0">
            <Reveal className="mb-12 text-center">
                <h2 className="text-2xl font-semibold text-white sm:text-3xl">Advanced Features</h2>
                <p className="mt-2 text-sm text-slate-300 sm:text-base">Everything you need for enterprise security.</p>
            </Reveal>

            {/* Alternating Pattern: Supporting cards LEFT (40%), Featured card RIGHT (60%) */}
            <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
                {/* Supporting Cards Grid - Takes up 40% on desktop (left side, stacked) */}
                <div className="lg:col-span-2 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-1 gap-4">
                    {otherBenefits.map((benefit) => (
                        <RevealItem
                            key={benefit.title}
                            className="rounded-2xl border border-cyan-300/20 bg-white/[0.05] p-5 shadow-[0_0_0_1px_rgba(34,211,238,0.08)] backdrop-blur-xl transform transition-all duration-300 ease-out hover:-translate-y-1 hover:shadow-md hover:border-cyan-300/40 hover:bg-gradient-to-br hover:from-indigo-600/10 hover:to-purple-600/5"
                        >
                            <div
                                className="mb-4 h-1.5 w-12 rounded-full bg-gradient-to-r from-cyan-300/80 to-indigo-300/80"/>
                            <h3 className="text-base font-semibold text-slate-100">{benefit.title}</h3>
                            <p className="mt-2 text-sm leading-relaxed text-slate-300">{benefit.description}</p>
                        </RevealItem>
                    ))}
                </div>

                {/* Featured Card - Takes up 60% on desktop (right side), full width on mobile */}
                {featuredBenefit && (
                    <Reveal
                        key={featuredBenefit.title}
                        className="lg:col-span-3 featured-card rounded-2xl border border-cyan-300/30 bg-gradient-to-br from-indigo-600/40 via-purple-600/30 to-indigo-600/20 p-8 shadow-[0_0_0_1px_rgba(34,211,238,0.15)] backdrop-blur-xl transform transition-all duration-300 ease-out hover:-translate-y-3 hover:shadow-xl hover:from-indigo-600/50 hover:to-indigo-600/30"
                    >
                        <div className="mb-6 h-2 w-24 rounded-full bg-gradient-to-r from-cyan-300/90 to-indigo-300/90"/>
                        <h3 className="text-3xl font-bold text-white leading-tight">{featuredBenefit.title}</h3>
                        <p className="mt-4 text-lg leading-relaxed text-slate-100">{featuredBenefit.description}</p>
                        <div className="mt-8 flex gap-3">
                            <div
                                className="inline-flex h-12 w-12 items-center justify-center rounded-lg bg-cyan-400/20 backdrop-blur">
                                <span className="text-2xl">✓</span>
                            </div>
                            <div
                                className="inline-flex h-12 w-12 items-center justify-center rounded-lg bg-indigo-400/20 backdrop-blur">
                                <span className="text-2xl">⭐</span>
                            </div>
                        </div>
                    </Reveal>
                )}
            </div>
        </section>
    );
};

export default BenefitsSection;

