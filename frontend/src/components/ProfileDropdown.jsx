import React, {useEffect, useMemo, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../context/authUtils';
import * as Icons from '../shims/lucide-react';

const {User, X} = Icons;

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
    const {isAuthenticated, logout, user} = useAuth();
    const [isOpen, setIsOpen] = useState(false);
    const ref = useRef(null);
    const navigate = useNavigate();
    const [failedAvatarUrl, setFailedAvatarUrl] = useState(null);

    useEffect(() => {
        const onDocClick = (e) => {
            if (!ref.current) return;
            if (!ref.current.contains(e.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', onDocClick);
        return () => document.removeEventListener('mousedown', onDocClick);
    }, []);

    const doLogout = async () => {
        setIsOpen(false);
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
        if (!explicitAvatar || failedAvatarUrl === explicitAvatar) {
            return buildDicebearAvatarUrl(fallbackSeed);
        }
        return explicitAvatar;
    }, [user, fallbackSeed, failedAvatarUrl]);

    return (
        <>
            <div className="relative" ref={ref}>
                <button
                    type="button"
                    onClick={() => {
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
                        onError={() => {
                            const explicitAvatar = user?.avatarUrl || user?.avatar_url;
                            if (explicitAvatar) {
                                setFailedAvatarUrl(explicitAvatar);
                            }
                        }}
                    />
                </button>

                <ul
                    className={`profile-dropdown-menu absolute right-0 mt-2 w-44 rounded-md bg-popover border border-border ${isOpen ? '' : 'hidden'}`}
                    role="menu"
                    aria-label="Profile menu"
                    onKeyDown={(event) => {
                        if (event.key === 'Escape') {
                            setIsOpen(false);
                        }
                    }}
                >
                    <li className="p-2">
                        <ul className="flex flex-col gap-1">
                            {!isAuthenticated ? (
                                <li>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setIsOpen(false);
                                            navigate('/login');
                                        }}
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
                                                navigate('/profile');
                                            }}
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
