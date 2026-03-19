import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { Reveal } from './Reveal';

const CallToAction = () => {
    const navigate = useNavigate();

    return (
        <section className="mx-auto mt-14 w-full max-w-5xl px-2 sm:px-0">
            <Reveal className="relative overflow-hidden rounded-3xl border border-cyan-300/20 bg-slate-900/45 px-6 py-10 text-center backdrop-blur-xl sm:px-10">
                <div className="pointer-events-none absolute -left-16 top-1/2 h-36 w-36 -translate-y-1/2 rounded-full bg-cyan-400/15 blur-3xl" />
                <div className="pointer-events-none absolute -right-14 top-6 h-32 w-32 rounded-full bg-indigo-400/15 blur-3xl" />

                <h3 className="text-2xl font-semibold text-white sm:text-3xl">Ready to secure your sessions?</h3>

                <div className="mt-6 flex items-center justify-center">
                    <motion.button
                        whileHover={{ y: -2, scale: 1.03 }}
                        whileTap={{ scale: 0.98 }}
                        onClick={() => navigate('/login')}
                        className="w-full rounded-lg bg-gradient-to-r from-cyan-500 to-violet-500 px-6 py-3 text-sm font-semibold text-white shadow-[0_0_36px_rgba(96,165,250,0.35)] transition hover:from-cyan-400 hover:to-violet-400 sm:w-auto"
                    >
                        Continue to Login / Sign Up
                    </motion.button>
                </div>
            </Reveal>
        </section>
    );
};

export default CallToAction;
