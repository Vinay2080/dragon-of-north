package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.service;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.MfaContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.VerifyResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.provider.MfaProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.registry.MfaProviderRegistry;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MfaVerificationServiceImpl implements MfaVerificationService {
    private final MfaProviderRegistry mfaProviderRegistry;

    /**
     * Verifies the provided MFA code for login challenge scenarios. This method checks if the specified MFA provider allows login challenge verification and if it is enabled for the user. If the provider is valid, it delegates the verification process to the provider's verify method, passing in the user, code, and context. The result of the verification is returned as a VerifyResult object, indicating success or failure along with any relevant failure reasons.
     *
     * @param user         The user attempting MFA verification during login.
     * @param providerType The type of MFA provider being verified (e.g., TOTP, SMS).
     * @param code         The MFA code submitted by the user for verification.
     * @param context      Additional context for the MFA verification process, which may include information about the authentication session or request.
     * @return A VerifyResult object indicating whether the login challenge verification was successful or failed, along with any relevant failure reasons.
     * @throws BusinessException If the specified MFA provider does not allow login challenge verification or is not enabled for the user.
     */
    @Override
    public VerifyResult verifyAtLogin(AppUser user, ProviderType providerType, String code, MfaContext context) {
        MfaProvider provider = mfaProviderRegistry.getProvider(providerType);
        if (!provider.allowsLoginChallenge()) {
            throw new BusinessException(ErrorCode.MFA_PROVIDER_NOT_ENABLED, "MFA provider does not allow login challenge verification");
        }

        if (!provider.isEnabledFor(user)) {
            throw new BusinessException(ErrorCode.MFA_PROVIDER_NOT_ENABLED, "Requested MFA provider is not enabled for this user");
        }

        return provider.verify(user, code, safeContext(context));
    }

    /**
     * Verifies the provided MFA code for step-up authentication scenarios. This method checks if the specified MFA provider allows step-up verification and if it is enabled for the user. If the provider is valid, it delegates the verification process to the provider's verify method, passing in the user, code, and context. The result of the verification is returned as a VerifyResult object, indicating success or failure along with any relevant failure reasons.
     *
     * @param user         The user attempting step-up verification.
     * @param providerType The type of MFA provider being verified (e.g., TOTP, SMS).
     * @param code         The MFA code submitted by the user for verification.
     * @param context      Additional context for the MFA verification process, which may include information about the authentication session or request.
     * @return A VerifyResult object indicating whether the step-up verification was successful or failed, along with any relevant failure reasons.
     * @throws BusinessException If the specified MFA provider does not allow step-up verification or is not enabled for the user.
     */
    @Override
    public VerifyResult verifyForStepUp(AppUser user, ProviderType providerType, String code, MfaContext context) {
        MfaProvider provider = mfaProviderRegistry.getProvider(providerType);
        if (provider == null) {
            throw new BusinessException(ErrorCode.MFA_INVALID_PROVIDER, "Invalid MFA provider type");
        }
        if (!provider.allowsStepUp()) {
            throw new BusinessException(ErrorCode.MFA_PROVIDER_NOT_ENABLED, "MFA provider does not allow step-up verification");
        }

        if (!provider.isEnabledFor(user)) {
            throw new BusinessException(ErrorCode.MFA_PROVIDER_NOT_ENABLED, "Requested MFA provider is not enabled for this user");
        }

        return provider.verify(user, code, safeContext(context));
    }

    private MfaContext safeContext(MfaContext context) {
        return context == null ? MfaContext.empty() : context;
    }
}
