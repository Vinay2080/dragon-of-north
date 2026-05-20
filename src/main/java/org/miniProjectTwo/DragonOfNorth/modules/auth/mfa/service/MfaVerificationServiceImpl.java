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

    @Override
    public VerifyResult verifyAtLogin(AppUser user, ProviderType providerType, String code, MfaContext context) {
        MfaProvider provider = mfaProviderRegistry.getProvider(providerType);
        if (!provider.allowsLoginChallenge()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "MFA provider does not allow login challenge verification");
        }

        if (!provider.isEnabledFor(user)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Requested MFA provider is not enabled for this user");
        }

        return provider.verify(user, code, safeContext(context));
    }

    @Override
    public VerifyResult verifyForStepUp(AppUser user, ProviderType providerType, String code, MfaContext context) {
        MfaProvider provider = mfaProviderRegistry.getProvider(providerType);
        if (!provider.allowsStepUp()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "MFA provider does not allow step-up verification");
        }

        if (!provider.isEnabledFor(user)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Requested MFA provider is not enabled for this user");
        }

        return provider.verify(user, code, safeContext(context));
    }

    private MfaContext safeContext(MfaContext context) {
        return context == null ? MfaContext.empty() : context;
    }
}
