import {useEffect, useState} from 'react';
import {motion, Variants} from 'framer-motion';

const TYPE_TARGET = 'Control Sessions.';
const TYPE_INTERVAL_MS = 52;

const SEQUENCE = {
    headlineLeadDuration: 0.4,
    subtextDuration: 0.5,
    buttonDuration: 0.45,
    metaDuration: 0.4,
    subtextGapAfterHeadline: 0.2,
    secondButtonGap: 0.12,
    metaGap: 0.08,
    afterButtonsGap: 0.18,
};

const easeOut = 'easeOut' as const;

const useTypewriter = (text: string, intervalMs: number) => {
    const [typed, setTyped] = useState('');

    useEffect(() => {
        let index = 0;
        const timer = window.setInterval(() => {
            index += 1;
            setTyped(text.slice(0, index));

            if (index >= text.length) {
                window.clearInterval(timer);
            }
        }, intervalMs);

        return () => window.clearInterval(timer);
    }, [text, intervalMs]);

    return typed;
};

const createEnterVariants = (delay: number): Variants => ({
    hidden: {opacity: 0, y: 20},
    show: {
        opacity: 1,
        y: 0,
        transition: {
            delay,
            duration: SEQUENCE.subtextDuration,
            ease: easeOut,
        },
    },
});

const createButtonVariants = (delay: number): Variants => ({
    hidden: {opacity: 0, y: 20, scale: 0.95},
    show: {
        opacity: 1,
        y: 0,
        scale: 1,
        transition: {
            delay,
            duration: SEQUENCE.buttonDuration,
            ease: easeOut,
        },
    },
});

const HeroSection = () => {
    const meta = ['JWT', 'Spring', 'Tracking'];
    const typedHeadline = useTypewriter(TYPE_TARGET, TYPE_INTERVAL_MS);

    const headlineTailDelay = (TYPE_TARGET.length * TYPE_INTERVAL_MS) / 1000;
    const subtextDelay = headlineTailDelay + SEQUENCE.headlineLeadDuration + SEQUENCE.subtextGapAfterHeadline;
    const firstButtonDelay = subtextDelay + SEQUENCE.subtextDuration;
    const secondButtonDelay = firstButtonDelay + SEQUENCE.secondButtonGap;
    const metaStartDelay = secondButtonDelay + SEQUENCE.buttonDuration + SEQUENCE.afterButtonsGap;

    return (
        <section className="flex flex-col items-center justify-center text-center py-24 px-6">
            <div className="mx-auto max-w-5xl w-full">
                {/* Headline */}
                <div>
                    <h1 className="text-5xl md:text-6xl font-bold tracking-tight">
                        {typedHeadline}
                    </h1>
                    <motion.p
                        initial={{opacity: 0, y: 20}}
                        animate={{opacity: 1, y: 0}}
                        transition={{
                            delay: headlineTailDelay,
                            duration: SEQUENCE.headlineLeadDuration,
                            ease: easeOut,
                        }}
                        className="text-5xl md:text-6xl font-bold tracking-tight min-h-[1.2em]"
                    >
                        Not Just Logins.
                    </motion.p>
                </div>

                {/* Subtext */}
                <motion.p
                    initial="hidden"
                    animate="show"
                    variants={createEnterVariants(subtextDelay)}
                    className="mt-4 text-muted-foreground max-w-xl mx-auto"
                >
                    Short-lived tokens, rotation, and full session visibility — built for modern systems.
                </motion.p>

                {/* CTA Buttons */}
                <div className="flex gap-4 mt-8 justify-center">
                    <motion.button
                        type="button"
                        initial="hidden"
                        animate="show"
                        variants={createButtonVariants(firstButtonDelay)}
                        className="bg-primary text-primary-foreground px-6 py-3 rounded-md btn interactive-card"
                    >
                        Explore Sessions
                    </motion.button>

                    <motion.button
                        type="button"
                        initial="hidden"
                        animate="show"
                        variants={createButtonVariants(secondButtonDelay)}
                        className="border border-border px-6 py-3 rounded-md btn interactive-card"
                    >
                        View Flow
                    </motion.button>
                </div>

                {/* Bottom Meta Row */}
                <div className="flex flex-wrap justify-center gap-3 mt-12">
                    {meta.map((item, index) => (
                        <motion.span
                            key={item}
                            initial={{opacity: 0, y: 20}}
                            animate={{opacity: 1, y: 0}}
                            transition={{
                                delay: metaStartDelay + index * SEQUENCE.metaGap,
                                duration: SEQUENCE.metaDuration,
                                ease: easeOut,
                            }}
                            className="px-4 py-2 rounded-full border border-border bg-card text-sm"
                        >
                            {item}
                        </motion.span>
                    ))}
                </div>
            </div>
        </section>
    );
};

export default HeroSection;
