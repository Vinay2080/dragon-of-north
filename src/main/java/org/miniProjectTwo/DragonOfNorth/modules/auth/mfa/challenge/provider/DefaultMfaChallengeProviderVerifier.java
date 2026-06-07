package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.provider;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.VerifyResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.service.MfaVerificationService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default implementation of {@link MfaChallengeProviderVerifier} that uses {@link MfaVerificationService} to verify MFA codes for a given user and provider type.
 * <p>
 * This component is responsible for orchestrating the verification of MFA codes during the login process. It retrieves the user by ID, invokes the appropriate verification logic based on the provider type, and normalizes failure reasons into standardized error messages for consistent client responses. The verification result is encapsulated in a {@link ChallengeProviderVerification} object, indicating success or failure along with relevant error details.
 *
 */
@Component
@RequiredArgsConstructor
public class DefaultMfaChallengeProviderVerifier implements MfaChallengeProviderVerifier {
    private final AppUserRepository appUserRepository;
    private final MfaVerificationService mfaVerificationService;

    /**
     * Verifies the provided MFA code for the specified user and provider type.
     *
     * @param userId       The ID of the user attempting MFA verification.
     * @param providerType The type of MFA provider being verified (e.g., TOTP, SMS).
     * @param code         The MFA code submitted by the user for verification.
     * @return A {@link ChallengeProviderVerification} object indicating whether the verification was successful or failed, along with any relevant error messages.
     */
    @Override
    public ChallengeProviderVerification verify(UUID userId, ProviderType providerType, String code) {
        AppUser user = appUserRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            return ChallengeProviderVerification.failure("user_not_found");
        }

        try {
            VerifyResult result = mfaVerificationService.verifyAtLogin(user, providerType, code, null);
            if (!result.success()) {
                return ChallengeProviderVerification.failure(normalizeProviderFailure(result.failureReason()));
            }
            return ChallengeProviderVerification.verified();
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.INVALID_INPUT) {
                return ChallengeProviderVerification.failure("invalid_provider");
            }
            if (ex.getErrorCode() == ErrorCode.ACCESS_DENIED) {
                return ChallengeProviderVerification.failure("provider_not_enabled");
            }
            return ChallengeProviderVerification.failure("provider_verification_failed");
        }
    }

    private String normalizeProviderFailure(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return "invalid_code";
        }
        return failureReason.trim();
    }
}
