package org.miniProjectTwo.DragonOfNorth.modules.profile.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.AvatarSource;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.modules.profile.repo.ProfileRepository;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Profile service implementation coordinating profile persistence with avatar/media workflows.
 */
@Service
@RequiredArgsConstructor
@Slf4j

public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final AppUserRepository appUserRepository;
    private final UserStateValidator userStateValidator;


    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp");
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String PROFILE_IMAGE_FOLDER = "profile_images";
    private final Cloudinary cloudinary;

    /**
     * Validates the original filename for potential security risks.
     * <p>
     * The method checks for null values and disallows filenames containing path traversal characters,
     * quotes, or sequences that could be used to manipulate file paths. This helps prevent
     * directory traversal attacks and other file-related vulnerabilities.
     *
     * @param originalFilename the original filename to validate
     * @throws BusinessException if the filename is deemed unsafe
     */
    private void validateFilename(String originalFilename) {
        if (originalFilename == null) {
            return;
        }
        String normalizedFilename = originalFilename.trim();
        if (normalizedFilename.contains("\\") || normalizedFilename.contains("/")
                || normalizedFilename.contains("\"") || normalizedFilename.contains("..")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid file format: unsupported filename");
        }
    }

    /**
     * Validates the content type of the uploaded file against allowed image formats.
     *
     * @param contentType the MIME type of the uploaded file
     * @throws BusinessException if the content type is not an allowed image format
     */
    private void validateContentType(String contentType) {
        String normalizedContentType = contentType == null ? "" : contentType.trim().toLowerCase().split(";")[0];
        if (!ALLOWED_IMAGE_TYPES.contains(normalizedContentType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Unsupported file type: " + contentType);
        }
    }    /**
     * Creates a new profile for the specified user.
     * <p>
     * The profile is initialized using OAuth user information when available.
     * If OAuth information is not provided, the user's email is used as the
     * default display name and username source.
     *
     * @param userId   unique identifier of the user
     * @param userInfo OAuth profile information used for initial profile setup
     * @throws BusinessException if the user does not exist or a profile already exists
     */

    @Override
    public void createProfile(UUID userId, OAuthUserInfo userInfo) {
        AppUser appUser = fetchAppUser(userId);
        ensureProfileDoesNotExist(userId, appUser);

        Profile profile = new Profile();
        profile.setAppUser(appUser);
        applyInitialProfileData(profile, appUser, userInfo);
        profileRepository.save(profile);
    }

    /**
     * Applies a user-defined avatar URL to the profile, replacing any existing avatar.
     * <p>
     * If the provided URL is null or empty after normalization, the profile's avatar
     * fields are cleared and any existing Cloudinary-managed image is deleted.
     *
     * @param profile      profile to update
     * @param rawAvatarUrl new avatar URL provided by the user
     */
    private void applyUserAvatarUpdate(Profile profile, String rawAvatarUrl) {
        String normalized = normalizeAvatarUrl(rawAvatarUrl);
        if (normalized == null) {
            profile.setAvatarUrl(null);
            profile.setAvatarExternalUrl(null);
            profile.setAvatarSource(AvatarSource.NONE);
            deleteExistingProfileImage(profile);
            return;
        }

        if (normalized.equals(profile.getAvatarUrl())) {
            return;
        }

        deleteExistingProfileImage(profile);
        profile.setAvatarPublicId(null);

        profile.setAvatarUrl(normalized);
        profile.setAvatarExternalUrl(null);
        profile.setAvatarSource(AvatarSource.USER_DEFINED);
    }

    /**
     * Ensures that a profile exists for the specified user.
     * If no profile is found, a new profile is automatically created.
     *
     * @param userId   unique identifier of the user
     * @param userInfo OAuth information used when profile creation is required
     */
    @Override
    public void ensureProfileExists(UUID userId, OAuthUserInfo userInfo) {
        if (!profileRepository.existsByAppUserId(userId)) {
            createProfile(userId, userInfo);
        }
    }



    /**
     * Synchronizes the user's Google avatar with the local profile.
     * <p>
     * Synchronization only occurs when the profile is not currently using
     * a user-defined avatar and a valid Google avatar URL is available.
     *
     * @param userId   unique identifier of the user
     * @param userInfo OAuth information containing the Google profile picture
     */
    @Override
    @Transactional
    public void syncGoogleAvatar(UUID userId, OAuthUserInfo userInfo) {
        String googleAvatar = normalizeGoogleAvatar(userInfo);
        if (googleAvatar == null) {
            return;
        }

        Profile profile = getOrCreateProfile(userId);
        if (!shouldSyncGoogleAvatar(profile)) {
            return;
        }

        applyGoogleAvatar(profile, googleAvatar);
    }

    /**
     * Updates editable profile information for the currently authenticated user.
     *
     * @param bio         new biography text, or {@code null} to leave unchanged
     * @param avatarUrl   new avatar URL, or {@code null} to leave unchanged
     * @param displayName new display name, or {@code null} to leave unchanged
     * @param username    new username, or {@code null} to leave unchanged
     * @return the updated profile entity
     * @throws BusinessException if authentication fails or the username is already taken
     */
    @Override
    @Transactional
    public Profile updateProfile(String bio, String avatarUrl, String displayName, String username) {
        AppUser appUser = findAuthenticatedUser(UserLifecycleOperation.PROFILE_UPDATE);
        Profile profile = getOrCreateProfile(appUser.getId());

        updateUsernameIfNeeded(profile, username);
        updateIfNotNull(bio, profile::setBio);
        applyAvatarUrlIfProvided(profile, avatarUrl);
        updateIfNotNull(displayName, profile::setDisplayName);
        return profile;
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @return authenticated user's profile
     * @throws BusinessException if authentication fails
     */
    @Override
    public Profile getProfile() {
        return getOrCreateProfile(findAuthenticatedUser(UserLifecycleOperation.PROFILE_READ).getId());
    }

    /**
     * Uploads and assigns a new profile image.
     * <p>
     * Existing Cloudinary-managed images are removed before the new image
     * is stored. Only JPEG, PNG, and WebP images up to 2 MB are accepted.
     *
     * @param userId        unique identifier of the user
     * @param multipartFile uploaded image file
     * @return updated profile entity
     * @throws BusinessException if validation or upload fails
     */
    @Override
    @Transactional
    public Profile updateProfileImage(UUID userId, MultipartFile multipartFile) {
        validateImageFile(multipartFile);

        AppUser appUser = fetchAppUser(userId);
        userStateValidator.validate(appUser, UserLifecycleOperation.PROFILE_UPDATE);

        Profile profile = getOrCreateProfile(userId);
        deleteExistingProfileImage(profile);

        Map<String, Object> uploadResult = uploadToCloudinary(multipartFile);
        String imageUrl = extractImageUrl(uploadResult);
        String publicId = (String) uploadResult.get("public_id");

        if (imageUrl == null || publicId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Failed to upload image to Cloudinary");
        }

        applyUploadedAvatar(profile, imageUrl, publicId);
        return profileRepository.save(profile);
    }

    /**
     * Removes the current profile image.
     * <p>
     * If the image is managed by Cloudinary, the remote asset is deleted
     * before profile metadata is cleared.
     *
     * @param userId unique identifier of the user
     */
    @Override
    @Transactional
    public void deleteProfileImage(UUID userId) {
        profileRepository.findByAppUserId(userId).ifPresent(profile -> {
            deleteExistingProfileImage(profile);
            profile.setAvatarUrl(null);
            profile.setAvatarExternalUrl(null);
            profile.setAvatarSource(AvatarSource.NONE);
            profileRepository.save(profile);
        });
    }

    private void validateImageFile(MultipartFile file) {
        validateFilePresence(file);
        validateFileSize(file);
        validateFilename(file.getOriginalFilename());
        validateContentType(file.getContentType());
    }

    private void applyUploadedAvatar(Profile profile, String imageUrl, String publicId) {
        profile.setAvatarPublicId(publicId);
        profile.setAvatarUrl(imageUrl);
        profile.setAvatarExternalUrl(null);
        profile.setAvatarSource(AvatarSource.USER_DEFINED);
    }

    private void validateFilePresence(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "No file uploaded");
        }
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "File size exceeds limit of 2MB");
        }
    }


    /**
     * Finds the authenticated user based on the current security context.
     *
     * @param operation the user lifecycle operation being performed
     * @return the authenticated user
     * @throws BusinessException if the user is not authenticated
     */
    private AppUser findAuthenticatedUser(UserLifecycleOperation operation) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "User must be authenticated");
        }

        UUID userId = resolveUserId(authentication);
        AppUser appUser = fetchAppUser(userId);
        userStateValidator.validate(appUser, operation);
        return appUser;
    }

    /**
     * Updates the profile's username if a new value is provided and different from the current one.
     * <p>
     * The method checks for null or unchanged values to avoid unnecessary updates. If a new username
     * is provided, it validates that the username is not already taken by another profile before applying the change.
     *
     * @param profile  the profile to update
     * @param username the new username to set, or null to leave unchanged
     * @throws BusinessException if the new username is already taken by another profile
     */
    private void updateUsernameIfNeeded(Profile profile, String username) {
        if (username == null || username.equalsIgnoreCase(profile.getUsername())) {
            return;
        }

        if (profileRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_TAKEN, "Username is already taken: " + username);
        }
        profile.setUsername(username);
    }

    private void applyAvatarUrlIfProvided(Profile profile, String avatarUrl) {
        if (avatarUrl == null) {
            return;
        }
        applyUserAvatarUpdate(profile, avatarUrl);
    }

    /**
     * Returns an existing profile or creates a minimal profile when one does not exist.
     *
     * @param userId user identifier
     * @return existing or newly created profile
     */
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

    /**
     * Generates a unique username based on the provided name.
     * <p>
     * The method sanitizes the input name, creates a base username, and appends a random suffix.
     * It checks for uniqueness against existing usernames and retries with different suffixes if needed.
     *
     * @param name the input name to generate a username from
     * @return a unique username derived from the input name
     */
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

    /**
     * Resolves the user ID from the authentication principal, supporting multiple principal types for flexibility.
     *
     * @param authentication the current authentication object containing the principal
     * @return the resolved user ID
     * @throws BusinessException if the principal type is unsupported or does not contain a valid user ID
     */
    private UUID resolveUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UUID userId) {
            return userId;
        }

        if (principal instanceof SecurityPrincipal securityPrincipal && securityPrincipal.userId() != null) {
            return securityPrincipal.userId();
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

    /**
     * Normalizes the Google avatar URL from the OAuth user information.
     * <p>
     * This method trims whitespace and returns null for empty or null URLs.
     *
     * @param userInfo OAuth user information containing the Google profile picture URL
     * @return normalized avatar URL, or null if not available
     */
    private String normalizeGoogleAvatar(OAuthUserInfo userInfo) {
        if (userInfo == null) {
            return null;
        }
        return normalizeAvatarUrl(userInfo.picture());
    }

    /**
     * Determines whether the Google avatar should be synchronized based on the profile's current avatar source and external URL.
     *
     * @param profile the profile to evaluate
     * @return true if synchronization should occur, false otherwise
     */
    private boolean shouldSyncGoogleAvatar(Profile profile) {
        AvatarSource source = profile.getAvatarSource() == null ? AvatarSource.NONE : profile.getAvatarSource();
        if (source == AvatarSource.USER_DEFINED) {
            return false;
        }

        return !(source == AvatarSource.GOOGLE && profile.getAvatarExternalUrl() != null);
    }

    /**
     * Fetches the AppUser entity by ID, throwing a BusinessException if not found.
     *
     * @param userId unique identifier of the user
     * @return the corresponding AppUser entity
     * @throws BusinessException if no user is found with the given ID
     */
    private AppUser fetchAppUser(UUID userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Validates that a profile does not already exist for the given user ID.
     *
     * @param userId  unique identifier of the user
     * @param appUser associated user entity for error context
     * @throws BusinessException if a profile already exists for the user
     */
    private void ensureProfileDoesNotExist(UUID userId, AppUser appUser) {
        if (profileRepository.existsByAppUserId(userId)) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_EXISTS, "Profile already exists for user: " + appUser.getEmail());
        }
    }

    /**
     * Initializes profile fields based on available OAuth user information or defaults to email-based values.
     *
     * @param profile  profile entity to initialize
     * @param appUser  associated user entity
     * @param userInfo OAuth information containing potential display name and avatar URL
     */
    private void applyInitialProfileData(Profile profile, AppUser appUser, OAuthUserInfo userInfo) {
        if (userInfo != null) {
            profile.setDisplayName(userInfo.name());
            applyGoogleAvatar(profile, userInfo.picture());
            profile.setUsername(generateUniqueUsername(userInfo.name()));
            return;
        }

        profile.setDisplayName(appUser.getEmail());
        profile.setUsername(generateUniqueUsername(appUser.getEmail()));
    }

    /**
     * Uploads an image to Cloudinary and returns the upload response.
     *
     * @param file image file to upload
     * @return Cloudinary upload metadata
     * @throws BusinessException when upload fails or the image format is invalid
     */
    private Map<String, Object> uploadToCloudinary(MultipartFile file) {
        try {
            Uploader uploader = cloudinary.uploader();
            Map<?, ?> rawResult = uploader.upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", PROFILE_IMAGE_FOLDER
            ));
            return toTypedUploadResult(rawResult);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Failed to upload image: " + exception.getMessage());
        } catch (RuntimeException exception) {
            String detail = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
            if (detail.contains("invalid image")) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "Invalid image format. Allowed formats: image/jpeg, image/png, image/webp");
            }
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid image upload payload");
        }
    }

    /**
     * Converts the raw Cloudinary upload result to a typed map with string keys.
     *
     * @param rawResult the raw upload response from Cloudinary
     * @return a typed map containing the upload metadata
     */
    private Map<String, Object> toTypedUploadResult(Map<?, ?> rawResult) {
        Map<String, Object> typedResult = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawResult.entrySet()) {
            if (entry.getKey() instanceof String key) {
                typedResult.put(key, entry.getValue());
            }
        }
        return typedResult;
    }

    /**
     * Extracts the image URL from the Cloudinary upload result, preferring the secure URL when available.
     *
     * @param uploadResult the raw upload response from Cloudinary
     * @return the extracted image URL, or null if not found
     */
    private String extractImageUrl(Map<String, Object> uploadResult) {
        Object secureUrl = uploadResult.get("secure_url");
        Object url = uploadResult.get("url");
        return secureUrl != null ? secureUrl.toString() : (url != null ? url.toString() : null);
    }

    /**
     * Normalizes the provided avatar URL by trimming whitespace and checking for null or empty values.
     *
     * @param avatarUrl the avatar URL to normalize
     * @return the normalized avatar URL, or null if it is null or empty
     */
    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl == null) {
            return null;
        }
        String normalized = avatarUrl.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Applies a Google avatar URL to the profile, replacing any existing avatar.
     * <p>
     * If the provided URL is null or empty after normalization, the profile's avatar
     * fields are cleared and any existing Cloudinary-managed image is deleted.
     *
     * @param profile            profile to update
     * @param rawGoogleAvatarUrl new Google avatar URL provided by the user
     */
    private void applyGoogleAvatar(Profile profile, String rawGoogleAvatarUrl) {
        String googleAvatar = normalizeAvatarUrl(rawGoogleAvatarUrl);
        if (googleAvatar == null) {
            return;
        }
        deleteExistingProfileImage(profile);
        profile.setAvatarUrl(googleAvatar);
        profile.setAvatarExternalUrl(googleAvatar);
        profile.setAvatarSource(AvatarSource.GOOGLE);
        profile.setAvatarPublicId(null);
    }

    /**
     * Deletes the currently stored Cloudinary profile image, if one exists.
     * Any cleanup failures are logged and do not interrupt profile operations.
     *
     * @param profile profile whose image should be removed
     */
    private void deleteExistingProfileImage(Profile profile) {
        String existingPublicId = profile.getAvatarPublicId();
        if (existingPublicId == null || existingPublicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(profile.getAvatarPublicId(), ObjectUtils.emptyMap());
        } catch (IOException exception) {
            log.error("Failed to delete profile image from Cloudinary (public_id={})", existingPublicId, exception);
        } finally {
            profile.setAvatarPublicId(null);
        }
    }


}
