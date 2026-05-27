package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.service.MfaChallengeService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.provider.MfaProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.registry.MfaProviderRegistry;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.SecurityAuditEvent;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaOrchestratorTest {

    @Test
    void orchestrateLogin_returnsNoChallenge_whenMfaDisabled() {
        MfaChallengeService challengeService = mock(MfaChallengeService.class);
        MfaProviderRegistry providerRegistry = mock(MfaProviderRegistry.class);
        AuditEventLogger auditEventLogger = mock(AuditEventLogger.class);
        MfaOrchestrator orchestrator = new MfaOrchestrator(challengeService, providerRegistry, auditEventLogger);

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setMfaEnabled(false);

        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");
        when(providerRegistry.getAvailableProviders(user)).thenReturn(List.of());

        MfaOrchestrationResult result = orchestrator.orchestrateLogin(user, "pwd", context);

        assertFalse(result.challengeRequired());
        verifyNoInteractions(challengeService);
        verifyNoInteractions(auditEventLogger);
    }

    @Test
    void orchestrateLogin_returnsChallenge_whenMfaEnabledAndProviderAvailable() {
        MfaChallengeService challengeService = mock(MfaChallengeService.class);
        MfaProviderRegistry providerRegistry = mock(MfaProviderRegistry.class);
        AuditEventLogger auditEventLogger = mock(AuditEventLogger.class);
        MfaOrchestrator orchestrator = new MfaOrchestrator(challengeService, providerRegistry, auditEventLogger);

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setMfaEnabled(true);

        MfaProvider provider = mock(MfaProvider.class);
        when(provider.allowsLoginChallenge()).thenReturn(true);
        when(provider.type()).thenReturn(ProviderType.TOTP);

        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");
        when(providerRegistry.getAvailableProviders(user)).thenReturn(List.of(provider));
        when(challengeService.createChallenge(eq(user.getId()), eq("pwd"), eq(context), eq(List.of(ProviderType.TOTP))))
                .thenReturn(new MfaChallenge("challenge-1", Instant.now().plusSeconds(120), List.of(ProviderType.TOTP)));

        MfaOrchestrationResult result = orchestrator.orchestrateLogin(user, "pwd", context);

        assertTrue(result.challengeRequired());
        assertNotNull(result.challenge());
    }

    @Test
    void orchestrateLogin_throwsWhenMfaEnabledAndNoProviders() {
        MfaChallengeService challengeService = mock(MfaChallengeService.class);
        MfaProviderRegistry providerRegistry = mock(MfaProviderRegistry.class);
        AuditEventLogger auditEventLogger = mock(AuditEventLogger.class);
        MfaOrchestrator orchestrator = new MfaOrchestrator(challengeService, providerRegistry, auditEventLogger);

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setMfaEnabled(true);

        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");
        when(providerRegistry.getAvailableProviders(user)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orchestrator.orchestrateLogin(user, "pwd", context));

        assertEquals(ErrorCode.MFA_CONFIGURATION_INVALID, ex.getErrorCode());
        verify(challengeService, never()).createChallenge(any(), anyString(), any(), anyList());
        verify(auditEventLogger).log(
                eq(SecurityAuditEvent.AUTH_MFA_CONFIGURATION_INVALID),
                eq(user.getId()),
                eq("device-1"),
                eq("127.0.0.1"),
                eq("failure"),
                contains("no_login_capable_provider"),
                eq("req-1")
        );
    }
}
