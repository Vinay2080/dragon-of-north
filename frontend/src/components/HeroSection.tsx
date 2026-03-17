import React from 'react';

const HeroSection: React.FC = () => {
    const steps = ['User Login', 'JWT', 'Expiry', 'Refresh', 'Session'];

    return (
        <section className="flex flex-col items-center justify-center text-center py-24 px-6">
            <div className="mx-auto max-w-5xl w-full">
                {/* Headline */}
                <h1 className="text-5xl md:text-6xl font-bold tracking-tight">
                    Control Sessions. Not Just Logins.
                </h1>

                {/* Subtext */}
                <p className="mt-4 text-muted-foreground max-w-xl mx-auto">
                    Short-lived tokens, rotation, and full session visibility — built for modern systems.
                </p>

                {/* CTA Buttons */}
                <div className="flex gap-4 mt-8 justify-center">
                    <button
                        type="button"
                        className="bg-primary text-primary-foreground px-6 py-3 rounded-md"
                    >
                        Explore Sessions
                    </button>

                    <button
                        type="button"
                        className="border border-border px-6 py-3 rounded-md"
                    >
                        View Architecture
                    </button>
                </div>

                {/* Flow Visual */}
                <div className="flex flex-wrap justify-center gap-3 mt-12">
                    {steps.map((s) => (
                        <div
                            key={s}
                            className="px-4 py-2 rounded-full border border-border bg-card text-sm"
                        >
                            {s}
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
};

export default HeroSection;
