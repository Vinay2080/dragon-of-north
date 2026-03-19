import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { Reveal } from './Reveal';
import {Button} from './Button';

const CallToAction = () => {
    const navigate = useNavigate();

    return (
        <section className="mx-auto mt-20 w-full max-w-5xl px-2 sm:px-0">
            {/* Dark Contrast Section - Featured Focal Point */}
            <div
                className="dark-contrast-section relative rounded-3xl bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 px-6 py-16 sm:px-12 sm:py-20 overflow-hidden border border-cyan-300/10">
                {/* Gradient overlays for premium feel */}
                <div
                    className="pointer-events-none absolute -left-20 top-1/4 h-40 w-40 -translate-y-1/2 rounded-full bg-indigo-500/15 blur-3xl"/>
                <div
                    className="pointer-events-none absolute -right-16 bottom-0 h-44 w-44 rounded-full bg-cyan-400/10 blur-3xl"/>

                {/* Content Grid: Icon + Text LEFT, CTA RIGHT */}
                <div className="relative z-10 grid grid-cols-1 md:grid-cols-2 gap-8 items-center">
                    {/* Left: Icon + Heading + Description */}
                    <div className="text-center md:text-left">
                        <Reveal
                            className="inline-flex md:inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-600/40 to-cyan-500/30 backdrop-blur mb-6">
                            <span className="text-3xl">🔐</span>
                        </Reveal>

                        <Reveal className="block">
                            <h2 className="text-3xl md:text-4xl font-bold text-white leading-tight">
                                Control Every Session
                            </h2>
                        </Reveal>

                        <Reveal className="mt-4 block">
                            <p className="text-lg text-slate-300 max-w-md">
                                Real-time visibility, instant revocation, and enterprise-grade session management—all in
                                one platform.
                            </p>
                        </Reveal>

                        {/* Feature Pills */}
                        <Reveal className="mt-6 flex flex-wrap gap-2 justify-center md:justify-start">
                            {['⚡ Real-Time', '🔒 Secure', '📱 Multi-Device'].map((pill) => (
                                <div key={pill}
                                     className="inline-flex items-center gap-2 rounded-full bg-white/5 border border-white/10 px-3 py-1.5 backdrop-blur">
                                    <span className="text-xs font-medium text-slate-200">{pill}</span>
                                </div>
                            ))}
                        </Reveal>
                    </div>

                    {/* Right: CTA Button - FEATURED FOCAL POINT */}
                    <div className="flex items-center justify-center md:justify-end">
                        <Reveal>
                            <motion.div
                                whileHover={{y: -4, scale: 1.08}}
                                whileTap={{scale: 0.96}}
                                className="w-full sm:w-auto"
                            >
                                <Button
                                    variant="primary"
                                    onClick={() => navigate('/login')}
                                    className="w-full sm:w-auto px-10 py-5 text-lg font-bold shadow-lg hover:shadow-xl transition-all duration-300"
                                >
                                    Start Exploring
                                </Button>
                            </motion.div>
                        </Reveal>
                    </div>
                </div>
            </div>

            {/* Secondary CTA - Light variant for contrast and information */}
            <Reveal
                className="mt-8 relative overflow-hidden rounded-2xl border border-cyan-300/15 bg-white/[0.03] px-6 py-8 sm:px-10 sm:py-10 backdrop-blur-xl text-center">
                <div
                    className="pointer-events-none absolute -left-12 top-1/2 h-32 w-32 -translate-y-1/2 rounded-full bg-cyan-400/8 blur-2xl"/>
                <div
                    className="pointer-events-none absolute -right-12 top-6 h-28 w-28 rounded-full bg-indigo-400/8 blur-2xl"/>

                <div className="relative z-10">
                    <h3 className="text-xl font-semibold text-slate-100">Learn about our architecture</h3>
                    <p className="mt-2 text-sm text-slate-400">Explore comprehensive documentation and security
                        features.</p>
                </div>
            </Reveal>
        </section>
    );
};

export default CallToAction;
