import {AnimatePresence, motion} from 'framer-motion';
import {Menu, Shield, X} from '../shims/lucide-react';
import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../context/authUtils';
import ProfileDropdown from './ProfileDropdown';
import ThemeSwitcherPopover from './ThemeSwitcherPopover';

const Navbar = () => {
    const {isAuthenticated, logout} = useAuth();
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const navigate = useNavigate();

    // Scroll state management
    const [isVisible, setIsVisible] = useState(true);
    const [isExpanded, setIsExpanded] = useState(() => window.scrollY < 50);
    const [isShrunk, setIsShrunk] = useState(() => window.scrollY > 80);
    const [lastScrollY, setLastScrollY] = useState(window.scrollY);

    useEffect(() => {
        let ticking = false;

        const handleScroll = () => {
            if (!ticking) {
                window.requestAnimationFrame(() => {
                    const currentScrollY = window.scrollY;
                    const scrollDelta = currentScrollY - lastScrollY;

                    // Update expanded state (top of page)
                    setIsExpanded(currentScrollY < 50);

                    // Update shrunk state
                    setIsShrunk(currentScrollY > 80);

                    // Show/hide based on scroll direction and position
                    if (currentScrollY > 150 && scrollDelta > 0) {
                        // Scrolling down past 150px - hide
                        setIsVisible(false);
                    } else if (scrollDelta < 0) {
                        // Scrolling up - show
                        setIsVisible(true);
                    } else if (currentScrollY <= 150) {
                        // Near top - always show
                        setIsVisible(true);
                    }

                    setLastScrollY(currentScrollY);
                    ticking = false;
                });
                ticking = true;
            }
        };

        window.addEventListener('scroll', handleScroll, { passive: true });
        return () => window.removeEventListener('scroll', handleScroll);
    }, [lastScrollY]);

    return (
        <header
            className={`navbar ${isExpanded ? 'expanded' : ''} ${isShrunk ? 'shrunk' : ''}`}
            style={{
                transform: isVisible ? 'translateY(0)' : 'translateY(-100%)',
                transition: 'transform 0.3s ease-in-out, height 0.3s ease-in-out, background 0.15s ease-in-out',
            }}
        >
            <button
                type="button"
                onClick={() => navigate('/')}
                className="navbar-brand"
            >
                <span className="navbar-brand-glyph">▣</span>
                <span>Dragon of North</span>
            </button>

            <div className="flex items-center gap-2">
                <ThemeSwitcherPopover/>

                <div className="hidden md:flex items-center gap-3">
                    <ProfileDropdown/>
                </div>

                <button
                    type="button"
                    onClick={() => setIsMobileMenuOpen((prev) => !prev)}
                    className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-border bg-background/70 text-foreground md:hidden"
                    aria-label="Toggle mobile menu"
                >
                    {isMobileMenuOpen ? <X size={18}/> : <Menu size={18}/>}
                </button>
            </div>

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
