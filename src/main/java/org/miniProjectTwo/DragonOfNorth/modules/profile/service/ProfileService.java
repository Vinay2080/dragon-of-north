package org.miniProjectTwo.DragonOfNorth.modules.profile.service;

import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Service interface for managing user profiles, including creation, updates, and avatar synchronization.
 * <p>
 * Handles profile lifecycle operations such as initial creation from OAuth data, ensuring existence on login,
 * updating profile details, and managing profile images. Integrates with external services for avatar syncing
 * and image storage as needed.
 */
public interface ProfileService {

    /**
     * Creates a new user profile based on OAuth user information. This method is typically called during
     * the first login of an OAuth-authenticated user to initialize their profile with data from the OAuth provider.
     *
     * @param userId   The unique identifier of the user for whom the profile is being created.
     * @param userInfo The OAuth user information containing details such as display name, email, and avatar URL.
     */
    void createProfile(UUID userId, OAuthUserInfo userInfo);

    /**
     * Ensures that a user profile exists for the given user ID. If a profile does not exist, it creates one using the provided OAuth user information. This method is typically called during the login process to guarantee that every authenticated user has an associated profile.
     *
     * @param userId   The unique identifier of the user for whom to ensure profile existence.
     * @param userInfo The OAuth user information containing details such as display name, email, and avatar URL, which can be used to create the profile if it does not already exist.
     */
    void ensureProfileExists(UUID userId, OAuthUserInfo userInfo);

    /**
     * Synchronizes the user's profile avatar with their Google avatar.
     *
     * @param userId   The unique identifier of the user for whom to sync the avatar.
     * @param userInfo The OAuth user information containing the Google avatar URL.
     */
    void syncGoogleAvatar(UUID userId, OAuthUserInfo userInfo);

    /**
     * Updates the user's profile with the provided details. This method allows users to modify their profile information such as bio, avatar URL, display name, and username.
     *
     * @param bio         The new biography or description for the user's profile.
     * @param avatarUrl   The new URL for the user's avatar image.
     * @param displayName The new display name for the user's profile.
     * @param username    The new username for the user's profile.
     * @return The updated Profile object reflecting the changes made to the user's profile.
     */
    Profile updateProfile(String bio, String avatarUrl, String displayName, String username);

    /**
     * Retrieves the current user's profile information.
     *
     * @return The Profile object containing the user's profile details.
     */
    Profile getProfile();

    /**
     * Updates the user's profile image with the provided multipart file. This method handles the process of uploading the new profile image, updating the user's profile with the new image URL, and returning the updated profile information.
     *
     * @param userId        The unique identifier of the user whose profile image is being updated.
     * @param multipartFile The multipart file containing the new profile image to be uploaded.
     * @return The updated Profile object reflecting the new profile image.
     */
    Profile updateProfileImage(UUID userId, MultipartFile multipartFile);

    /**
     * Deletes the user's profile image. This method removes the current profile image associated with the user's profile and updates the profile accordingly.
     *
     * @param userId The unique identifier of the user whose profile image is being deleted.
     */
    void deleteProfileImage(UUID userId);
}
