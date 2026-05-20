package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.service.impl;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.config.MfaChallengeProperties;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.ChallengeState;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis.ChallengeStateRedisStore;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.token.MfaChallengeTokenGenerator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.TokenHasher;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MfaChallengeServiceImplTest {

    @Test
    void createChallenge_shouldPersistStateAndReturnChallenge() {
        MfaChallengeTokenGenerator tokenGenerator = () -> "token-1";
        ChallengeStateRedisStore store = mock(ChallengeStateRedisStore.class);
        MfaChallengeProperties properties = new MfaChallengeProperties();
        properties.setTtl(Duration.ofMinutes(5));
        TokenHasher tokenHasher = new TokenHasher();
        AuditEventLogger auditEventLogger = mock(AuditEventLogger.class);

        MfaChallengeServiceImpl service = new MfaChallengeServiceImpl(
                tokenGenerator,
                store,
                properties,
                tokenHasher,
                auditEventLogger
        );

        UUID userId = UUID.randomUUID();
        AuthRequestContext context = new AuthRequestContext(
                "device-1",
                "203.0.113.42",
                "req-1",
                "Mozilla/5.0"
        );

        MfaChallenge challenge = service.createChallenge(
                userId,
                "pwd",
                context,
                List.of(ProviderType.TOTP)
        );

        assertEquals("token-1", challenge.mfaToken());
        assertEquals(List.of(ProviderType.TOTP), challenge.availableMethods());
        assertNotNull(challenge.expiresAt());

        var stateCaptor = org.mockito.ArgumentCaptor.forClass(ChallengeState.class);
        verify(store).save(eq("token-1"), stateCaptor.capture());
        ChallengeState state = stateCaptor.getValue();

        assertEquals(userId, state.userId());
        assertEquals("pwd", state.primaryAmr());
        assertEquals("device-1", state.deviceId());
        assertEquals("203.0.113", state.ipPrefix());
        assertEquals(tokenHasher.hashToken("Mozilla/5.0"), state.userAgentHash());
        assertEquals(0, state.attempts());
        assertNotNull(state.createdAt());
        assertEquals(state.createdAt().plus(properties.getTtl()), state.expiresAt());
        assertEquals(state.expiresAt(), challenge.expiresAt());

        verify(auditEventLogger).log(
                eq("auth.mfa.challenge.issued"),
                eq(userId),
                eq("device-1"),
                eq("203.0.113.42"),
                eq("success"),
                isNull(),
                eq("req-1")
        );
    }

    @Test
    void peek_shouldDelegateToStore() {
        MfaChallengeTokenGenerator tokenGenerator = mock(MfaChallengeTokenGenerator.class);
        ChallengeStateRedisStore store = mock(ChallengeStateRedisStore.class);
        MfaChallengeProperties properties = new MfaChallengeProperties();
        TokenHasher tokenHasher = new TokenHasher();
        AuditEventLogger auditEventLogger = mock(AuditEventLogger.class);

        MfaChallengeServiceImpl service = new MfaChallengeServiceImpl(
                tokenGenerator,
                store,
                properties,
                tokenHasher,
                auditEventLogger
        );

        ChallengeState state = new ChallengeState(UUID.randomUUID(), "pwd", null, null, null, 0, null, null);
        when(store.find("token-1")).thenReturn(Optional.of(state));

        Optional<ChallengeState> peeked = service.peek("token-1");

        assertTrue(peeked.isPresent());
        assertSame(state, peeked.get());
    }

    @Test
    void invalidate_shouldDeleteAndAudit() {
        MfaChallengeTokenGenerator tokenGenerator = mock(MfaChallengeTokenGenerator.class);
        ChallengeStateRedisStore store = mock(ChallengeStateRedisStore.class);
        MfaChallengeProperties properties = new MfaChallengeProperties();
        TokenHasher tokenHasher = new TokenHasher();
        AuditEventLogger auditEventLogger = mock(AuditEventLogger.class);

        MfaChallengeServiceImpl service = new MfaChallengeServiceImpl(
                tokenGenerator,
                store,
                properties,
                tokenHasher,
                auditEventLogger
        );

        UUID userId = UUID.randomUUID();
        ChallengeState state = new ChallengeState(userId, "pwd", null, null, null, 0, null, null);
        when(store.find("token-1")).thenReturn(Optional.of(state));
        when(store.deleteIfPresent("token-1")).thenReturn(true);

        AuthRequestContext context = new AuthRequestContext("device-1", "203.0.113.42", "req-1", "ua");
        service.invalidate("token-1", context);

        verify(store).deleteIfPresent("token-1");
        verify(auditEventLogger).log(
                eq("auth.mfa.challenge.invalidated"),
                eq(userId),
                eq("device-1"),
                eq("203.0.113.42"),
                eq("success"),
                isNull(),
                eq("req-1")
        );
    }
}

