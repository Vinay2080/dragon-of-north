import {AnimatePresence, motion} from 'framer-motion';
import {Menu, Monitor, Moon, Shield, Sun, X} from 'lucide-react';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useTheme} from '../context/ThemeContext';

type ThemeMode = 'light' | 'dark' | 'system';

const THEME_SEQUENCE: ThemeMode[] = ['light', 'dark', 'system'];

const themeIcon = {
    light: Sun,
    dark: Moon,
    system: Monitor,
} as const;

const themeLabel = {
    light: 'Light',
    dark: 'Dark',
    system: 'System',
} as const;

const Navbar = () => {
    const {theme, setTheme} = useTheme();
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const navigate = useNavigate();

    const ThemeIcon = themeIcon[theme];

    const cycleTheme = () => {
        const currentIndex = THEME_SEQUENCE.indexOf(theme);
        const nextTheme = THEME_SEQUENCE[(currentIndex + 1) % THEME_SEQUENCE.length];
        setTheme(nextTheme);
    };

    return (
        <header className="fixed inset-x-0 top-0 z-50 border-b border-white/20 bg-white/60 backdrop-blur-xl transition-colors duration-300 dark:border-white/10 dark:bg-slate-950/60">
            <nav className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
                <button
                    type="button"
                    onClick={() => navigate('/')}
                    className="group inline-flex items-center gap-2 rounded-lg px-2 py-1.5 transition hover:bg-slate-900/5 dark:hover:bg-white/5"
                >
                    <span className="rounded-md border border-cyan-300/50 bg-cyan-400/20 p-1 text-cyan-700 transition group-hover:scale-105 dark:border-cyan-300/30 dark:bg-cyan-300/10 dark:text-cyan-200">
                        <Shield size={16} />
                    </span>
                    <span className="text-sm font-semibold tracking-wide text-slate-800 dark:text-slate-100 sm:text-base">
                        Dragon of North
                    </span>
                </button>

                <div className="hidden items-center gap-2 md:flex">
                    <button
                        type="button"
                        onClick={cycleTheme}
                        className="inline-flex items-center gap-2 rounded-lg border border-slate-200/80 bg-white/70 px-3 py-2 text-sm text-slate-700 shadow-sm transition hover:-translate-y-0.5 hover:bg-white dark:border-slate-700 dark:bg-slate-900/70 dark:text-slate-200 dark:hover:bg-slate-900"
                        aria-label="Cycle theme"
                    >
                        <ThemeIcon size={16} />
                        <span>{themeLabel[theme]}</span>
                    </button>

                    <button
                        type="button"
                        onClick={() => navigate('/login')}
                        className="rounded-lg bg-cyan-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-cyan-400"
                    >
                        Login / Signup
                    </button>
                </div>

                <button
                    type="button"
                    onClick={() => setIsMobileMenuOpen((prev) => !prev)}
                    className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-slate-200/80 bg-white/70 text-slate-700 shadow-sm transition hover:bg-white dark:border-slate-700 dark:bg-slate-900/70 dark:text-slate-200 md:hidden"
                    aria-label="Toggle mobile menu"
                >
                    {isMobileMenuOpen ? <X size={18} /> : <Menu size={18} />}
                </button>
            </nav>

            <AnimatePresence>
                {isMobileMenuOpen && (
                    <motion.div
                        initial={{opacity: 0, height: 0}}
                        animate={{opacity: 1, height: 'auto'}}
                        exit={{opacity: 0, height: 0}}
                        transition={{duration: 0.2, ease: 'easeInOut'}}
                        className="overflow-hidden border-t border-white/20 bg-white/55 px-4 pb-4 pt-2 backdrop-blur-xl dark:border-white/10 dark:bg-slate-950/65 md:hidden"
                    >
                        <div className="flex flex-col gap-2">
                            <button
                                type="button"
                                onClick={cycleTheme}
                                className="flex items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-white/60 dark:text-slate-200 dark:hover:bg-white/10"
                            >
                                <ThemeIcon size={16} />
                                Theme: {themeLabel[theme]}
                            </button>

                            <button
                                type="button"
                                onClick={() => {
                                    setIsMobileMenuOpen(false);
                                    navigate('/login');
                                }}
                                className="rounded-lg bg-cyan-500 px-3 py-2 text-left text-sm font-semibold text-white transition hover:bg-cyan-400"
                            >
                                Login / Signup
                            </button>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </header>
    );
};

export default Navbar;
