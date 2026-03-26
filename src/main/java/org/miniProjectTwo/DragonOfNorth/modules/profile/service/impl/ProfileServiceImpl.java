package org.miniProjectTwo.DragonOfNorth.modules.profile.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
    public void createProfile(AppUser appUser, OAuthUserInfo userInfo) {
        if (profileRepository.existsProfileByAppUser(appUser)) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_EXISTS, "Profile already exists for user: " + appUser.getEmail());
        }
        Profile profile = new Profile();
        profile.setAppUser(appUser);
        if (userInfo != null) {
            profile.setDisplayName(userInfo.name());
            profile.setAvatarUrl(userInfo.picture());
            profile.setUsername(generateUniqueUsername(userInfo.name()));
        } else {
            profile.setDisplayName(appUser.getEmail());
            profile.setUsername(generateUniqueUsername(appUser.getEmail()));
        }
        profileRepository.save(profile);
    }

    private String generateUniqueUsername(String name) {
        String base = name.toLowerCase()
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

    private boolean usernameExists(String uniqueUsername) {
        return profileRepository.existsByUsernameIgnoreCase(uniqueUsername);
    }

    @Override
    @Transactional
    public void updateProfile(String bio, String avatarUrl, String displayName, String username) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "User must be authenticated to update profile");
        }

        UUID userId = resolveUserId(authentication);
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Authenticated user not found"));
        Profile profile = getOrCreateProfile(appUser);

        if (username != null && !username.equalsIgnoreCase(profile.getUsername())) {
            if (profileRepository.existsByUsernameIgnoreCase(username)) {
                throw new BusinessException(ErrorCode.USERNAME_ALREADY_TAKEN, "Username is already taken: " + username);
            }
            profile.setUsername(username);
        }

        updateIfNotNull(bio, profile::setBio);
        updateIfNotNull(avatarUrl, profile::setAvatarUrl);
        updateIfNotNull(displayName, profile::setDisplayName);

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

    private Profile getOrCreateProfile(AppUser appUser) {
        return profileRepository.findByAppUser(appUser).orElseGet(() -> {
            Profile newProfile = new Profile();
            newProfile.setAppUser(appUser);
            newProfile.setUsername(generateUniqueUsername(appUser.getEmail()));
            newProfile.setDisplayName(appUser.getEmail());
            return profileRepository.save(newProfile);
        });
    }

    private void updateIfNotNull(String value, Consumer<String> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
