import React from 'react';
import {Menu, Monitor} from 'lucide-react';
import IconButton from '../ui/IconButton.jsx';

const ProfileHeader = ({
                           avatarSrc,
                           displayName,
                           username,
                           email,
                           bio,
                           activeSessions,
                           lastLoginAt,
                           onManageSessions,
                           onAvatarClick
                       }) => {
    const resolvedDisplayName = displayName || username || 'User';
    const shouldShowUsername = Boolean(username) && username !== resolvedDisplayName;
    const fallbackSeed = (username || displayName || email || 'user').trim();
    const fallbackAvatar = `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(fallbackSeed || 'user')}`;

    return (
        <section
            className="relative isolate overflow-hidden rounded-3xl border border-slate-200/80 bg-[linear-gradient(135deg,rgba(20,184,166,0.12),rgba(255,255,255,0.92),rgba(14,165,233,0.12))] p-6 shadow-[0_24px_56px_rgba(15,23,42,0.10)] transition-all duration-300 hover:-translate-y-0.5 hover:shadow-[0_28px_64px_rgba(20,184,166,0.16)] dark:border-slate-800/80 dark:bg-[linear-gradient(135deg,rgba(11,18,32,0.98),rgba(15,23,42,0.96),rgba(8,47,73,0.88))] dark:shadow-[0_0_44px_rgba(20,184,166,0.12)]"
        >
            <div className="pointer-events-none absolute inset-0 -z-10 opacity-75">
                <div className="absolute -left-14 -top-20 h-56 w-56 rounded-full bg-teal-400/20 blur-3xl"></div>
                <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-sky-400/20 blur-3xl"></div>
                <div className="absolute bottom-0 left-1/3 h-60 w-60 rounded-full bg-fuchsia-400/10 blur-3xl"></div>
            </div>

            <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
                <div className="flex min-w-0 items-center gap-5">
                    <div className="relative">
                        <div
                            className="absolute -inset-1.5 rounded-full bg-[conic-gradient(from_180deg_at_50%_50%,#14B8A6,#0EA5E9,#A855F7,#14B8A6)] opacity-70 blur-md"></div>
                        <div
                            className="absolute -inset-0.5 rounded-full bg-[conic-gradient(from_180deg_at_50%_50%,rgba(20,184,166,0.95),rgba(14,165,233,0.95),rgba(168,85,247,0.85),rgba(20,184,166,0.95))] opacity-90"></div>
                        <img
                            src={avatarSrc}
                            alt="Profile avatar"
                            className="relative h-[88px] w-[88px] rounded-full border border-white/70 object-cover shadow-[0_18px_44px_rgba(14,165,233,0.18)] ring-1 ring-black/5 transition-transform duration-300 group-hover:scale-[1.02] dark:border-slate-950/40 dark:shadow-[0_0_36px_rgba(20,184,166,0.18)] cursor-zoom-in"
                            referrerPolicy="no-referrer"
                            onClick={onAvatarClick}
                            role="button"
                            tabIndex={0}
                            onKeyDown={(event) => {
                                if ((event.key === 'Enter' || event.key === ' ') && onAvatarClick) {
                                    event.preventDefault();
                                    onAvatarClick();
                                }
                            }}
                            onError={(event) => {
                                event.currentTarget.src = fallbackAvatar;
                            }}
                        />
                    </div>

                    <div className="min-w-0">
                        <h1 className="truncate text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-50 sm:text-3xl">
                            <span>{resolvedDisplayName}</span>
                            {shouldShowUsername ? (
                                <span className="font-normal text-teal-700 dark:text-teal-300"> ({username})</span>
                            ) : null}
                        </h1>
                        <p className="mt-1 truncate text-sm font-medium text-slate-600 dark:text-slate-300">
                            {email || 'No email available'}
                        </p>
                        {bio ? (
                            <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600/90 dark:text-slate-300">
                                {bio}
                            </p>
                        ) : null}

                        <div className="mt-4 flex flex-wrap items-center gap-2">
                            <IconButton
                                label="Manage sessions"
                                tooltip="Manage sessions"
                                onClick={onManageSessions}
                                className="h-10 w-10"
                            >
                                <Menu className="h-[18px] w-[18px]"/>
                            </IconButton>
                        </div>
                    </div>
                </div>

                <div className="grid min-w-[260px] gap-3 text-sm text-slate-600 dark:text-slate-300">
                    <div
                        className="rounded-2xl border border-emerald-200/70 bg-white/60 px-4 py-3 shadow-sm backdrop-blur dark:border-emerald-500/25 dark:bg-slate-950/30">
                        <div className="flex items-center justify-between gap-3">
                            <p className="text-xs uppercase tracking-[0.18em] text-emerald-700 dark:text-emerald-300">Active
                                sessions</p>
                            <Monitor className="h-4 w-4 text-emerald-600/80 dark:text-emerald-300/80"/>
                        </div>
                        <div className="mt-1 flex items-center gap-2">
                            <span
                                className="inline-flex h-2.5 w-2.5 rounded-full bg-[#22C55E] shadow-[0_0_12px_rgba(34,197,94,0.55)]"></span>
                            <p className="text-lg font-semibold text-slate-900 dark:text-slate-50">{activeSessions}</p>
                        </div>
                    </div>

                    <div
                        className="rounded-2xl border border-teal-200/70 bg-white/60 px-4 py-3 shadow-sm backdrop-blur dark:border-teal-500/25 dark:bg-slate-950/30">
                        <div className="flex items-center justify-between gap-3">
                            <p className="text-xs uppercase tracking-[0.18em] text-teal-700 dark:text-teal-300">Last
                                login</p>
                            <span
                                className="inline-flex h-4 w-4 items-center justify-center text-teal-600/80 dark:text-teal-300/80">⏱</span>
                        </div>
                        <p className="mt-1 font-semibold text-slate-900 dark:text-slate-50">{lastLoginAt || '—'}</p>
                    </div>
                </div>
            </div>
        </section>
    );
};

export default ProfileHeader;
