import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../context/authUtils';
import {useToast} from '../hooks/useToast';
import {apiService} from '../services/apiService';
import {API_CONFIG} from '../config';
import {formatDateTime} from '../components/sessions/sessionFormatters';
import ProfileHeader from '../components/profile/ProfileHeader.jsx';
import ProfileInfoSection from '../components/profile/ProfileInfoSection.jsx';
import SecuritySection from '../components/profile/SecuritySection.jsx';

const EMPTY_PROFILE = {
    username: '',
    displayName: '',
    bio: '',
    avatarUrl: '',
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
});

const ProfilePage = () => {
    const navigate = useNavigate();
    const {user, patchUser} = useAuth();
    const {toast} = useToast();

    const [loadingProfile, setLoadingProfile] = useState(true);
    const [isProfileSubmitting, setIsProfileSubmitting] = useState(false);
    const [initialProfile, setInitialProfile] = useState(() => buildProfileFromUser(user));
    const [profileForm, setProfileForm] = useState(() => buildProfileFromUser(user));
    const [profileErrors, setProfileErrors] = useState(EMPTY_PROFILE_ERRORS);

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

            setActiveSessionsCount(activeSessions.length);
            setLastLoginAt(formatDateTime(sortedByLastSeen[0]?.last_used_at || sortedByLastSeen[0]?.created_at));
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
                candidate[field] = currentValue;
            }
        });

        return candidate;
    }, [initialProfile, profileForm]);

    const hasProfileChanges = Object.keys(profileUpdatePayload).length > 0;

    const fallbackSeed = useMemo(() => resolveUserSeed(profileForm.username ? profileForm : user), [profileForm, user]);

    const avatarSrc = useMemo(() => {
        return profileForm.avatarUrl || user?.avatarUrl || buildDicebearAvatarUrl(fallbackSeed);
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

    const submitProfile = async (event) => {
        event.preventDefault();
        if (!hasProfileChanges || isProfileSubmitting) {
            return;
        }

        setProfileErrors(EMPTY_PROFILE_ERRORS);
        setIsProfileSubmitting(true);

        const result = await apiService.patch(API_CONFIG.ENDPOINTS.PROFILE, profileUpdatePayload);
        if (apiService.isErrorResponse(result)) {
            const consumed = setInlineErrorByCode(result);
            if (!consumed) {
                toast.error(result.backendMessage || result.message || 'Unable to update profile.');
            }
            setIsProfileSubmitting(false);
            return;
        }

        const serverPayload = normalizeProfile(getResponseData(result) || profileForm);
        applyProfileLocally(serverPayload);
        toast.success('Profile updated successfully.');
        setIsProfileSubmitting(false);
    };

    return (
        <div className="mx-auto w-full max-w-6xl space-y-6 px-4 py-6 sm:px-6 lg:px-8">
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
                    onFieldChange={(field, value) => {
                        setProfileForm((prev) => ({...prev, [field]: value}));
                    }}
                    onSubmit={submitProfile}
                />

                <div className="space-y-6">
                    <SecuritySection/>

                    <section className="rounded-xl border border-border bg-card p-6">
                        <h2 className="text-lg font-semibold text-foreground">Session summary</h2>
                        <p className="mt-2 text-sm text-muted-foreground">
                            You currently have {activeSessionsCount} active
                            session{activeSessionsCount === 1 ? '' : 's'}.
                        </p>
                        <button
                            type="button"
                            onClick={() => navigate('/sessions')}
                            className="mt-4 rounded-md border border-border px-3 py-1.5 text-sm font-medium text-foreground"
                        >
                            Open sessions page
                        </button>
                    </section>
                </div>
            </div>
        </div>
    );
};

export default ProfilePage;





