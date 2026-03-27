package org.miniProjectTwo.DragonOfNorth.modules.profile.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.AvatarSource;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.modules.profile.repo.ProfileRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService {

    private final ProfileRepository profileRepository;
    private final AppUserRepository appUserRepository;

    @Override
    public void createProfile(UUID userId, OAuthUserInfo userInfo) {
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (profileRepository.existsByAppUserId(userId)) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_EXISTS, "Profile already exists for user: " + appUser.getEmail());
        }
        Profile profile = new Profile();
        profile.setAppUser(appUser);
        if (userInfo != null) {
            profile.setDisplayName(userInfo.name());
            applyGoogleAvatar(profile, userInfo.picture());
            profile.setUsername(generateUniqueUsername(userInfo.name()));
        } else {
            profile.setDisplayName(appUser.getEmail());
            profile.setUsername(generateUniqueUsername(appUser.getEmail()));
        }
        profileRepository.save(profile);
    }

    @Override
    @Transactional
    public void syncGoogleAvatar(UUID userId, OAuthUserInfo userInfo) {
        if (userInfo == null) {
            return;
        }

        String googleAvatar = normalizeAvatarUrl(userInfo.picture());
        if (googleAvatar == null) {
            return;
        }

        Profile profile = getOrCreateProfile(userId);
        AvatarSource source = profile.getAvatarSource() == null ? AvatarSource.NONE : profile.getAvatarSource();

        if (source == AvatarSource.USER_DEFINED) {
            return;
        }

        boolean alreadySynced = source == AvatarSource.GOOGLE
                && googleAvatar.equals(profile.getAvatarUrl())
                && googleAvatar.equals(profile.getAvatarExternalUrl());

        if (alreadySynced) {
            return;
        }

        applyGoogleAvatar(profile, googleAvatar);
    }

    @Override
    @Transactional
    public void updateProfile(String bio, String avatarUrl, String displayName, String username) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "User must be authenticated to update profile");
        }

        UUID userId = resolveUserId(authentication);
        Profile profile = getOrCreateProfile(userId);

        if (username != null && !username.equalsIgnoreCase(profile.getUsername())) {
            if (profileRepository.existsByUsernameIgnoreCase(username)) {
                throw new BusinessException(ErrorCode.USERNAME_ALREADY_TAKEN, "Username is already taken: " + username);
            }
            profile.setUsername(username);
        }

        updateIfNotNull(bio, profile::setBio);
        if (avatarUrl != null) {
            applyUserAvatarUpdate(profile, avatarUrl);
        }
        updateIfNotNull(displayName, profile::setDisplayName);

    }

    @Override
    public Profile getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "User must be authenticated to view profile");
        }

        UUID userId = resolveUserId(authentication);
        return getOrCreateProfile(userId);
    }

    private Profile getOrCreateProfile(UUID userId) {
        return profileRepository.findByAppUserId(userId).orElseGet(() -> {
            String identifier = appUserRepository.findPreferredIdentifierById(userId)
                    .orElse("user_" + userId.toString().substring(0, 8));

            Profile newProfile = new Profile();
            newProfile.setAppUser(appUserRepository.getReferenceById(userId));
            newProfile.setUsername(generateUniqueUsername(identifier));
            newProfile.setDisplayName(identifier);
            return profileRepository.save(newProfile);
        });
    }

    private String generateUniqueUsername(String name) {
        String sanitizedName = name == null ? "" : name;
        String base = sanitizedName.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();

        if (base.isBlank()) base = "user";

        String username;
        int attempts = 0;

        do {
            int rand = (int) (Math.random() * 10000);
            username = base + "_" + rand;
            attempts++;
        } while (usernameExists(username) && attempts < 5);

        if (usernameExists(username)) {
            username = base + "_" + System.currentTimeMillis();
        }

        return username;
    }

    private UUID resolveUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UUID userId) {
            return userId;
        }

        if (principal instanceof AppUser appUser && appUser.getId() != null) {
            return appUser.getId();
        }

        if (principal instanceof AppUserDetails appUserDetails && appUserDetails.getAppUser().getId() != null) {
            return appUserDetails.getAppUser().getId();
        }

        if (principal instanceof String raw && !raw.isBlank() && !"anonymousUser".equals(raw)) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                // Let the common unauthorized error path handle unsupported principal values.
            }
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED, "Unsupported authentication principal");
    }

    private boolean usernameExists(String uniqueUsername) {
        return profileRepository.existsByUsernameIgnoreCase(uniqueUsername);
    }

    private void updateIfNotNull(String value, Consumer<String> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void applyUserAvatarUpdate(Profile profile, String rawAvatarUrl) {
        String normalized = normalizeAvatarUrl(rawAvatarUrl);
        if (normalized == null) {
            profile.setAvatarUrl(null);
            profile.setAvatarExternalUrl(null);
            profile.setAvatarSource(AvatarSource.NONE);
            return;
        }

        profile.setAvatarUrl(normalized);
        profile.setAvatarExternalUrl(null);
        profile.setAvatarSource(AvatarSource.USER_DEFINED);
    }

    private void applyGoogleAvatar(Profile profile, String rawGoogleAvatarUrl) {
        String googleAvatar = normalizeAvatarUrl(rawGoogleAvatarUrl);
        if (googleAvatar == null) {
            return;
        }
        profile.setAvatarUrl(googleAvatar);
        profile.setAvatarExternalUrl(googleAvatar);
        profile.setAvatarSource(AvatarSource.GOOGLE);
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl == null) {
            return null;
        }
        String normalized = avatarUrl.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
