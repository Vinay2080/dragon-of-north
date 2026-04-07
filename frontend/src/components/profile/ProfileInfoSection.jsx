import React from 'react';
import AuthButton from '../auth/AuthButton';
import AuthInput from '../auth/AuthInput';
import ValidationError from '../Validation/ValidationError';

const ProfileInfoSection = ({
                                loading,
                                profileForm,
                                profileErrors,
                                hasChanges,
                                isSubmitting,
                                isEditing,
                                onFieldChange,
                                onEdit,
                                onCancel,
                                onSubmit,
                                onOpenAvatarUpload
                            }) => {
    const readOnlyField = (label, value, fallback = 'Not set') => (
        <div
            className="rounded-2xl border border-slate-200/70 bg-gradient-to-br from-white/85 to-slate-50/75 px-4 py-3 shadow-sm transition-all duration-200 hover:border-slate-200/90 hover:shadow-md dark:border-slate-700/70 dark:bg-gradient-to-br dark:from-slate-900/60 dark:to-slate-800/50 dark:hover:border-slate-700/90 dark:hover:shadow-lg">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-teal-700 dark:text-teal-300">{label}</p>
            <p className="mt-1.5 text-sm font-medium text-slate-900 dark:text-slate-50">{value || fallback}</p>
        </div>
    );

    return (
        <section
            className="group rounded-3xl border border-slate-200/80 bg-[rgba(255,255,255,0.88)] p-6 shadow-[0_20px_40px_rgba(15,23,42,0.10)] transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_28px_48px_rgba(20,184,166,0.14)] dark:border-slate-800/80 dark:bg-[rgba(11,18,32,0.94)]">
            <div className="mb-6 flex items-start justify-between gap-4">
                <div className="flex items-start gap-3">
                    <div className="mt-0.5 flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-teal-500/20 to-cyan-500/20 ring-1 ring-teal-300/30 dark:from-teal-500/15 dark:to-cyan-500/15 dark:ring-teal-400/20">
                        <svg className="h-5 w-5 text-teal-700 dark:text-teal-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                    </div>
                    <div>
                        <h2 className="text-lg font-bold text-slate-900 dark:text-slate-50">Account information</h2>
                        <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">Manage your public profile details.</p>
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={onOpenAvatarUpload}
                        className="h-10 rounded-lg border border-slate-300/80 bg-white/90 px-3 text-xs font-semibold text-slate-700 transition-all duration-200 hover:bg-slate-50 hover:border-slate-400 focus:outline-none focus:ring-2 focus:ring-teal-400/40 dark:border-slate-700/80 dark:bg-slate-900/70 dark:text-slate-200 dark:hover:bg-slate-800 dark:hover:border-slate-600"
                    >
                        Change avatar
                    </button>
                    {!isEditing ? (
                        <button
                            type="button"
                            onClick={onEdit}
                            className="h-10 rounded-lg border border-teal-300/60 bg-gradient-to-br from-teal-500/90 to-teal-600/90 px-3 text-xs font-semibold text-white shadow-md transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg focus:outline-none focus:ring-2 focus:ring-teal-400/40 dark:border-teal-400/40 dark:from-teal-600/90 dark:to-teal-700/90"
                        >
                            Edit profile
                        </button>
                    ) : null}
                </div>
            </div>

            {loading ? <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">Syncing profile...</p> : null}

            {!isEditing ? (
                <div className="space-y-3">
                    {readOnlyField('Username', profileForm.username, 'Not set')}
                    {readOnlyField('Display name', profileForm.displayName, 'Not set')}
                    {readOnlyField('Bio', profileForm.bio, 'No bio yet')}
                </div>
            ) : (
                <form className="space-y-4" onSubmit={onSubmit}>
                    <div>
                        <label className="block text-xs font-semibold uppercase tracking-[0.15em] text-slate-700 dark:text-slate-300 mb-2">Username</label>
                        <AuthInput
                            value={profileForm.username}
                            onChange={(event) => onFieldChange('username', event.target.value)}
                            hasError={Boolean(profileErrors.username.length)}
                            className="border-slate-200/70 bg-white/85 focus:border-teal-500 focus:ring-4 focus:ring-teal-500/20 dark:border-slate-700/70 dark:bg-slate-950/50 dark:focus:border-teal-400 dark:focus:ring-teal-400/15 rounded-lg transition-colors"
                            placeholder="Username"
                            autoComplete="username"
                        />
                        <ValidationError errors={profileErrors.username}/>
                    </div>

                    <div>
                        <label className="block text-xs font-semibold uppercase tracking-[0.15em] text-slate-700 dark:text-slate-300 mb-2">Display name</label>
                        <AuthInput
                            value={profileForm.displayName}
                            onChange={(event) => onFieldChange('displayName', event.target.value)}
                            hasError={Boolean(profileErrors.displayName.length)}
                            className="border-slate-200/70 bg-white/85 focus:border-teal-500 focus:ring-4 focus:ring-teal-500/20 dark:border-slate-700/70 dark:bg-slate-950/50 dark:focus:border-teal-400 dark:focus:ring-teal-400/15 rounded-lg transition-colors"
                            placeholder="Display name"
                        />
                        <ValidationError errors={profileErrors.displayName}/>
                    </div>

                    <div>
                        <label className="block text-xs font-semibold uppercase tracking-[0.15em] text-slate-700 dark:text-slate-300 mb-2">Bio</label>
                        <AuthInput
                            value={profileForm.bio}
                            onChange={(event) => onFieldChange('bio', event.target.value)}
                            hasError={Boolean(profileErrors.bio.length)}
                            className="border-slate-200/70 bg-white/85 focus:border-teal-500 focus:ring-4 focus:ring-teal-500/20 dark:border-slate-700/70 dark:bg-slate-950/50 dark:focus:border-teal-400 dark:focus:ring-teal-400/15 rounded-lg transition-colors"
                            placeholder="Bio"
                        />
                        <ValidationError errors={profileErrors.bio}/>
                    </div>

                    <div className="flex items-center gap-2 pt-2">
                        <AuthButton
                            type="submit"
                            disabled={!hasChanges || isSubmitting}
                            className="h-10 rounded-lg border-0 bg-gradient-to-br from-teal-500 to-teal-600 px-4 text-sm font-semibold text-white shadow-[0_12px_24px_rgba(20,184,166,0.28)] transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_16px_32px_rgba(20,184,166,0.36)] focus:outline-none focus:ring-2 focus:ring-teal-400/40 disabled:cursor-not-allowed disabled:opacity-50 dark:from-teal-600 dark:to-teal-700"
                        >
                            {isSubmitting ? (
                                <span className="btn-loading-indicator">
                                <span className="spinner spinner-sm"></span>
                                <span>Saving profile...</span>
                            </span>
                            ) : 'Save profile'}
                        </AuthButton>
                        <button
                            type="button"
                            onClick={onCancel}
                            className="h-10 rounded-lg border border-slate-300/80 bg-white/90 px-4 text-sm font-semibold text-slate-700 transition-all duration-200 hover:bg-slate-50 hover:border-slate-400 focus:outline-none focus:ring-2 focus:ring-slate-300/40 dark:border-slate-700/80 dark:bg-slate-900/70 dark:text-slate-200 dark:hover:bg-slate-800 dark:hover:border-slate-600"
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            )}
        </section>
    );
};

export default ProfileInfoSection;

