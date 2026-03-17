import React, {useEffect, useMemo, useRef, useState} from 'react';
import {Outlet, useLocation, useNavigate} from 'react-router-dom';
import * as Icons from '../shims/lucide-react';
import ProfileDropdown from './ProfileDropdown';

const SIDEBAR_EXPANDED_KEY = 'don-dashboard-sidebar-expanded';
const {Home, Menu, Shield} = Icons;

const navItems = [
    {id: 'home', label: 'Home', icon: Home, to: '/'},
    {id: 'sessions', label: 'Sessions', icon: Shield, to: '/sessions'},
];

const authSystemSections = [
    {
        title: 'Overview',
        items: [
            {label: 'Home', to: '/'},
            {label: 'Architecture', to: '/architecture'},
        ],
    },
    {
        title: 'Core Concepts',
        items: [
            {label: 'Features', to: '/features'},
            {label: 'Identifier Flow', to: '/identifier-flow'},
            {label: 'Security Demo', to: '/security-demo'},
        ],
    },
    {
        title: 'Platform Info',
        items: [
            {label: 'Deployment', to: '/deployment'},
            {label: 'Privacy', to: '/privacy'},
            {label: 'Terms', to: '/terms'},
        ],
    },
];

const pageTitleByPath = {
    '/': 'Home',
    '/features': 'Features',
    '/architecture': 'Architecture',
    '/security-demo': 'Security Demo',
    '/identifier-flow': 'Identifier Flow',
    '/deployment': 'Deployment',
    '/privacy': 'Privacy Policy',
    '/terms': 'Terms of Service',
    '/signup': 'Sign Up',
    '/otp': 'OTP Verification',
    '/login': 'Login',
    '/auth/callback': 'Completing Sign-In',
    '/forgot-password': 'Forgot Password',
    '/reset-password': 'Reset Password',
    '/sessions': 'Session Management',
};

const AppLayout = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const [isAuthMenuOpen, setIsAuthMenuOpen] = useState(false);
    const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
    const [isThemeOpen, setIsThemeOpen] = useState(false);
    const [isSidebarExpanded, setIsSidebarExpanded] = useState(() => localStorage.getItem(SIDEBAR_EXPANDED_KEY) === 'true');

    const authMenuRef = useRef(null);
    const profileMenuRef = useRef(null);

    useEffect(() => {
        localStorage.setItem(SIDEBAR_EXPANDED_KEY, String(isSidebarExpanded));
    }, [isSidebarExpanded]);

    useEffect(() => {
        if (!isProfileMenuOpen && !isAuthMenuOpen && !isThemeOpen) {
            return;
        }

        const handlePointerDown = (event) => {
            const clickedOutsideAuthMenu = !authMenuRef.current?.contains(event.target);
            const clickedOutsideProfileMenu = !profileMenuRef.current?.contains(event.target);

            if (clickedOutsideAuthMenu && clickedOutsideProfileMenu) {
                setIsAuthMenuOpen(false);
                setIsProfileMenuOpen(false);
                setIsThemeOpen(false);
            }
        };

        const handleEscape = (event) => {
            if (event.key === 'Escape') {
                setIsAuthMenuOpen(false);
                setIsProfileMenuOpen(false);
                setIsThemeOpen(false);
            }
        };

        document.addEventListener('mousedown', handlePointerDown);
        document.addEventListener('keydown', handleEscape);

        return () => {
            document.removeEventListener('mousedown', handlePointerDown);
            document.removeEventListener('keydown', handleEscape);
        };
    }, [isAuthMenuOpen, isProfileMenuOpen, isThemeOpen]);

    const handleNavSelect = (item) => {
        setIsAuthMenuOpen(false);
        setIsProfileMenuOpen(false);
        setIsThemeOpen(false);
        navigate(item.to);
    };

    const handleAuthMenuNavigate = (to) => {
        setIsAuthMenuOpen(false);
        setIsProfileMenuOpen(false);
        setIsThemeOpen(false);
        navigate(to);
    };

    const activeNavItem = useMemo(() => {
        if (location.pathname === '/') return 'home';
        if (location.pathname.startsWith('/sessions')) return 'sessions';
        return '';
    }, [location.pathname]);

    const title = pageTitleByPath[location.pathname] || 'Dragon of North';

    return (
        <div className="dashboard-shell">
            <div
                className={`dashboard-layout ${isSidebarExpanded ? 'dashboard-layout--expanded' : 'dashboard-layout--collapsed'}`}>
                <aside className="dashboard-sidebar" aria-label="Primary">
                    <div className="dashboard-sidebar__top">
                        <button
                            type="button"
                            className="dashboard-sidebar__toggle"
                            onClick={() => setIsSidebarExpanded((expanded) => !expanded)}
                            aria-label={isSidebarExpanded ? 'Collapse sidebar' : 'Expand sidebar'}
                            title={isSidebarExpanded ? 'Collapse sidebar' : 'Expand sidebar'}
                        >
                            <Menu size={20}/>
                        </button>
                    </div>

                    <div className="dashboard-sidebar__brand">Dragon of North</div>
                    <nav className="dashboard-nav" aria-label="Primary navigation">
                        {navItems.map((item) => {
                            const ItemIcon = item.icon;
                            return (
                                <button
                                    key={item.id}
                                    type="button"
                                    onClick={() => handleNavSelect(item)}
                                    className={`dashboard-nav-item ${activeNavItem === item.id ? 'dashboard-nav-item--active' : ''}`}
                                    aria-current={activeNavItem === item.id ? 'page' : undefined}
                                    aria-label={item.label}
                                    title={item.label}
                                    data-tooltip={item.label}
                                >
                                    <ItemIcon size={18}/>
                                    <span className="dashboard-nav-item__label">{item.label}</span>
                                </button>
                            );
                        })}
                    </nav>
                </aside>

                <div className="dashboard-content">
                    <header className="dashboard-topbar">
                        <h1 className="dashboard-topbar__title">{title}</h1>
                        <div className="dashboard-topbar__actions">
                            <div className="auth-system-menu" ref={authMenuRef}>
                                <button
                                    type="button"
                                    className="auth-system-trigger"
                                    aria-label="Open authentication system menu"
                                    aria-haspopup="menu"
                                    aria-expanded={isAuthMenuOpen}
                                    onClick={() => {
                                        setIsProfileMenuOpen(false);
                                        setIsAuthMenuOpen((open) => !open);
                                    }}
                                >
                                    <span>Authentication System</span>
                                </button>

                                <div
                                    className={`auth-system-dropdown ${isAuthMenuOpen ? 'auth-system-dropdown--open' : ''}`}
                                    role="menu">
                                    <div className="auth-system-dropdown__sections">
                                        {authSystemSections.map((section) => (
                                            <div key={section.title} className="auth-system-dropdown__section">
                                                <p className="auth-system-dropdown__title">{section.title}</p>
                                                <div className="auth-system-dropdown__items">
                                                    {section.items.map((item) => (
                                                        <button
                                                            key={item.to}
                                                            type="button"
                                                            role="menuitem"
                                                            onClick={() => handleAuthMenuNavigate(item.to)}
                                                            className="auth-system-dropdown__item"
                                                        >
                                                            {item.label}
                                                        </button>
                                                    ))}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>

                            <div className="dashboard-profile">
                                <ProfileDropdown/>
                            </div>
                        </div>
                    </header>

                    <main className="dashboard-main">
                        <Outlet/>
                    </main>
                </div>
            </div>
        </div>
    );
};

export default AppLayout;

