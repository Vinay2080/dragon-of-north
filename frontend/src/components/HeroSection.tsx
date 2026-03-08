import {motion} from 'framer-motion';

const SESSION_NODES = [
    {id: 'n1', x: 18, y: 26, size: 16, delay: 0},
    {id: 'n2', x: 46, y: 18, size: 20, delay: 0.4},
    {id: 'n3', x: 72, y: 28, size: 14, delay: 0.8},
    {id: 'n4', x: 82, y: 56, size: 18, delay: 1.1},
    {id: 'n5', x: 56, y: 72, size: 22, delay: 0.7},
    {id: 'n6', x: 30, y: 66, size: 14, delay: 1.3},
    {id: 'n7', x: 14, y: 50, size: 10, delay: 1.6},
];

const CONNECTIONS = [
    ['n1', 'n2'],
    ['n2', 'n3'],
    ['n3', 'n4'],
    ['n4', 'n5'],
    ['n5', 'n6'],
    ['n6', 'n7'],
    ['n7', 'n1'],
    ['n2', 'n5'],
    ['n3', 'n6'],
];

const nodeMap = Object.fromEntries(SESSION_NODES.map((node) => [node.id, node]));

const HeroSection = () => {
    return (
        <section className="relative isolate overflow-hidden bg-slate-950 px-6 pb-20 pt-28 text-slate-100 sm:px-10 lg:px-16 lg:pb-28 lg:pt-36">
            <motion.div
                aria-hidden
                className="pointer-events-none absolute inset-0"
                animate={{
                    background: [
                        'radial-gradient(circle at 15% 20%, rgba(56, 189, 248, 0.20), transparent 42%), radial-gradient(circle at 80% 12%, rgba(168, 85, 247, 0.15), transparent 38%), linear-gradient(130deg, #020617 0%, #0f172a 50%, #111827 100%)',
                        'radial-gradient(circle at 20% 24%, rgba(56, 189, 248, 0.26), transparent 44%), radial-gradient(circle at 78% 16%, rgba(168, 85, 247, 0.20), transparent 40%), linear-gradient(130deg, #020617 0%, #0b1220 50%, #111827 100%)',
                        'radial-gradient(circle at 15% 20%, rgba(56, 189, 248, 0.20), transparent 42%), radial-gradient(circle at 80% 12%, rgba(168, 85, 247, 0.15), transparent 38%), linear-gradient(130deg, #020617 0%, #0f172a 50%, #111827 100%)',
                    ],
                }}
                transition={{duration: 16, ease: 'easeInOut', repeat: Infinity}}
            />

            <div className="pointer-events-none absolute -top-24 left-1/2 h-[24rem] w-[24rem] -translate-x-1/2 rounded-full bg-cyan-400/10 blur-3xl" />
            <div className="pointer-events-none absolute bottom-0 right-0 h-80 w-80 rounded-full bg-fuchsia-500/10 blur-3xl" />

            <svg
                aria-hidden
                className="pointer-events-none absolute inset-0 h-full w-full opacity-30"
                viewBox="0 0 1200 600"
                preserveAspectRatio="none"
            >
                <path
                    d="M1020 490c-55-58-118-83-188-75-72 9-128 49-196 44-68-5-112-57-184-84-69-26-143-20-208 18-63 37-124 100-236 97"
                    stroke="url(#dragonGlow)"
                    strokeWidth="2.5"
                    fill="none"
                    strokeLinecap="round"
                />
                <defs>
                    <linearGradient id="dragonGlow" x1="0%" y1="0%" x2="100%" y2="0%">
                        <stop offset="0%" stopColor="#06b6d4" stopOpacity="0" />
                        <stop offset="40%" stopColor="#22d3ee" stopOpacity="0.5" />
                        <stop offset="100%" stopColor="#a78bfa" stopOpacity="0.1" />
                    </linearGradient>
                </defs>
            </svg>

            <div className="relative mx-auto grid w-full max-w-7xl items-center gap-14 lg:grid-cols-[1.05fr_0.95fr]">
                <motion.div
                    initial={{opacity: 0, y: 24}}
                    animate={{opacity: 1, y: 0}}
                    transition={{duration: 0.7, ease: 'easeOut'}}
                    className="space-y-8"
                >
                    <div className="inline-flex items-center rounded-full border border-cyan-400/25 bg-cyan-300/10 px-4 py-1 text-xs font-semibold uppercase tracking-[0.22em] text-cyan-200">
                        Session Security Platform
                    </div>

                    <h1 className="max-w-2xl text-4xl font-semibold leading-tight text-white sm:text-5xl lg:text-6xl">
                        Control Every Session. See Every Device. Trust Nothing.
                    </h1>

                    <p className="max-w-2xl text-base leading-relaxed text-slate-300 sm:text-lg">
                        Dragon of North is a session-aware authentication system that allows developers to monitor devices,
                        revoke sessions, and maintain complete control over account security.
                    </p>

                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                        <motion.a
                            href="#session-game"
                            whileHover={{y: -2, scale: 1.02}}
                            whileTap={{scale: 0.98}}
                            className="inline-flex items-center justify-center rounded-lg bg-cyan-500 px-6 py-3 text-sm font-semibold text-white shadow-[0_0_32px_rgba(34,211,238,0.25)] transition hover:bg-cyan-400"
                        >
                            Get Started
                        </motion.a>
                        <motion.a
                            href="https://dragon-api.duckdns.org/swagger-ui/index.html#/"
                            target="_blank"
                            rel="noreferrer"
                            whileHover={{y: -2, scale: 1.02}}
                            whileTap={{scale: 0.98}}
                            className="inline-flex items-center justify-center rounded-lg border border-slate-500/60 bg-slate-900/40 px-6 py-3 text-sm font-semibold text-slate-100 backdrop-blur-sm transition hover:border-cyan-300/70 hover:text-cyan-100"
                        >
                            API Documentation
                        </motion.a>
                    </div>
                </motion.div>

                <motion.div
                    initial={{opacity: 0, y: 24}}
                    animate={{opacity: 1, y: 0}}
                    transition={{duration: 0.8, delay: 0.15, ease: 'easeOut'}}
                    className="relative mx-auto w-full max-w-xl"
                >
                    <div className="relative overflow-hidden rounded-3xl border border-cyan-300/20 bg-slate-900/40 p-6 shadow-[0_30px_80px_rgba(6,182,212,0.18)] backdrop-blur-xl">
                        <div className="absolute -left-24 top-1/2 h-40 w-40 -translate-y-1/2 rounded-full bg-cyan-400/20 blur-3xl" />
                        <div className="absolute -right-20 top-12 h-32 w-32 rounded-full bg-fuchsia-400/20 blur-3xl" />

                        <div className="relative aspect-[4/3] w-full rounded-2xl border border-slate-700/70 bg-slate-950/70 p-4">
                            <svg viewBox="0 0 100 100" className="absolute inset-0 h-full w-full">
                                {CONNECTIONS.map(([from, to], index) => {
                                    const start = nodeMap[from as keyof typeof nodeMap];
                                    const end = nodeMap[to as keyof typeof nodeMap];
                                    return (
                                        <motion.line
                                            key={`${from}-${to}`}
                                            x1={start.x}
                                            y1={start.y}
                                            x2={end.x}
                                            y2={end.y}
                                            stroke="rgba(34,211,238,0.38)"
                                            strokeWidth="0.5"
                                            initial={{opacity: 0.25}}
                                            animate={{opacity: [0.25, 0.65, 0.25]}}
                                            transition={{
                                                duration: 4,
                                                delay: index * 0.15,
                                                repeat: Infinity,
                                                ease: 'easeInOut',
                                            }}
                                        />
                                    );
                                })}
                            </svg>

                            {SESSION_NODES.map((node) => (
                                <motion.div
                                    key={node.id}
                                    className="absolute rounded-full border border-cyan-200/70 bg-cyan-300/90 shadow-[0_0_25px_rgba(56,189,248,0.8)]"
                                    style={{
                                        width: `${node.size}px`,
                                        height: `${node.size}px`,
                                        left: `calc(${node.x}% - ${node.size / 2}px)`,
                                        top: `calc(${node.y}% - ${node.size / 2}px)`,
                                    }}
                                    animate={{
                                        y: [0, -10, 0],
                                        x: [0, 5, 0],
                                        opacity: [0.7, 1, 0.7],
                                    }}
                                    transition={{
                                        duration: 6,
                                        delay: node.delay,
                                        repeat: Infinity,
                                        ease: 'easeInOut',
                                    }}
                                >
                                    <div className="absolute inset-0 rounded-full bg-cyan-100/80 blur-[2px]" />
                                </motion.div>
                            ))}
                        </div>
                    </div>
                </motion.div>
            </div>

        </section>
    );
};

export default HeroSection;
