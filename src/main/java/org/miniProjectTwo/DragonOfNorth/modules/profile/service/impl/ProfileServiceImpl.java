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
    public void ensureProfileExists(UUID userId, OAuthUserInfo userInfo) {
        if (!profileRepository.existsByAppUserId(userId)) {
            createProfile(userId, userInfo);
        }
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

        if (source == AvatarSource.GOOGLE && profile.getAvatarExternalUrl() != null) {
            return;
        }
        applyGoogleAvatar(profile, googleAvatar);
    }

    @Override
    @Transactional
    public Profile updateProfile(String bio, String avatarUrl, String displayName, String username) {
        AppUser appUser = findAuthenticatedUser(UserLifecycleOperation.PROFILE_UPDATE);
        UUID userId = appUser.getId();
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
        return profile;
    }

    @Override
    public Profile getProfile() {
        return getOrCreateProfile(findAuthenticatedUser(UserLifecycleOperation.PROFILE_READ).getId());
    }

    @Override
    @Transactional
    public Profile updateProfileImage(UUID userId, MultipartFile multipartFile) {
        validateImageFile(multipartFile);

        AppUser appUser = appUserRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userStateValidator.validate(appUser, UserLifecycleOperation.PROFILE_UPDATE);

        Profile profile = getOrCreateProfile(userId);
        deleteExistingProfileImage(profile);

        Map<String, Object> uploadResult = uploadToCloudinary(multipartFile);
        String imageUrl = extractImageUrl(uploadResult);
        String publicId = (String) uploadResult.get("public_id");

        if (imageUrl == null || publicId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Failed to upload image to Cloudinary");
        }

        profile.setAvatarPublicId(publicId);
        profile.setAvatarUrl(imageUrl);
        profile.setAvatarExternalUrl(null);
        profile.setAvatarSource(AvatarSource.USER_DEFINED);
        return profileRepository.save(profile);
    }

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

    private AppUser findAuthenticatedUser(UserLifecycleOperation operation) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "User must be authenticated");
        }

        UUID userId = resolveUserId(authentication);
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userStateValidator.validate(appUser, operation);
        return appUser;
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

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "No file uploaded");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "File size exceeds limit of 2MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String normalizedFilename = originalFilename.trim();
            if (normalizedFilename.contains("\\") || normalizedFilename.contains("/")
                    || normalizedFilename.contains("\"") || normalizedFilename.contains("..")) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Invalid file format: unsupported filename");
            }
        }

        String contentType = file.getContentType();
        String normalizedContentType = contentType == null ? "" : contentType.trim().toLowerCase().split(";")[0];
        if (normalizedContentType == null || !ALLOWED_IMAGE_TYPES.contains(normalizedContentType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Unsupported file type: " + contentType);
        }
    }

    //todo:uploader.uplaodisthrowingerror
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

    private Map<String, Object> toTypedUploadResult(Map<?, ?> rawResult) {
        Map<String, Object> typedResult = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawResult.entrySet()) {
            if (entry.getKey() instanceof String key) {
                typedResult.put(key, entry.getValue());
            }
        }
        return typedResult;
    }

    private String extractImageUrl(Map<String, Object> uploadResult) {
        Object secureUrl = uploadResult.get("secure_url");
        Object url = uploadResult.get("url");
        return secureUrl != null ? secureUrl.toString() : (url != null ? url.toString() : null);
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl == null) {
            return null;
        }
        String normalized = avatarUrl.trim();
        return normalized.isEmpty() ? null : normalized;
    }

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
}