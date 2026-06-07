package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator;

import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coordinates post-primary-auth MFA decisioning and challenge handoff.
 * <p>
 * Bridges auth services and provider/challenge subsystems so local/OAuth/passwordless flows share
 * consistent step-up behavior.
 */
@Service
@RequiredArgsConstructor
public class MfaOrchestrator {

    private final MfaChallengeService challengeService;
    private final MfaProviderRegistry providerRegistry;
    private final AuditEventLogger auditEventLogger;

    /**
     * Determines if MFA is required for the given user and primary authentication method, and if so, creates an MFA challenge and returns the necessary context for the client to proceed with MFA verification. This method checks if the user has MFA enabled, retrieves the available MFA providers for the user, and logs an audit event if there is a configuration issue (MFA enabled but no providers available). If MFA is not required, it returns a result indicating that no challenge is needed along with the available methods. If MFA is required and properly configured, it creates a new MFA challenge using the {@link MfaChallengeService} and returns it in the orchestration result.
     *
     * @param user       The user for whom the MFA orchestration is being performed. Must not be null.
     * @param primaryAmr The primary authentication method reference. Must not be null or blank.
     * @param context    The authentication request context. Must not be null.
     * @return The result of the MFA orchestration.
     */
    public MfaOrchestrationResult orchestrateLogin(AppUser user, String primaryAmr, AuthRequestContext context) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (primaryAmr == null || primaryAmr.isBlank()) {
            throw new IllegalArgumentException("primaryAmr must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        List<ProviderType> availableMethods = providerRegistry.getAvailableProviders(user).stream()
                .filter(MfaProvider::allowsLoginChallenge)
                .map(MfaProvider::type)
                .toList();

        if (!user.isMfaEnabled()) {
            return MfaOrchestrationResult.noChallenge(false, availableMethods);
        }

        if (availableMethods.isEmpty()) {
            auditEventLogger.log(
                    SecurityAuditEvent.AUTH_MFA_CONFIGURATION_INVALID,
                    user.getId(),
                    context.deviceId(),
                    context.ipAddress(),
                    "failure",
                    "mfa_enabled_but_no_login_capable_provider",
                    context.requestId()
            );
            throw new BusinessException(ErrorCode.MFA_CONFIGURATION_INVALID);
        }

        MfaChallenge challenge = challengeService.createChallenge(user.getId(), primaryAmr, context, availableMethods);
        return MfaOrchestrationResult.withChallenge(challenge);
    }
}
