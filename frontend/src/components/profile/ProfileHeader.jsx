import React from 'react';

const ProfileHeader = ({
                           avatarSrc,
                           displayName,
                           username,
                           email,
                           bio,
                           activeSessions,
                           lastLoginAt,
                           onManageSessions
                       }) => {
    const resolvedDisplayName = displayName || username || 'User';
    const shouldShowUsername = Boolean(username) && username !== resolvedDisplayName;

    return (
        <section className="rounded-xl border border-border bg-card p-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex items-center gap-4">
                    <img
                        src={avatarSrc}
                        alt="Profile avatar"
                        className="h-16 w-16 rounded-full border border-border object-cover"
                    />
                    <div>
                        <h1 className="text-2xl text-foreground">
                            <span className="font-bold">{resolvedDisplayName}</span>
                            {shouldShowUsername ? <span className="font-normal"> ({username})</span> : null}
                        </h1>
                        <p className="text-sm text-muted-foreground">{email || 'No email available'}</p>
                        {bio ? <p className="mt-2 text-sm text-muted-foreground">{bio}</p> : null}
                    </div>
                </div>

                <div className="min-w-[180px] space-y-2 text-sm text-muted-foreground">
                    <p>
                        <span className="font-semibold text-foreground">Active sessions:</span> {activeSessions}
                    </p>
                    <p>
                        <span className="font-semibold text-foreground">Last login:</span> {lastLoginAt}
                    </p>
                    <button
                        type="button"
                        onClick={onManageSessions}
                        className="rounded-md border border-border px-3 py-1.5 text-sm font-medium text-foreground"
                    >
                        Manage sessions
                    </button>
                </div>
            </div>
        </section>
    );
};

export default ProfileHeader;

