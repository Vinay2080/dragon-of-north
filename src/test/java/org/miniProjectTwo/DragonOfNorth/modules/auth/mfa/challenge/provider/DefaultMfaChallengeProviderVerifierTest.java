package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.provider;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.VerifyResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.service.MfaVerificationService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultMfaChallengeProviderVerifierTest {

    @Test
    void verify_shouldMapUnsupportedProvider() {
        AppUserRepository userRepository = mock(AppUserRepository.class);
        MfaVerificationService verificationService = mock(MfaVerificationService.class);
        DefaultMfaChallengeProviderVerifier verifier = new DefaultMfaChallengeProviderVerifier(userRepository, verificationService);

        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(AppUser.class)));
        when(verificationService.verifyAtLogin(any(), eq(ProviderType.TOTP), eq("123456"), isNull()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_INPUT));

        ChallengeProviderVerification result = verifier.verify(userId, ProviderType.TOTP, "123456");
        assertFalse(result.success());
        assertEquals("invalid_provider", result.failureReason());
    }

    @Test
    void verify_shouldMapInvalidCodeFailureReasonFromProvider() {
        AppUserRepository userRepository = mock(AppUserRepository.class);
        MfaVerificationService verificationService = mock(MfaVerificationService.class);
        DefaultMfaChallengeProviderVerifier verifier = new DefaultMfaChallengeProviderVerifier(userRepository, verificationService);

        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(AppUser.class)));
        when(verificationService.verifyAtLogin(any(), eq(ProviderType.TOTP), eq("654321"), isNull()))
                .thenReturn(VerifyResult.failure(ProviderType.TOTP, "invalid_code"));

        ChallengeProviderVerification result = verifier.verify(userId, ProviderType.TOTP, "654321");
        assertFalse(result.success());
        assertEquals("invalid_code", result.failureReason());
    }
}
