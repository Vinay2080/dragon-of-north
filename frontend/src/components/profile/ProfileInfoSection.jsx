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
                                onSubmit
                            }) => {
    const readOnlyField = (label, value, fallback = 'Not set') => (
        <div
            className="rounded-2xl border border-slate-200/80 bg-white/80 px-4 py-3 shadow-sm dark:border-slate-800/80 dark:bg-slate-900/75">
            <p className="text-xs uppercase tracking-[0.18em] text-teal-700 dark:text-teal-300">{label}</p>
            <p className="mt-1 text-sm font-medium text-slate-800 dark:text-slate-100">{value || fallback}</p>
        </div>
    );

    return (
        <section
            className="group rounded-3xl border border-slate-200/80 bg-[rgba(255,255,255,0.86)] p-6 shadow-[0_18px_36px_rgba(15,23,42,0.08)] transition-all duration-300 hover:-translate-y-0.5 hover:shadow-[0_24px_44px_rgba(20,184,166,0.12)] dark:border-slate-800/80 dark:bg-[rgba(11,18,32,0.92)]">
            <div className="mb-4 flex items-start justify-between gap-3">
                <div>
                    <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Account information</h2>
                    <p className="text-sm text-slate-500 dark:text-slate-400">Manage your public profile details.</p>
                </div>
                {!isEditing ? (
                    <button
                        type="button"
                        onClick={onEdit}
                        className="h-11 rounded-2xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-teal-400/25 dark:border-slate-700 dark:bg-slate-900/70 dark:text-slate-200 dark:hover:bg-slate-800"
                    >
                        Edit profile
                    </button>
                ) : null}
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
                        <label className="auth-label">Username</label>
                        <AuthInput
                            value={profileForm.username}
                            onChange={(event) => onFieldChange('username', event.target.value)}
                            hasError={Boolean(profileErrors.username.length)}
                            className="border-slate-200 bg-white/90 focus:border-[#14B8A6] focus:ring-4 focus:ring-teal-500/15 dark:border-slate-700 dark:bg-slate-950/60 dark:focus:border-[#14B8A6] dark:focus:ring-teal-400/10"
                            placeholder="Username"
                            autoComplete="username"
                        />
                        <ValidationError errors={profileErrors.username}/>
                    </div>

                    <div>
                        <label className="auth-label">Display name</label>
                        <AuthInput
                            value={profileForm.displayName}
                            onChange={(event) => onFieldChange('displayName', event.target.value)}
                            hasError={Boolean(profileErrors.displayName.length)}
                            className="border-slate-200 bg-white/90 focus:border-[#14B8A6] focus:ring-4 focus:ring-teal-500/15 dark:border-slate-700 dark:bg-slate-950/60 dark:focus:border-[#14B8A6] dark:focus:ring-teal-400/10"
                            placeholder="Display name"
                        />
                        <ValidationError errors={profileErrors.displayName}/>
                    </div>

                    <div>
                        <label className="auth-label">Bio</label>
                        <AuthInput
                            value={profileForm.bio}
                            onChange={(event) => onFieldChange('bio', event.target.value)}
                            hasError={Boolean(profileErrors.bio.length)}
                            className="border-slate-200 bg-white/90 focus:border-[#14B8A6] focus:ring-4 focus:ring-teal-500/15 dark:border-slate-700 dark:bg-slate-950/60 dark:focus:border-[#14B8A6] dark:focus:ring-teal-400/10"
                            placeholder="Bio"
                        />
                        <ValidationError errors={profileErrors.bio}/>
                    </div>

                    <div className="flex items-center gap-2">
                        <AuthButton
                            type="submit"
                            disabled={!hasChanges || isSubmitting}
                            className="profile-primary-btn h-11"
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
                            className="h-11 rounded-2xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-teal-400/25 dark:border-slate-700 dark:bg-slate-900/70 dark:text-slate-200 dark:hover:bg-slate-800"
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
