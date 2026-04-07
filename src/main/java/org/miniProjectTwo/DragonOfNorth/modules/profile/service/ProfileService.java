package org.miniProjectTwo.DragonOfNorth.modules.profile.service;

import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ProfileService {
    void createProfile(UUID userId, OAuthUserInfo userInfo);

    void ensureProfileExists(UUID userId, OAuthUserInfo userInfo);

    void syncGoogleAvatar(UUID userId, OAuthUserInfo userInfo);

    Profile updateProfile(String bio, String avatarUrl, String displayName, String username);

    Profile getProfile();

    Profile updateProfileImage(UUID userId, MultipartFile multipartFile);

    void deleteProfileImage(UUID userId);
}
