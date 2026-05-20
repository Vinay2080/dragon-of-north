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

@Component
@RequiredArgsConstructor
public class DefaultMfaChallengeProviderVerifier implements MfaChallengeProviderVerifier {
    private final AppUserRepository appUserRepository;
    private final MfaVerificationService mfaVerificationService;

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
