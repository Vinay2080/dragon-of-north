import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../context/authUtils';
import {useToast} from '../hooks/useToast';
import {apiService} from '../services/apiService';
import {API_CONFIG} from '../config';
import {formatDateTime} from '../components/sessions/sessionFormatters';
import ProfileHeader from '../components/profile/ProfileHeader.jsx';
import ProfileInfoSection from '../components/profile/ProfileInfoSection.jsx';
import ProfileSettings from '../components/profile/ProfileSettings.jsx';

const EMPTY_PROFILE = {
    username: '',
    displayName: '',
    bio: '',
    avatarUrl: '',
    authProvider: null,
};

const EMPTY_PROFILE_ERRORS = {
    username: [],
    displayName: [],
    bio: [],
};

const normalizeProfile = (payload = {}) => ({
    username: payload?.username || payload?.user_name || '',
    displayName: payload?.displayName || payload?.display_name || '',
    bio: payload?.bio || '',
    avatarUrl: payload?.avatarUrl || payload?.avatar_url || '',
    authProvider: payload?.authProvider || payload?.auth_provider || null,
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

const getSessionTimestamp = (session) => {
    const value = session?.last_used_at || session?.created_at || session?.issued_at || session?.login_at;
    const timestamp = value ? new Date(value).getTime() : 0;
    return Number.isFinite(timestamp) ? timestamp : 0;
};

const buildProfileFromUser = (user) => ({
    username: user?.username || user?.user_name || '',
    displayName: user?.displayName || user?.display_name || '',
    bio: user?.bio || '',
    avatarUrl: user?.avatarUrl || user?.avatar_url || '',
    authProvider: user?.authProvider || user?.auth_provider || null,
});

const PROFILE_PATCH_FIELD_MAP = {
    displayName: 'display_name',
    avatarUrl: 'avatar_url',
};

const buildOptimisticProfile = (initialProfile, profileForm) => ({
    ...initialProfile,
    username: profileForm.username,
    displayName: profileForm.displayName,
    bio: profileForm.bio,
});

const ProfilePage = () => {
    const navigate = useNavigate();
    const {user, patchUser, syncUserProfile} = useAuth();
    const {toast} = useToast();

    const [loadingProfile, setLoadingProfile] = useState(true);
    const [isProfileSubmitting, setIsProfileSubmitting] = useState(false);
    const [initialProfile, setInitialProfile] = useState(() => buildProfileFromUser(user));
    const [profileForm, setProfileForm] = useState(() => buildProfileFromUser(user));
    const [profileErrors, setProfileErrors] = useState(EMPTY_PROFILE_ERRORS);
    const [isEditingProfile, setIsEditingProfile] = useState(false);

    const [activeSessionsCount, setActiveSessionsCount] = useState(0);
    const [lastLoginAt, setLastLoginAt] = useState('—');
    const cachedProfileRef = useRef(buildProfileFromUser(user));

    const applyProfileLocally = useCallback((profilePayload) => {
        cachedProfileRef.current = profilePayload;
        setInitialProfile(profilePayload);
        setProfileForm(profilePayload);
        patchUser({
            username: profilePayload.username,
            displayName: profilePayload.displayName,
            avatarUrl: profilePayload.avatarUrl,
            bio: profilePayload.bio,
            authProvider: profilePayload.authProvider,
        });
    }, [patchUser]);

    useEffect(() => {
        let isMounted = true;

        const loadProfile = async () => {
            setLoadingProfile(true);
            setProfileErrors(EMPTY_PROFILE_ERRORS);

            const result = await apiService.get(API_CONFIG.ENDPOINTS.PROFILE);
            if (!isMounted) {
                return;
            }

            if (apiService.isErrorResponse(result)) {
                toast.error(result.backendMessage || result.message || 'Unable to load profile.');
                // Keep page interactive with the last known profile snapshot when backend is unavailable.
                setInitialProfile(cachedProfileRef.current);
                setProfileForm(cachedProfileRef.current);
                setLoadingProfile(false);
                return;
            }

            const profilePayload = normalizeProfile(getResponseData(result) || {});
            applyProfileLocally(profilePayload);
            setLoadingProfile(false);
        };

        void loadProfile();

        return () => {
            isMounted = false;
        };
    }, [applyProfileLocally, toast]);

    useEffect(() => {
        let isMounted = true;

        const loadSessionsPreview = async () => {
            const result = await apiService.get(API_CONFIG.ENDPOINTS.SESSIONS_ALL);
            if (!isMounted || apiService.isErrorResponse(result) || !Array.isArray(result?.data)) {
                return;
            }

            const sessions = result.data;
            const activeSessions = sessions.filter((session) => !session.revoked);
            const sortedByLastSeen = [...sessions].sort((a, b) => getSessionTimestamp(b) - getSessionTimestamp(a));
            const lastSeen = sortedByLastSeen[0]?.last_used_at || sortedByLastSeen[0]?.created_at;

            setActiveSessionsCount(activeSessions.length);
            setLastLoginAt(formatDateTime(lastSeen));
        };

        void loadSessionsPreview();

        return () => {
            isMounted = false;
        };
    }, []);

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
                const apiField = PROFILE_PATCH_FIELD_MAP[field] || field;
                candidate[apiField] = currentValue;
            }
        });

        return candidate;
    }, [initialProfile, profileForm]);

    const hasProfileChanges = Object.keys(profileUpdatePayload).length > 0;

    const fallbackSeed = useMemo(() => resolveUserSeed(profileForm.username ? profileForm : user), [profileForm, user]);

    const avatarSrc = useMemo(() => {
        return profileForm.avatarUrl || user?.avatarUrl || user?.avatar_url || buildDicebearAvatarUrl(fallbackSeed);
    }, [profileForm.avatarUrl, user, fallbackSeed]);

    const setInlineErrorByCode = (result) => {
        const errorCode = result?.errorCode || result?.raw?.code;

        if (errorCode === 'USERNAME_TAKEN') {
            setProfileErrors((prev) => ({
                ...prev,
                username: [result.backendMessage || result.message || 'Username already exists.'],
            }));
            return true;
        }

        return false;
    };

    const cancelProfileEdit = () => {
        setProfileForm(initialProfile);
        setProfileErrors(EMPTY_PROFILE_ERRORS);
        setIsEditingProfile(false);
    };

    const submitProfile = async (event) => {
        event.preventDefault();
        if (!hasProfileChanges || isProfileSubmitting) {
            return;
        }

        const previousProfile = initialProfile;
        const optimisticProfile = buildOptimisticProfile(initialProfile, profileForm);
        setProfileErrors(EMPTY_PROFILE_ERRORS);
        setIsProfileSubmitting(true);
        applyProfileLocally(optimisticProfile);

        const result = await apiService.patch(API_CONFIG.ENDPOINTS.PROFILE, profileUpdatePayload);
        if (apiService.isErrorResponse(result)) {
            applyProfileLocally(previousProfile);
            const consumed = setInlineErrorByCode(result);
            if (!consumed) {
                toast.error(result.backendMessage || result.message || 'Unable to update profile.');
            }
            setIsProfileSubmitting(false);
            return;
        }

        const responseData = getResponseData(result);
        if (responseData) {
            applyProfileLocally(normalizeProfile(responseData));
        } else {
            const freshUser = await syncUserProfile({
                ...user,
                ...optimisticProfile,
            });

            if (freshUser) {
                applyProfileLocally(normalizeProfile(freshUser));
            }
        }

        toast.success('Profile updated successfully.');
        setIsEditingProfile(false);
        setIsProfileSubmitting(false);
    };

    return (
        <div
            className="mx-auto w-full max-w-6xl space-y-6 bg-[radial-gradient(circle_at_top,rgba(20,184,166,0.08),transparent_32%),linear-gradient(180deg,rgba(248,250,252,0.96),rgba(248,250,252,1))] px-4 py-6 dark:bg-[radial-gradient(circle_at_top,rgba(20,184,166,0.10),transparent_28%),linear-gradient(180deg,#0B1220,#0F172A)] sm:px-6 lg:px-8">
            <ProfileHeader
                avatarSrc={avatarSrc}
                displayName={profileForm.displayName || user?.displayName || 'User'}
                username={profileForm.username || user?.username || ''}
                email={user?.email || user?.identifier}
                bio={profileForm.bio}
                activeSessions={activeSessionsCount}
                lastLoginAt={lastLoginAt}
                onManageSessions={() => navigate('/sessions')}
            />

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                <ProfileInfoSection
                    loading={loadingProfile}
                    profileForm={profileForm}
                    profileErrors={profileErrors}
                    hasChanges={hasProfileChanges}
                    isSubmitting={isProfileSubmitting}
                    isEditing={isEditingProfile}
                    onEdit={() => setIsEditingProfile(true)}
                    onCancel={cancelProfileEdit}
                    onFieldChange={(field, value) => {
                        setProfileForm((prev) => ({...prev, [field]: value}));
                    }}
                    onSubmit={submitProfile}
                />

                <ProfileSettings authProvider={profileForm.authProvider || user?.authProvider || user?.auth_provider}/>
            </div>

            <section
                className="group rounded-3xl border border-slate-200/80 bg-[rgba(255,255,255,0.86)] p-6 shadow-[0_18px_36px_rgba(15,23,42,0.08)] transition-all duration-300 hover:-translate-y-0.5 hover:shadow-[0_24px_44px_rgba(20,184,166,0.12)] dark:border-slate-800/80 dark:bg-[rgba(11,18,32,0.92)]">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Session summary / activity</h2>
                        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                            You currently have {activeSessionsCount} active session{activeSessionsCount === 1 ? '' : 's'}.
                        </p>
                    </div>
                    <div
                        className="rounded-2xl border border-emerald-200/80 bg-white/80 px-4 py-3 shadow-sm dark:border-emerald-500/25 dark:bg-slate-900/75">
                        <p className="text-xs uppercase tracking-[0.18em] text-emerald-700 dark:text-emerald-300">Current
                            device</p>
                        <p className="mt-1 text-sm font-semibold text-slate-800 dark:text-slate-100">Desktop browser</p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">Last
                            activity: {lastLoginAt || '—'}</p>
                    </div>
                </div>
                <button
                    type="button"
                    onClick={() => navigate('/sessions')}
                    className="profile-primary-btn mt-4 h-11"
                >
                    View all sessions
                </button>
            </section>
        </div>
    );
};

export default ProfilePage;
