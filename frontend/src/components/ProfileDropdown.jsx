import React, {useEffect, useMemo, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useTheme} from '../context/ThemeContext';
import {useAuth} from '../context/authUtils';
import * as Icons from '../shims/lucide-react';

const {Monitor, Moon, Sun, User, X} = Icons;

const THEME_SEQUENCE = ['light', 'dark', 'system'];
const SUBMENU_CLOSE_DELAY_MS = 150;

const resolveUserSeed = (user) => {
    const seedSource = user?.username || user?.displayName || user?.identifier || 'user';
    const normalizedSeed = String(seedSource).trim();

    if (!normalizedSeed) {
        return 'user';
    }

    if (normalizedSeed.includes('@')) {
        return normalizedSeed.split('@')[0] || 'user';
    }

    return normalizedSeed;
};

const buildDicebearAvatarUrl = (seed) => {
    return `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(seed)}`;
};

export default function ProfileDropdown() {
    const {setTheme} = useTheme();
    const {isAuthenticated, logout, user} = useAuth();
    const [isOpen, setIsOpen] = useState(false);
    const [isThemeSubmenuOpen, setIsThemeSubmenuOpen] = useState(false);
    const ref = useRef(null);
    const themeTriggerRef = useRef(null);
    const firstThemeItemRef = useRef(null);
    const closeSubmenuTimerRef = useRef(null);
    const navigate = useNavigate();
    const [avatarLoadFailed, setAvatarLoadFailed] = useState(false);

    const clearSubmenuCloseTimer = () => {
        if (closeSubmenuTimerRef.current) {
            clearTimeout(closeSubmenuTimerRef.current);
            closeSubmenuTimerRef.current = null;
        }
    };

    const openThemeSubmenu = () => {
        clearSubmenuCloseTimer();
        setIsThemeSubmenuOpen(true);
    };

    const closeThemeSubmenuNow = () => {
        clearSubmenuCloseTimer();
        setIsThemeSubmenuOpen(false);
    };

    const scheduleThemeSubmenuClose = () => {
        clearSubmenuCloseTimer();
        closeSubmenuTimerRef.current = setTimeout(() => {
            setIsThemeSubmenuOpen(false);
            closeSubmenuTimerRef.current = null;
        }, SUBMENU_CLOSE_DELAY_MS);
    };

    useEffect(() => {
        const onDocClick = (e) => {
            if (!ref.current) return;
            if (!ref.current.contains(e.target)) {
                setIsOpen(false);
                clearSubmenuCloseTimer();
                setIsThemeSubmenuOpen(false);
            }
        };
        document.addEventListener('mousedown', onDocClick);
        return () => document.removeEventListener('mousedown', onDocClick);
    }, []);

    useEffect(() => () => clearSubmenuCloseTimer(), []);

    const doLogout = async () => {
        setIsOpen(false);
        closeThemeSubmenuNow();
        try {
            await logout();
            navigate('/');
        } catch (e) {
            console.error('Logout failed', e);
        }
    };

    const fallbackSeed = useMemo(() => resolveUserSeed(user), [user]);

    const avatarSrc = useMemo(() => {
        const explicitAvatar = user?.avatarUrl || user?.avatar_url;
        if (avatarLoadFailed || !explicitAvatar) {
            return buildDicebearAvatarUrl(fallbackSeed);
        }
        return explicitAvatar;
    }, [user, fallbackSeed, avatarLoadFailed]);

    // Reset the fallback state whenever the avatar URL changes.
    // (Avoid triggering our lint rule that flags setState directly inside useEffect.)
    const lastAvatarUrlRef = useRef(null);
    const currentAvatarUrl = user?.avatarUrl || user?.avatar_url;
    if (lastAvatarUrlRef.current !== currentAvatarUrl) {
        lastAvatarUrlRef.current = currentAvatarUrl;
        if (avatarLoadFailed) {
            setAvatarLoadFailed(false);
        }
    }

    return (
        <>
            <div className="relative" ref={ref}>
                <button
                    type="button"
                    onClick={() => {
                        if (isOpen) {
                            closeThemeSubmenuNow();
                        }
                        setIsOpen((v) => !v);
                    }}
                    className="dashboard-avatar-btn"
                    aria-haspopup="menu"
                    aria-expanded={isOpen}
                    aria-label="Open profile menu"
                >
                    <img
                        src={avatarSrc}
                        alt="User avatar"
                        className="dashboard-avatar"
                        referrerPolicy="no-referrer"
                        onError={() => setAvatarLoadFailed(true)}
                    />
                </button>

                <ul
                    className={`profile-dropdown-menu absolute right-0 mt-2 w-44 rounded-md bg-popover border border-border ${isOpen ? '' : 'hidden'}`}
                    role="menu"
                    aria-label="Profile menu"
                    onKeyDown={(event) => {
                        if (event.key === 'Escape') {
                            setIsOpen(false);
                            closeThemeSubmenuNow();
                        }
                    }}
                >
                    <li className="p-2">
                        <ul className="flex flex-col gap-1">
                            <li
                                className="submenu-anchor"
                                onMouseEnter={openThemeSubmenu}
                                onMouseLeave={scheduleThemeSubmenuClose}
                            >
                                <button
                                    ref={themeTriggerRef}
                                    type="button"
                                    className={`menu-item w-full text-left ${isThemeSubmenuOpen ? 'menu-item--active' : ''}`}
                                    aria-haspopup="menu"
                                    aria-expanded={isThemeSubmenuOpen}
                                    onClick={() => setIsThemeSubmenuOpen((prev) => !prev)}
                                    onFocus={openThemeSubmenu}
                                    onKeyDown={(event) => {
                                        if (event.key === 'ArrowRight') {
                                            event.preventDefault();
                                            openThemeSubmenu();
                                            firstThemeItemRef.current?.focus();
                                        }
                                        if (event.key === 'ArrowLeft') {
                                            event.preventDefault();
                                            closeThemeSubmenuNow();
                                        }
                                    }}
                                >
                                    <span>Theme</span>
                                    <span
                                        className={`arrow transition-transform duration-200 ${isThemeSubmenuOpen ? 'rotate-180' : ''}`}>›</span>
                                </button>

                                {/* Hover bridge keeps the submenu open when the cursor moves diagonally */}
                                <div
                                    className={`submenu-bridge submenu-bridge--left ${isThemeSubmenuOpen ? 'is-active' : ''}`}
                                    onMouseEnter={openThemeSubmenu}
                                    onMouseLeave={scheduleThemeSubmenuClose}
                                    aria-hidden
                                />

                                <ul
                                    className={`submenu-panel submenu-panel--left ${isThemeSubmenuOpen ? 'is-open' : ''}`}
                                    role="menu"
                                    aria-label="Theme selection"
                                    onMouseEnter={openThemeSubmenu}
                                    onMouseLeave={scheduleThemeSubmenuClose}
                                >
                                    {THEME_SEQUENCE.map((t) => {
                                        const Icon = t === 'light' ? Sun : t === 'dark' ? Moon : Monitor;
                                        return (
                                            <li key={t} role="none">
                                                <button
                                                    ref={t === THEME_SEQUENCE[0] ? firstThemeItemRef : undefined}
                                                    type="button"
                                                    role="menuitem"
                                                    onClick={() => {
                                                        setTheme(t);
                                                        setIsOpen(false);
                                                        closeThemeSubmenuNow();
                                                    }}
                                                    className="submenu-item"
                                                    onKeyDown={(event) => {
                                                        if (event.key === 'ArrowLeft') {
                                                            event.preventDefault();
                                                            closeThemeSubmenuNow();
                                                            themeTriggerRef.current?.focus();
                                                        }
                                                    }}
                                                >
                                                    <Icon size={14}/>
                                                    <span className="capitalize">{t}</span>
                                                </button>
                                            </li>
                                        );
                                    })}
                                </ul>
                            </li>

                            {!isAuthenticated ? (
                                <li>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setIsOpen(false);
                                            closeThemeSubmenuNow();
                                            navigate('/login');
                                        }}
                                        onMouseEnter={closeThemeSubmenuNow}
                                        className="menu-item w-full text-left"
                                        role="menuitem"
                                    >
                                        Login
                                    </button>
                                </li>
                            ) : (
                                <>
                                    <li>
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setIsOpen(false);
                                                closeThemeSubmenuNow();
                                                navigate('/profile');
                                            }}
                                            onMouseEnter={closeThemeSubmenuNow}
                                            className="menu-item w-full text-left flex items-center gap-2"
                                            role="menuitem"
                                        >
                                            <User size={14}/>
                                            <span>Profile</span>
                                        </button>
                                    </li>

                                    <li>
                                        <button
                                            type="button"
                                            onClick={doLogout}
                                            onMouseEnter={closeThemeSubmenuNow}
                                            className="menu-item w-full text-left flex items-center gap-2"
                                            role="menuitem"
                                        >
                                            <X size={14}/>
                                            <span>Logout</span>
                                        </button>
                                    </li>
                                </>
                            )}
                        </ul>
                    </li>
                </ul>
            </div>
        </>
    );
}
