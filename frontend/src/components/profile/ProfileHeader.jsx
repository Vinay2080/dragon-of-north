import React from 'react';

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
            className="group rounded-3xl border border-slate-200/80 bg-[linear-gradient(135deg,rgba(20,184,166,0.08),rgba(255,255,255,0.90),rgba(14,165,233,0.08))] p-4 shadow-[0_20px_44px_rgba(15,23,42,0.10)] transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_28px_56px_rgba(20,184,166,0.14)] dark:border-slate-800/80 dark:bg-[linear-gradient(135deg,rgba(11,18,32,0.98),rgba(15,23,42,0.96),rgba(8,47,73,0.86))] dark:shadow-[0_0_40px_rgba(20,184,166,0.12)] sm:p-6">
            <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
                <div className="flex min-w-0 flex-col items-start gap-4 sm:flex-row sm:items-center">
                    <img
                        src={avatarSrc}
                        alt="Profile avatar"
                        className="h-16 w-16 rounded-full border-2 border-teal-200/80 object-cover ring-4 ring-teal-500/15 shadow-md transition-all duration-300 group-hover:scale-[1.04] group-hover:ring-teal-500/25 dark:border-teal-500/40 dark:ring-teal-400/15 dark:group-hover:ring-teal-400/30 cursor-zoom-in"
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
                    <div className="min-w-0">
                        <h1 className="break-words text-xl font-bold tracking-tight text-slate-900 dark:text-slate-50 sm:text-2xl">
                            <span>{resolvedDisplayName}</span>
                            {shouldShowUsername ? <span
                                className="font-normal text-teal-700 dark:text-teal-300"> ({username})</span> : null}
                        </h1>
                        <p className="break-words text-sm text-slate-500 dark:text-slate-400">{email || 'No email available'}</p>
                        {bio ?
                            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 dark:text-slate-300">{bio}</p> : null}
                    </div>
                </div>

                <div className="grid w-full gap-3 text-sm text-slate-600 dark:text-slate-300 lg:w-auto lg:min-w-[240px]">
                    <div
                        className="rounded-2xl border border-emerald-200/70 bg-gradient-to-br from-white/80 to-emerald-50/60 px-4 py-3 shadow-sm transition-all dark:border-emerald-500/25 dark:bg-gradient-to-br dark:from-slate-900/75 dark:to-emerald-500/8">
                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-emerald-700 dark:text-emerald-300">Active sessions</p>
                        <div className="mt-1.5 flex items-center gap-2">
                            <span
                                className="inline-flex h-2.5 w-2.5 rounded-full bg-[#22C55E] shadow-[0_0_12px_rgba(34,197,94,0.55)]"></span>
                            <p className="text-lg font-bold text-slate-900 dark:text-slate-50">{activeSessions}</p>
                        </div>
                    </div>
                    <div
                        className="rounded-2xl border border-teal-200/70 bg-gradient-to-br from-white/80 to-cyan-50/60 px-4 py-3 shadow-sm transition-all dark:border-teal-500/25 dark:bg-gradient-to-br dark:from-slate-900/75 dark:to-cyan-500/8">
                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-teal-700 dark:text-teal-300">Last login</p>
                        <p className="mt-1.5 font-bold text-slate-900 dark:text-slate-50">{lastLoginAt || '—'}</p>
                    </div>
                    <button
                        type="button"
                        onClick={onManageSessions}
                        className="h-11 w-full rounded-lg border border-teal-400/60 bg-gradient-to-br from-teal-500 to-cyan-600 px-3 text-sm font-semibold text-white shadow-[0_12px_28px_rgba(20,184,166,0.28)] transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_16px_36px_rgba(20,184,166,0.36)] focus:outline-none focus:ring-2 focus:ring-teal-400/50 disabled:cursor-not-allowed disabled:opacity-60 dark:border-teal-400/30 dark:from-teal-600 dark:to-cyan-700"
                    >
                        Manage sessions
                    </button>
                </div>
            </div>
        </section>
    );
};

export default ProfileHeader;
