import {motion} from 'framer-motion';

const CallToAction = () => {
    return (
        <section className="mx-auto mt-14 w-full max-w-5xl px-2 sm:px-0">
            <motion.div
                initial={{opacity: 0, y: 20}}
                whileInView={{opacity: 1, y: 0}}
                viewport={{once: true, amount: 0.2}}
                transition={{duration: 0.55, ease: 'easeOut'}}
                className="relative overflow-hidden rounded-3xl border border-cyan-300/20 bg-slate-900/45 px-6 py-10 text-center backdrop-blur-xl sm:px-10"
            >
                <div className="pointer-events-none absolute -left-16 top-1/2 h-36 w-36 -translate-y-1/2 rounded-full bg-cyan-400/15 blur-3xl" />
                <div className="pointer-events-none absolute -right-14 top-6 h-32 w-32 rounded-full bg-indigo-400/15 blur-3xl" />

                <h3 className="text-2xl font-semibold text-white sm:text-3xl">Ready to secure your sessions?</h3>

                <div className="mt-6 flex flex-col items-center justify-center gap-3 sm:flex-row">
                    <motion.button
                        whileHover={{y: -2, scale: 1.03}}
                        whileTap={{scale: 0.98}}
                        className="w-full rounded-lg bg-cyan-500 px-6 py-3 text-sm font-semibold text-white shadow-[0_0_30px_rgba(34,211,238,0.35)] transition hover:bg-cyan-400 sm:w-auto"
                    >
                        Sign Up
                    </motion.button>
                    <motion.button
                        whileHover={{y: -2, scale: 1.03}}
                        whileTap={{scale: 0.98}}
                        className="w-full rounded-lg border border-slate-500/60 bg-slate-900/40 px-6 py-3 text-sm font-semibold text-slate-100 shadow-[0_0_22px_rgba(129,140,248,0.25)] transition hover:border-cyan-300/70 hover:text-cyan-100 sm:w-auto"
                    >
                        Login
                    </motion.button>
                </div>
            </motion.div>
        </section>
    );
};

export default CallToAction;
