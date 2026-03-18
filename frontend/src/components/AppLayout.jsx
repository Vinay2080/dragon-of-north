import React, {useEffect, useMemo, useRef, useState} from 'react';
import {Outlet, useLocation, useNavigate} from 'react-router-dom';
import * as Icons from '../shims/lucide-react';
import ProfileDropdown from './ProfileDropdown';
import CascadingMenu from './CascadingMenu';
import ScrollToTopButton from './ScrollToTopButton';

const SIDEBAR_EXPANDED_KEY = 'don-dashboard-sidebar-expanded';
const {Home, Menu, Shield, X, BookOpen, Zap, Lock} = Icons;

const isDesktopViewport = () => window.matchMedia('(min-width: 768px)').matches;

const navItems = [
    {id: 'home', label: 'Home', icon: Home, to: '/'},
    {id: 'sessions', label: 'Sessions', icon: Shield, to: '/sessions'},
];

const authSystemMenu = [
    {
        id: 'overview',
        label: 'Overview',
        icon: BookOpen,
        items: [
            {id: 'home', label: 'Home', onClick: null, to: '/'},
            {id: 'architecture', label: 'Architecture', onClick: null, to: '/architecture'},
        ],
    },
    {
        id: 'core-concepts',
        label: 'Core Concepts',
        icon: Zap,
        items: [
            {id: 'features', label: 'Features', onClick: null, to: '/features'},
            {
                id: 'identifier-flow',
                label: 'Identifier Flow',
                icon: Lock,
                items: [
                    {id: 'jwt-flow', label: 'JWT Flow', to: '/identifier-flow'},
                    {id: 'refresh-rotation', label: 'Refresh Rotation', to: '/identifier-flow'},
                ],
            },
            {id: 'security-demo', label: 'Security Demo', onClick: null, to: '/security-demo'},
        ],
    },
    {
        id: 'platform-info',
        label: 'Platform Info',
        icon: Shield,
        items: [
            {id: 'deployment', label: 'Deployment', onClick: null, to: '/deployment'},
            {id: 'privacy', label: 'Privacy', onClick: null, to: '/privacy'},
            {id: 'terms', label: 'Terms', onClick: null, to: '/terms'},
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

    const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
    const [isSidebarExpanded, setIsSidebarExpanded] = useState(() => localStorage.getItem(SIDEBAR_EXPANDED_KEY) === 'true');
    const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);

    const profileMenuRef = useRef(null);

    useEffect(() => {
        localStorage.setItem(SIDEBAR_EXPANDED_KEY, String(isSidebarExpanded));
    }, [isSidebarExpanded]);

    useEffect(() => {
        if (!isMobileSidebarOpen) {
            return;
        }

        const originalOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';

        return () => {
            document.body.style.overflow = originalOverflow;
        };
    }, [isMobileSidebarOpen]);


    useEffect(() => {
        if (!isProfileMenuOpen) {
            return;
        }

        const handlePointerDown = (event) => {
            const clickedOutsideProfileMenu = !profileMenuRef.current?.contains(event.target);

            if (clickedOutsideProfileMenu) {
                setIsProfileMenuOpen(false);
            }
        };

        const handleEscape = (event) => {
            if (event.key === 'Escape') {
                setIsProfileMenuOpen(false);
                setIsMobileSidebarOpen(false);
            }
        };

        document.addEventListener('mousedown', handlePointerDown);
        document.addEventListener('keydown', handleEscape);

        return () => {
            document.removeEventListener('mousedown', handlePointerDown);
            document.removeEventListener('keydown', handleEscape);
        };
    }, [isProfileMenuOpen]);

    const handleNavSelect = (item) => {
        setIsProfileMenuOpen(false);
        setIsMobileSidebarOpen(false);
        navigate(item.to);
    };


    const handleSidebarToggle = () => {
        if (isDesktopViewport()) {
            setIsSidebarExpanded((expanded) => !expanded);
            return;
        }

        setIsMobileSidebarOpen(false);
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
                <button
                    type="button"
                    className={`dashboard-sidebar-backdrop ${isMobileSidebarOpen ? 'dashboard-sidebar-backdrop--visible' : ''}`}
                    onClick={() => setIsMobileSidebarOpen(false)}
                    aria-label="Close navigation menu"
                    aria-hidden={!isMobileSidebarOpen}
                    tabIndex={isMobileSidebarOpen ? 0 : -1}
                />

                <aside
                    className={`dashboard-sidebar ${isMobileSidebarOpen ? 'dashboard-sidebar--mobile-open' : ''}`}
                    aria-label="Primary"
                >
                    <div className="dashboard-sidebar__top">
                        <button
                            type="button"
                            className="dashboard-sidebar__toggle"
                            onClick={handleSidebarToggle}
                            aria-label={isSidebarExpanded ? 'Collapse sidebar' : 'Expand sidebar'}
                            title={isSidebarExpanded ? 'Collapse sidebar' : 'Expand sidebar'}
                        >
                            {isMobileSidebarOpen ? <X size={20}/> : <Menu size={20}/>} 
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
                        <div className="dashboard-topbar__left">
                            <button
                                type="button"
                                className="dashboard-mobile-menu-btn"
                                onClick={() => setIsMobileSidebarOpen(true)}
                                aria-label="Open navigation menu"
                                aria-expanded={isMobileSidebarOpen}
                            >
                                <Menu size={20}/>
                            </button>
                            <h1 className="dashboard-topbar__title">{title}</h1>
                        </div>
                        <div className="dashboard-topbar__actions">
                            <CascadingMenu
                                trigger="Authentication System"
                                items={authSystemMenu}
                                onItemClick={(item) => {
                                    if (item.to) {
                                        navigate(item.to);
                                    }
                                }}
                                className="flex-shrink-0"
                            />

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
            <ScrollToTopButton/>
        </div>
    );
};

export default AppLayout;

