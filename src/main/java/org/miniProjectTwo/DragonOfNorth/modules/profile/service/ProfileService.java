package org.miniProjectTwo.DragonOfNorth.modules.profile.service;

import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;

public interface ProfileService {
    void createProfile(AppUser appUser, OAuthUserInfo userInfo);

    void updateProfile(String bio, String avatarUrl, String displayName, String username);
}
