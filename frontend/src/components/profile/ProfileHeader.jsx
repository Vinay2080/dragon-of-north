import React from 'react';

const ProfileHeader = ({
                           avatarSrc,
                           displayName,
                           username,
                           email,
                           bio,
                           activeSessions,
                           lastLoginAt,
                           lastLoginRelative,
                           onManageSessions
                       }) => {
    const resolvedDisplayName = displayName || username || 'User';
    const shouldShowUsername = Boolean(username) && username !== resolvedDisplayName;
    const fallbackSeed = (username || displayName || email || 'user').trim();
    const fallbackAvatar = `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(fallbackSeed || 'user')}`;

    return (
        <section className="group rounded-2xl border border-slate-200/80 bg-slate-50/80 p-6 shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-lg dark:border-slate-800 dark:bg-gradient-to-br dark:from-slate-900 dark:via-slate-900 dark:to-violet-950/40 dark:shadow-[0_0_32px_rgba(124,58,237,0.08)]">
            <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
                <div className="flex min-w-0 items-center gap-4">
                    <img
                        src={avatarSrc}
                        alt="Profile avatar"
                        className="h-14 w-14 rounded-full border border-slate-300 object-cover transition-transform duration-300 group-hover:scale-[1.02] dark:border-slate-700"
                        referrerPolicy="no-referrer"
                        onError={(event) => {
                            event.currentTarget.src = fallbackAvatar;
                        }}
                    />
                    <div className="min-w-0">
                        <h1 className="truncate text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
                            <span>{resolvedDisplayName}</span>
                            {shouldShowUsername ? <span className="font-normal"> ({username})</span> : null}
                        </h1>
                        <p className="truncate text-sm text-slate-500 dark:text-slate-400">{email || 'No email available'}</p>
                        {bio ? <p className="mt-2 max-w-2xl text-sm text-slate-600 dark:text-slate-300">{bio}</p> : null}
                    </div>
                </div>

                <div className="grid min-w-[220px] gap-3 text-sm text-slate-600 dark:text-slate-300">
                    <div className="rounded-xl border border-slate-200/80 bg-white/70 px-3 py-2 dark:border-slate-800 dark:bg-slate-900/70">
                        <p className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">Active sessions</p>
                        <p className="text-lg font-semibold text-slate-900 dark:text-slate-100">{activeSessions}</p>
                    </div>
                    <div className="rounded-xl border border-slate-200/80 bg-white/70 px-3 py-2 dark:border-slate-800 dark:bg-slate-900/70">
                        <p className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">Last login</p>
                        <p className="font-semibold text-slate-900 dark:text-slate-100">{lastLoginRelative || '—'}</p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">{lastLoginAt || '—'}</p>
                    </div>
                    <button
                        type="button"
                        onClick={onManageSessions}
                        className="h-10 rounded-lg border border-violet-300 bg-violet-600 px-4 text-sm font-semibold text-white transition-all hover:bg-violet-500 dark:border-violet-500/40 dark:bg-violet-600"
                    >
                        Manage sessions
                    </button>
                </div>
            </div>
        </section>
    );
};

export default ProfileHeader;
