import { Reveal, RevealList, RevealItem } from './Reveal';

const HeroSection = () => {
    const steps = ['User Login', 'JWT', 'Expiry', 'Refresh', 'Session'];

    return (
        <section className="flex flex-col items-center justify-center text-center py-24 px-6">
            <div className="mx-auto max-w-5xl w-full">
                {/* Headline */}
                <Reveal>
                    <h1 className="text-5xl md:text-6xl font-bold tracking-tight">
                        Control Sessions. Not Just Logins.
                    </h1>
                </Reveal>

                {/* Subtext */}
                <Reveal delay={0.1}>
                    <p className="mt-4 text-muted-foreground max-w-xl mx-auto">
                        Short-lived tokens, rotation, and full session visibility — built for modern systems.
                    </p>
                </Reveal>

                {/* CTA Buttons */}
                <Reveal delay={0.2}>
                    <div className="flex gap-4 mt-8 justify-center">
                        <button
                            type="button"
                            className="bg-primary text-primary-foreground px-6 py-3 rounded-md btn interactive-card"
                        >
                            Explore Sessions
                        </button>

                        <button
                            type="button"
                            className="border border-border px-6 py-3 rounded-md btn interactive-card"
                        >
                            View Architecture
                        </button>
                    </div>
                </Reveal>

                {/* Flow Visual - Staggered List */}
                <RevealList className="flex flex-wrap justify-center gap-3 mt-12">
                    {steps.map((s) => (
                        <RevealItem
                            key={s}
                            className="px-4 py-2 rounded-full border border-border bg-card text-sm"
                        >
                            {s}
                        </RevealItem>
                    ))}
                </RevealList>
            </div>
        </section>
    );
};

export default HeroSection;
