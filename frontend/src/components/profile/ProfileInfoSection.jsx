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
        <div className="rounded-lg border border-slate-200/80 bg-white/70 px-3 py-2 dark:border-slate-800 dark:bg-slate-900/70">
            <p className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">{label}</p>
            <p className="mt-1 text-sm font-medium text-slate-800 dark:text-slate-100">{value || fallback}</p>
        </div>
    );

    return (
        <section className="group rounded-2xl border border-slate-200/80 bg-slate-50/80 p-6 shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-md dark:border-slate-800 dark:bg-slate-900/70">
            <div className="mb-4 flex items-start justify-between gap-3">
                <div>
                    <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Account information</h2>
                    <p className="text-sm text-slate-500 dark:text-slate-400">Manage your public profile details.</p>
                </div>
                {!isEditing ? (
                    <button
                        type="button"
                        onClick={onEdit}
                        className="h-9 rounded-lg border border-slate-300 px-3 text-sm font-medium text-slate-700 transition hover:bg-slate-100 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
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
                            placeholder="Bio"
                        />
                        <ValidationError errors={profileErrors.bio}/>
                    </div>

                    <div className="flex items-center gap-2">
                        <AuthButton type="submit" disabled={!hasChanges || isSubmitting}>
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
                            className="h-10 rounded-lg border border-slate-300 px-4 text-sm font-medium text-slate-700 transition hover:bg-slate-100 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
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

