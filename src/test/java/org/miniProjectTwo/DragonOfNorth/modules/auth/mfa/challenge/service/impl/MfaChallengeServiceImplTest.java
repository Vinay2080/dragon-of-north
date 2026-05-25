package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.service.impl;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.config.MfaChallengeProperties;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.context.ChallengeRequestBinding;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.ChallengeState;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.VerificationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.provider.MfaChallengeProviderVerifier;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis.ChallengeStateAtomicRedisOps;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis.ChallengeStateRedisStore;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.token.MfaChallengeTokenGenerator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.TokenHasher;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MfaChallengeServiceImplTest {

    @Test
    void createChallenge_shouldPersistStateAndReturnChallenge() {
        MfaChallengeTokenGenerator tokenGenerator = () -> "token-1";
        ChallengeStateRedisStore store = mock(ChallengeStateRedisStore.class);
        MfaChallengeProperties properties = new MfaChallengeProperties();
        properties.setTtl(Duration.ofMinutes(5));

        MfaChallengeServiceImpl service = new MfaChallengeServiceImpl(
                tokenGenerator,
                store,
                mock(ChallengeStateAtomicRedisOps.class),
                new ChallengeRequestBinding(new TokenHasher()),
                mock(org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.provider.MfaChallengeProviderVerifier.class),
                properties,
                mock(AuditEventLogger.class)
        );

        var challenge = service.createChallenge(UUID.randomUUID(), "pwd", new AuthRequestContext("device-1", "203.0.113.42", "req-1", "Mozilla/5.0"), List.of(ProviderType.TOTP));
        assertEquals("token-1", challenge.mfaToken());

        var captor = org.mockito.ArgumentCaptor.forClass(ChallengeState.class);
        verify(store).save(eq("token-1"), captor.capture());
        assertEquals("203.0.113", captor.getValue().ipPrefix());
        assertEquals(List.of(ProviderType.TOTP), captor.getValue().allowedProviders());
    }

    @Test
    void peek_shouldDelegateToStore() {
        ChallengeStateRedisStore store = mock(ChallengeStateRedisStore.class);
        ChallengeState state = new ChallengeState(UUID.randomUUID(), "pwd", null, null, null, List.of(ProviderType.TOTP), 0, null, null);
        when(store.find("token-1")).thenReturn(Optional.of(state));

        MfaChallengeServiceImpl service = new MfaChallengeServiceImpl(
                mock(MfaChallengeTokenGenerator.class), store, mock(ChallengeStateAtomicRedisOps.class),
                new ChallengeRequestBinding(new TokenHasher()), mock(org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.provider.MfaChallengeProviderVerifier.class),
                new MfaChallengeProperties(), mock(AuditEventLogger.class));

        assertTrue(service.peek("token-1").isPresent());
    }

    @Test
    void verifyAndConsume_shouldRejectProviderNotBoundToChallenge() {
        ChallengeStateRedisStore store = mock(ChallengeStateRedisStore.class);
        ChallengeStateAtomicRedisOps atomicOps = mock(ChallengeStateAtomicRedisOps.class);
        MfaChallengeProviderVerifier providerVerifier = mock(MfaChallengeProviderVerifier.class);
        MfaChallengeProperties properties = new MfaChallengeProperties();
        properties.setMaxAttempts(3);
        properties.setLockoutTtl(Duration.ofMinutes(5));
        MfaChallengeServiceImpl service = new MfaChallengeServiceImpl(
                mock(MfaChallengeTokenGenerator.class), store, atomicOps,
                new ChallengeRequestBinding(new TokenHasher()), providerVerifier, properties, mock(AuditEventLogger.class));

        AuthRequestContext context = new AuthRequestContext("device-1", "203.0.113.42", "req-1", "Mozilla/5.0");
        ChallengeState state = new ChallengeState(
                UUID.randomUUID(),
                "pwd",
                "device-1",
                "203.0.113",
                new TokenHasher().hashToken("Mozilla/5.0"),
                List.of(ProviderType.TOTP),
                0,
                null,
                null
        );
        when(atomicOps.claim(anyString(), anyString(), any())).thenReturn(new ChallengeStateAtomicRedisOps.ClaimResult(ChallengeStateAtomicRedisOps.ClaimStatus.OK, "json"));
        when(store.decode("json")).thenReturn(state);
        when(atomicOps.recordFailure(anyString(), anyString(), anyInt(), any())).thenReturn(new ChallengeStateAtomicRedisOps.FailResult(ChallengeStateAtomicRedisOps.FailStatus.FAILED, 1, state.userId().toString()));

        VerificationResult result = service.verifyAndConsume("t-1", ProviderType.RECOVERY_CODE, "111111", context);

        assertEquals(VerificationResult.FailureReason.PROVIDER_MISMATCH, result.failureReason());
        verify(providerVerifier, never()).verify(any(), any(), anyString());
    }
}
