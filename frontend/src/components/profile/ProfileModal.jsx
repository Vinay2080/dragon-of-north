import React, {useEffect, useMemo, useState} from 'react';
import {X} from 'lucide-react';
import AuthButton from '../auth/AuthButton';
import AuthInput from '../auth/AuthInput';
import PasswordInput from '../auth/PasswordInput';
import ValidationError from '../Validation/ValidationError';
import {apiService} from '../../services/apiService';
import {API_CONFIG} from '../../config';
import {useToast} from '../../hooks/useToast';
import {useAuth} from '../../context/authUtils';

const EMPTY_PROFILE = {
    username: '',
    displayName: '',
    bio: '',
    avatarUrl: '',
};

const EMPTY_PASSWORD_STATE = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
};

const EMPTY_PROFILE_ERRORS = {
    username: [],
    displayName: [],
    bio: [],
};

const EMPTY_PASSWORD_ERRORS = {
    currentPassword: [],
    newPassword: [],
    confirmPassword: [],
};

const MIN_PASSWORD_LENGTH = 8;

const normalizeProfile = (payload = {}) => ({
    username: payload?.username || '',
    displayName: payload?.displayName || '',
    bio: payload?.bio || '',
    avatarUrl: payload?.avatarUrl || '',
});

const getResponseData = (result) => {
    if (result?.data && typeof result.data === 'object' && !Array.isArray(result.data)) {
        return result.data;
    }

    if (result && typeof result === 'object' && !Array.isArray(result) && !result.type) {
        return result;
    }

    return null;
};

const ProfileModal = ({isOpen, onClose, onProfileUpdated}) => {
    const {toast} = useToast();
    const {user, patchUser, syncUserProfile} = useAuth();
    const [loadingProfile, setLoadingProfile] = useState(false);
    const [isProfileSubmitting, setIsProfileSubmitting] = useState(false);
    const [isPasswordSubmitting, setIsPasswordSubmitting] = useState(false);

    const [initialProfile, setInitialProfile] = useState(EMPTY_PROFILE);
    const [profileForm, setProfileForm] = useState(EMPTY_PROFILE);
    const [profileErrors, setProfileErrors] = useState(EMPTY_PROFILE_ERRORS);

    const [passwordForm, setPasswordForm] = useState(EMPTY_PASSWORD_STATE);
    const [passwordErrors, setPasswordErrors] = useState(EMPTY_PASSWORD_ERRORS);

    const resetPasswordForm = () => {
        setPasswordForm(EMPTY_PASSWORD_STATE);
        setPasswordErrors(EMPTY_PASSWORD_ERRORS);
    };

    const resetAllState = () => {
        setLoadingProfile(false);
        setIsProfileSubmitting(false);
        setIsPasswordSubmitting(false);
        setInitialProfile(EMPTY_PROFILE);
        setProfileForm(EMPTY_PROFILE);
        setProfileErrors(EMPTY_PROFILE_ERRORS);
        resetPasswordForm();
    };

    const closeModal = () => {
        resetAllState();
        onClose();
    };

    useEffect(() => {
        if (!isOpen) {
            return;
        }

        let isMounted = true;

        const loadProfile = async () => {
            setLoadingProfile(true);
            setProfileErrors(EMPTY_PROFILE_ERRORS);

            const result = await apiService.get(API_CONFIG.ENDPOINTS.PROFILE);
            if (!isMounted) {
                return;
            }

            if (apiService.isErrorResponse(result)) {
                setLoadingProfile(false);
                toast.error(result.backendMessage || result.message || 'Unable to load profile.');
                return;
            }

            const profilePayload = normalizeProfile(getResponseData(result) || {});
            setInitialProfile(profilePayload);
            setProfileForm(profilePayload);
            setLoadingProfile(false);
            onProfileUpdated?.(profilePayload);
        };

        void loadProfile();

        return () => {
            isMounted = false;
        };
    }, [isOpen, toast, onProfileUpdated]);

    useEffect(() => {
        if (!isOpen) {
            return;
        }

        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        return () => {
            document.body.style.overflow = previousOverflow;
        };
    }, [isOpen]);

    const profileUpdatePayload = useMemo(() => {
        const candidate = {};
        const fields = ['username', 'displayName', 'bio'];

        fields.forEach((field) => {
            const currentValue = profileForm[field];
            const initialValue = initialProfile[field];

            if (currentValue === undefined || currentValue === null) {
                return;
            }

            if (currentValue !== initialValue) {
                candidate[field] = currentValue;
            }
        });

        return candidate;
    }, [profileForm, initialProfile]);

    const hasProfileChanges = Object.keys(profileUpdatePayload).length > 0;

    const setInlineErrorByCode = (result, target) => {
        const errorCode = result?.errorCode || result?.raw?.code;

        if (target === 'profile' && errorCode === 'USERNAME_TAKEN') {
            setProfileErrors((prev) => ({
                ...prev,
                username: [result.backendMessage || result.message || 'Username already exists.'],
            }));
            return true;
        }

        if (target === 'password' && errorCode === 'PASSWORD_MISMATCH') {
            setPasswordErrors((prev) => ({
                ...prev,
                confirmPassword: [result.backendMessage || result.message || 'Passwords do not match.'],
            }));
            return true;
        }

        if (target === 'password' && errorCode === 'INVALID_CURRENT_PASSWORD') {
            setPasswordErrors((prev) => ({
                ...prev,
                currentPassword: [result.backendMessage || result.message || 'Current password is incorrect.'],
            }));
            return true;
        }

        return false;
    };

    const submitProfile = async (event) => {
        event.preventDefault();
        if (!hasProfileChanges || isProfileSubmitting) {
            return;
        }

        const previousProfile = initialProfile;
        const optimisticProfile = {
            ...initialProfile,
            username: profileForm.username,
            displayName: profileForm.displayName,
            bio: profileForm.bio,
        };
        setProfileErrors(EMPTY_PROFILE_ERRORS);
        setIsProfileSubmitting(true);
        setInitialProfile(optimisticProfile);
        setProfileForm(optimisticProfile);
        patchUser(optimisticProfile);
        onProfileUpdated?.(optimisticProfile);

        const result = await apiService.patch(API_CONFIG.ENDPOINTS.PROFILE, profileUpdatePayload);
        if (apiService.isErrorResponse(result)) {
            setInitialProfile(previousProfile);
            setProfileForm(previousProfile);
            patchUser(previousProfile);
            onProfileUpdated?.(previousProfile);
            const consumed = setInlineErrorByCode(result, 'profile');
            if (!consumed) {
                toast.error(result.backendMessage || result.message || 'Unable to update profile.');
            }
            setIsProfileSubmitting(false);
            return;
        }

        const responseData = getResponseData(result);
        let nextProfile = optimisticProfile;

        if (responseData) {
            nextProfile = normalizeProfile(responseData);
        } else {
            const freshUser = await syncUserProfile({
                ...user,
                ...optimisticProfile,
            });
            if (freshUser) {
                nextProfile = normalizeProfile(freshUser);
            }
        }

        setInitialProfile(nextProfile);
        setProfileForm(nextProfile);
        patchUser(nextProfile);
        onProfileUpdated?.(nextProfile);
        toast.success('Profile updated successfully.');
        setIsProfileSubmitting(false);
    };

    const validatePasswordForm = () => {
        const nextErrors = {
            currentPassword: [],
            newPassword: [],
            confirmPassword: [],
        };

        if (!passwordForm.currentPassword) {
            nextErrors.currentPassword.push('Current password is required.');
        }

        if (!passwordForm.newPassword) {
            nextErrors.newPassword.push('New password is required.');
        } else if (passwordForm.newPassword.length < MIN_PASSWORD_LENGTH) {
            nextErrors.newPassword.push(`New password must be at least ${MIN_PASSWORD_LENGTH} characters.`);
        }

        if (!passwordForm.confirmPassword) {
            nextErrors.confirmPassword.push('Please confirm your new password.');
        }

        if (passwordForm.newPassword && passwordForm.confirmPassword && passwordForm.newPassword !== passwordForm.confirmPassword) {
            nextErrors.confirmPassword.push('New password and confirm password must match.');
        }

        setPasswordErrors(nextErrors);
        return Object.values(nextErrors).every((errors) => errors.length === 0);
    };

    const submitPassword = async (event) => {
        event.preventDefault();
        if (isPasswordSubmitting) {
            return;
        }

        if (!validatePasswordForm()) {
            return;
        }

        setIsPasswordSubmitting(true);

        try {
            const result = await apiService.patch(API_CONFIG.ENDPOINTS.PASSWORD_CHANGE, {
                currentPassword: passwordForm.currentPassword,
                newPassword: passwordForm.newPassword,
            });

            if (apiService.isErrorResponse(result)) {
                const consumed = setInlineErrorByCode(result, 'password');
                if (!consumed) {
                    toast.error(result.backendMessage || result.message || 'Unable to update password.');
                }
                return;
            }

            toast.success('Password updated successfully.');
        } finally {
            // Clear sensitive fields immediately after request completion.
            resetPasswordForm();
            setIsPasswordSubmitting(false);
        }
    };

    if (!isOpen) {
        return null;
    }

    return (
        <div className="protected-gate__overlay" onClick={closeModal}>
            <div className="protected-gate__modal" onClick={(event) => event.stopPropagation()}>
                <div className="relative mb-4">
                    <h2 className="protected-gate__title">Profile</h2>
                    <button
                        type="button"
                        aria-label="Close modal"
                        onClick={closeModal}
                        className="absolute right-3 top-3 h-8 w-8 flex items-center justify-center rounded-full bg-red-50 text-red-500 hover:bg-red-100 hover:text-red-600 active:bg-red-200 transition-colors duration-200 shadow-sm hover:shadow-md"
                    >
                        <X className="h-4 w-4"/>
                    </button>
                </div>

                {loadingProfile ? (
                    <div className="auth-section">
                        <p className="protected-gate__description">Loading profile...</p>
                    </div>
                ) : (
                    <>
                        <form className="auth-section space-y-3" onSubmit={submitProfile}>
                            <div>
                                <label className="auth-label">Username</label>
                                <AuthInput
                                    value={profileForm.username}
                                    onChange={(event) => setProfileForm((prev) => ({
                                        ...prev,
                                        username: event.target.value
                                    }))}
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
                                    onChange={(event) => setProfileForm((prev) => ({
                                        ...prev,
                                        displayName: event.target.value
                                    }))}
                                    hasError={Boolean(profileErrors.displayName.length)}
                                    placeholder="Display name"
                                />
                                <ValidationError errors={profileErrors.displayName}/>
                            </div>

                            <div>
                                <label className="auth-label">Bio</label>
                                <AuthInput
                                    value={profileForm.bio}
                                    onChange={(event) => setProfileForm((prev) => ({...prev, bio: event.target.value}))}
                                    hasError={Boolean(profileErrors.bio.length)}
                                    placeholder="Bio"
                                />
                                <ValidationError errors={profileErrors.bio}/>
                            </div>

                            <AuthButton type="submit" disabled={!hasProfileChanges || isProfileSubmitting}>
                                {isProfileSubmitting ? (
                                    <span className="btn-loading-indicator">
                                        <span className="spinner spinner-sm"></span>
                                        <span>Saving profile...</span>
                                    </span>
                                ) : 'Save profile'}
                            </AuthButton>
                        </form>

                        <form className="auth-section space-y-3 mt-6" onSubmit={submitPassword}>
                            <h3 className="auth-label">Change password</h3>

                            <div>
                                <label className="auth-label">Current password</label>
                                <PasswordInput
                                    value={passwordForm.currentPassword}
                                    onChange={(event) => setPasswordForm((prev) => ({
                                        ...prev,
                                        currentPassword: event.target.value
                                    }))}
                                    hasError={Boolean(passwordErrors.currentPassword.length)}
                                    placeholder="Current password"
                                />
                                <ValidationError errors={passwordErrors.currentPassword}/>
                            </div>

                            <div>
                                <label className="auth-label">New password</label>
                                <PasswordInput
                                    value={passwordForm.newPassword}
                                    onChange={(event) => setPasswordForm((prev) => ({
                                        ...prev,
                                        newPassword: event.target.value
                                    }))}
                                    hasError={Boolean(passwordErrors.newPassword.length)}
                                    placeholder="New password"
                                />
                                <ValidationError errors={passwordErrors.newPassword}/>
                            </div>

                            <div>
                                <label className="auth-label">Confirm password</label>
                                <PasswordInput
                                    value={passwordForm.confirmPassword}
                                    onChange={(event) => setPasswordForm((prev) => ({
                                        ...prev,
                                        confirmPassword: event.target.value
                                    }))}
                                    hasError={Boolean(passwordErrors.confirmPassword.length)}
                                    placeholder="Confirm password"
                                />
                                <ValidationError errors={passwordErrors.confirmPassword}/>
                            </div>

                            <AuthButton type="submit" disabled={isPasswordSubmitting}>
                                {isPasswordSubmitting ? (
                                    <span className="btn-loading-indicator">
                                        <span className="spinner spinner-sm"></span>
                                        <span>Updating password...</span>
                                    </span>
                                ) : 'Update password'}
                            </AuthButton>
                        </form>
                    </>
                )}
            </div>
        </div>
    );
};

export default ProfileModal;

