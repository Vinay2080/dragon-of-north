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
                                onFieldChange,
                                onSubmit
                            }) => {
    return (
        <section className="rounded-xl border border-border bg-card p-6">
            <div className="mb-4">
                <h2 className="text-lg font-semibold text-foreground">Account information</h2>
                <p className="text-sm text-muted-foreground">Update your public profile details.</p>
            </div>

            {loading ? <p className="mb-4 text-sm text-muted-foreground">Syncing profile...</p> : null}

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

                <AuthButton type="submit" disabled={!hasChanges || isSubmitting}>
                    {isSubmitting ? (
                        <span className="btn-loading-indicator">
                                <span className="spinner spinner-sm"></span>
                                <span>Saving profile...</span>
                            </span>
                    ) : 'Save profile'}
                </AuthButton>
            </form>
        </section>
    );
};

export default ProfileInfoSection;


