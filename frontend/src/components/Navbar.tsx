import {AnimatePresence, motion} from 'framer-motion';
import {Menu, Monitor, Moon, Shield, Sun, X} from '../shims/lucide-react';
import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useTheme} from '../context/ThemeContext';
import {useAuth} from '../context/authUtils';
import ProfileDropdown from './ProfileDropdown';

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
    const {setTheme} = useTheme();
    const {isAuthenticated, logout} = useAuth();
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const [scrolled, setScrolled] = useState(false);
    const [isThemeOpen, setIsThemeOpen] = useState(false);
    const navigate = useNavigate();
    useEffect(() => {
        const handleScroll = () => {
            setScrolled(window.scrollY > 20);
        };
        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    return (
        <header className={`navbar ${scrolled ? 'scrolled' : ''}`}>
            <button
                type="button"
                onClick={() => navigate('/')}
                className="navbar-brand"
            >
                <span className="navbar-brand-glyph">▣</span>
                <span>Dragon of North</span>
            </button>

            <div className="hidden md:flex items-center gap-3">
                <ProfileDropdown/>
            </div>

            <button
                type="button"
                onClick={() => setIsMobileMenuOpen((prev) => !prev)}
                className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-slate-200/80 bg-white/70 md:hidden"
                aria-label="Toggle mobile menu"
            >
                {isMobileMenuOpen ? <X size={18}/> : <Menu size={18}/>}
            </button>

            <AnimatePresence>
                {isMobileMenuOpen && (
                    <motion.div
                        initial={{opacity: 0, height: 0}}
                        animate={{opacity: 1, height: 'auto'}}
                        exit={{opacity: 0, height: 0}}
                        transition={{duration: 0.2, ease: 'easeInOut'}}
                        className="absolute left-0 right-0 top-[60px] overflow-hidden px-4 pb-4 pt-2 md:hidden"
                        style={{
                            background: 'var(--don-bg-surface)',
                            borderTop: '1px solid var(--don-border-subtle)'
                        }}
                    >
                        <div className="flex flex-col gap-2">
                            <div className="p-2 flex flex-col gap-1">
                                <div className="relative">
                                    <button
                                        type="button"
                                        onClick={() => setIsThemeOpen((v) => !v)}
                                        className="flex items-center justify-between w-full px-4 py-2 rounded-md hover:bg-muted"
                                    >
                                        <span>Theme</span>
                                        <span>▸</span>
                                    </button>

                                    <div className={`mt-1 ${isThemeOpen ? '' : 'hidden'}`}>
                                        {THEME_SEQUENCE.map((t) => {
                                            const Icon = themeIcon[t];
                                            return (
                                                <button
                                                    key={t}
                                                    type="button"
                                                    onClick={() => {
                                                        setTheme(t);
                                                        setIsThemeOpen(false);
                                                        setIsMobileMenuOpen(false);
                                                    }}
                                                    className="flex items-center gap-2 w-full px-4 py-2 text-sm rounded-md hover:bg-muted"
                                                >
                                                    <Icon size={14}/>
                                                    <span>{themeLabel[t]}</span>
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>

                                {!isAuthenticated ? (
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
                                ) : (
                                    <>
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setIsMobileMenuOpen(false);
                                                navigate('/sessions');
                                            }}
                                            className="w-full text-left px-4 py-2 rounded-md hover:bg-muted flex items-center gap-2"
                                        >
                                            <Shield size={14}/>
                                            <span>Sessions</span>
                                        </button>

                                        <button
                                            type="button"
                                            onClick={async () => {
                                                setIsMobileMenuOpen(false);
                                                try {
                                                    await logout();
                                                    navigate('/');
                                                } catch (e) {
                                                    console.error(e);
                                                }
                                            }}
                                            className="w-full text-left px-4 py-2 rounded-md hover:bg-muted flex items-center gap-2"
                                        >
                                            <X size={14}/>
                                            <span>Logout</span>
                                        </button>
                                    </>
                                )}
                            </div>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </header>
    );
};

// noinspection JSUnusedGlobalSymbols
export default Navbar;
