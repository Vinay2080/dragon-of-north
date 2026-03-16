import {useEffect, useMemo, useRef, useState} from 'react';
import {NavLink, useNavigate} from 'react-router-dom';
import {useAuth} from '../context/authUtils';

const publicLinks = [
    {to: '/', label: 'Home'},
    {to: '/features', label: 'Features'},
    {to: '/architecture', label: 'Architecture'},
    {to: '/security-demo', label: 'Security Demo'},
    {to: '/identifier-flow', label: 'Identifier Flow'},
    {to: '/deployment', label: 'Deployment'},
    {to: '/privacy', label: 'Privacy'},
    {to: '/terms', label: 'Terms'},
];

const DefaultAvatarIcon = () => (
    <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden>
        <path
            d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Zm0 2c-4.1 0-7 2.1-7 5v1h14v-1c0-2.9-2.9-5-7-5Z"
            fill="currentColor"
        />
    </svg>
);

const DocsLayout = ({title, subtitle, children}) => {
    const navigate = useNavigate();
    const {isAuthenticated, user, logout} = useAuth();
    const [menuOpen, setMenuOpen] = useState(false);
    const menuRef = useRef(null);

    useEffect(() => {
        const onPointerDown = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setMenuOpen(false);
            }
        };

        const onKeyDown = (event) => {
            if (event.key === 'Escape') {
                setMenuOpen(false);
            }
        };

        document.addEventListener('mousedown', onPointerDown);
        document.addEventListener('keydown', onKeyDown);

        return () => {
            document.removeEventListener('mousedown', onPointerDown);
            document.removeEventListener('keydown', onKeyDown);
        };
    }, []);

    const avatarLetter = useMemo(() => {
        const identifier = user?.identifier || user?.email || '';
        return identifier ? identifier.trim().charAt(0).toUpperCase() : '';
    }, [user]);

    const googleAvatar = user?.picture || user?.avatar_url || user?.photo_url || null;

    const closeAndNavigate = (path) => {
        setMenuOpen(false);
        navigate(path);
    };

    const handleLogout = async () => {
        setMenuOpen(false);
        await logout();
        navigate('/login', {replace: true});
    };

    return (
        <div className="relative min-h-screen overflow-hidden bg-[#070b14] text-slate-100">
            <div className="dynamic-grid-overlay" aria-hidden />
            <header className="sticky top-0 z-20 border-b border-white/10 bg-[#070b14]/85 backdrop-blur">
                <div className="mx-auto flex w-full max-w-7xl items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8">
                    <div>
                        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300">Identity Architecture Lab</p>
                        <h1 className="text-lg font-semibold">{title}</h1>
                    </div>

                    <div className="flex items-center gap-3">
                        <nav className="flex max-w-[65vw] flex-wrap justify-end gap-2 sm:max-w-none">
                            {publicLinks.map((link) => (
                                <NavLink
                                    key={link.to}
                                    to={link.to}
                                    className={({isActive}) => `rounded-lg border px-3 py-1.5 text-xs font-medium transition ${isActive ? 'border-cyan-300/70 bg-cyan-300/10 text-cyan-100' : 'border-white/15 text-slate-300 hover:border-white/35'}`}
                                >
                                    {link.label}
                                </NavLink>
                            ))}
                            {!isAuthenticated && (
                                <NavLink
                                    to="/login"
                                    className={({isActive}) => `rounded-lg border px-3 py-1.5 text-xs font-medium transition ${isActive ? 'border-cyan-300/70 bg-cyan-300/10 text-cyan-100' : 'border-white/15 text-slate-300 hover:border-white/35'}`}
                                >
                                    Login
                                </NavLink>
                            )}
                        </nav>

                        {isAuthenticated && (
                            <div className="relative" ref={menuRef}>
                                <button
                                    type="button"
                                    onClick={() => setMenuOpen((prev) => !prev)}
                                    aria-haspopup="menu"
                                    aria-expanded={menuOpen}
                                    aria-label="Open profile menu"
                                    className="flex h-9 w-9 items-center justify-center overflow-hidden rounded-full border border-white/20 bg-white/10 text-sm font-semibold text-cyan-100 shadow-[0_0_0_1px_rgba(0,255,255,0.08)] transition hover:border-cyan-300/70 hover:bg-white/20 hover:shadow-[0_0_20px_rgba(56,189,248,0.2)] focus:outline-none focus-visible:ring-2 focus-visible:ring-cyan-300/60"
                                >
                                    {googleAvatar ? (
                                        <img src={googleAvatar} alt="Profile" className="h-full w-full object-cover" referrerPolicy="no-referrer" />
                                    ) : avatarLetter ? (
                                        <span>{avatarLetter}</span>
                                    ) : (
                                        <DefaultAvatarIcon />
                                    )}
                                </button>

                                <div
                                    role="menu"
                                    aria-label="Profile actions"
                                    className={`absolute right-0 mt-2 w-52 origin-top-right rounded-xl border border-white/10 bg-[rgba(20,20,30,0.85)] p-1.5 shadow-2xl backdrop-blur-xl transition-all duration-200 ${menuOpen ? 'pointer-events-auto translate-y-0 opacity-100' : 'pointer-events-none -translate-y-1 opacity-0'}`}
                                >
                                    <button type="button" role="menuitem" onClick={() => closeAndNavigate('/dashboard')} className="block w-full rounded-lg px-3 py-2 text-left text-sm text-slate-200 transition hover:bg-cyan-500/15 hover:text-cyan-100">Dashboard</button>
                                    <button type="button" role="menuitem" onClick={() => closeAndNavigate('/dashboard#sessions')} className="block w-full rounded-lg px-3 py-2 text-left text-sm text-slate-200 transition hover:bg-cyan-500/15 hover:text-cyan-100">Account Sessions</button>
                                    <button type="button" role="menuitem" onClick={handleLogout} className="block w-full rounded-lg px-3 py-2 text-left text-sm text-rose-200 transition hover:bg-rose-500/15 hover:text-rose-100">Logout</button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </header>
            <main className="relative z-10 mx-auto w-full max-w-7xl space-y-8 px-4 py-8 sm:px-6 lg:px-8">
                <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
                    <h2 className="text-3xl font-semibold tracking-tight">{title}</h2>
                    <p className="mt-2 max-w-4xl text-slate-300">{subtitle}</p>
                </div>
                {children}
            </main>
        </div>
    );
};

export default DocsLayout;
