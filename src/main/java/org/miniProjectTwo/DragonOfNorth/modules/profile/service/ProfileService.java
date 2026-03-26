package org.miniProjectTwo.DragonOfNorth.modules.profile.service;

import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;

import java.util.UUID;

public interface ProfileService {
    void createProfile(UUID userId, OAuthUserInfo userInfo);

    void updateProfile(String bio, String avatarUrl, String displayName, String username);

    Profile getProfile();

}
