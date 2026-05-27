package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.service.impl;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.config.MfaChallengeProperties;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.context.ChallengeRequestBinding;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.ChallengeState;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.VerificationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.provider.MfaChallengeProviderVerifier;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis.ChallengeStateAtomicRedisOps;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis.ChallengeStateRedisStore;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.service.MfaChallengeService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.token.MfaChallengeTokenGenerator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.SecurityAuditContext;
import org.miniProjectTwo.DragonOfNorth.shared.util.SecurityAuditEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed MFA challenge lifecycle implementation with expiration and state-transition control.
 */
@lombok.extern.slf4j.Slf4j
@Service
@RequiredArgsConstructor
public class MfaChallengeServiceImpl implements MfaChallengeService {
    private static final java.time.Duration CLAIM_LOCK_TTL = java.time.Duration.ofSeconds(5);

    private final MfaChallengeTokenGenerator tokenGenerator;
    private final ChallengeStateRedisStore store;
    private final ChallengeStateAtomicRedisOps atomicOps;
    private final ChallengeRequestBinding requestBinding;
    private final MfaChallengeProviderVerifier providerVerifier;
    private final MfaChallengeProperties properties;
    private final AuditEventLogger auditEventLogger;

    @Override
    public MfaChallenge createChallenge(UUID userId,
                                        String primaryAmr,
                                        AuthRequestContext context,
                                        List<ProviderType> availableMethods) {
        return createChallengeInternal(userId, null, primaryAmr, context, availableMethods);
    }

    @Override
    public MfaChallenge createStepUpChallenge(UUID userId,
                                              UUID sessionId,
                                              AuthRequestContext context,
                                              List<ProviderType> availableMethods) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        return createChallengeInternal(userId, sessionId, "step_up", context, availableMethods);
    }

    private MfaChallenge createChallengeInternal(UUID userId,
                                                 UUID sessionId,
                                                 String primaryAmr,
                                                 AuthRequestContext context,
                                                 List<ProviderType> availableMethods) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (primaryAmr == null || primaryAmr.isBlank()) {
            throw new IllegalArgumentException("primaryAmr must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        String token = tokenGenerator.generateToken();
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(properties.getTtl());

        ChallengeState state = new ChallengeState(
                userId,
                sessionId,
                primaryAmr.trim(),
                requestBinding.normalizeDeviceId(context.deviceId()),
                requestBinding.ipPrefix(context.ipAddress()),
                requestBinding.userAgentHash(context.userAgent()),
                availableMethods == null ? List.of() : List.copyOf(availableMethods),
                0,
                createdAt,
                expiresAt
        );

        store.save(token, state);
        auditEventLogger.logSecurity(SecurityAuditEvent.AUTH_MFA_CHALLENGE_ISSUED, new SecurityAuditContext(
                userId,
                sessionId,
                context.deviceId(),
                context.requestId(),
                context.ipAddress(),
                requestBinding.userAgentHash(context.userAgent()),
                primaryAmr,
                null,
                "success",
                null,
                "challenge_ref=" + Integer.toHexString(token.hashCode())
        ));

        return new MfaChallenge(token, expiresAt, availableMethods);
    }

    @Override
    public VerificationResult verifyAndConsume(String mfaToken,
                                               ProviderType providerType,
                                               String code,
                                               AuthRequestContext context) {
        return verifyAndConsume(mfaToken, providerType, code, context, null);
    }

    @Override
    public VerificationResult verifyAndConsume(String mfaToken,
                                               ProviderType providerType,
                                               String code,
                                               AuthRequestContext context,
                                               UUID sessionId) {
        if (mfaToken == null || mfaToken.isBlank()) throw new IllegalArgumentException("mfaToken must not be blank");
        if (providerType == null) throw new IllegalArgumentException("providerType must not be null");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        if (context == null) throw new IllegalArgumentException("context must not be null");

        String token = mfaToken.trim();
        String lockValue = UUID.randomUUID().toString();
        var claim = atomicOps.claim(token, lockValue, CLAIM_LOCK_TTL);
        if (claim.status() != ChallengeStateAtomicRedisOps.ClaimStatus.OK || claim.stateJson() == null) {
            auditEventLogger.logSecurity(SecurityAuditEvent.AUTH_MFA_CHALLENGE_FAILED, new SecurityAuditContext(null, sessionId, context.deviceId(), context.requestId(), context.ipAddress(), requestBinding.userAgentHash(context.userAgent()), "mfa_challenge", providerType.name(), "failure", claim.status().name().toLowerCase(), "challenge_ref=" + Integer.toHexString(token.hashCode())));
            return switch (claim.status()) {
                case LOCKED_OUT -> VerificationResult.failure(null, null, VerificationResult.FailureReason.CHALLENGE_LOCKED_OUT);
                case BUSY -> VerificationResult.failure(null, null, VerificationResult.FailureReason.CHALLENGE_BUSY_OR_REPLAY);
                case MISSING, OK -> VerificationResult.failure(null, null, VerificationResult.FailureReason.CHALLENGE_EXPIRED_OR_MISSING);
            };
        }

        try {

        ChallengeState state = store.decode(claim.stateJson());
        var bindings = requestBinding.fromContext(context);
        boolean sessionMismatch = (state.sessionId() != null && !java.util.Objects.equals(state.sessionId(), sessionId))
                || (state.sessionId() == null && sessionId != null);
        if (!java.util.Objects.equals(state.deviceId(), bindings.deviceId())
                || !java.util.Objects.equals(state.ipPrefix(), bindings.ipPrefix())
                || !java.util.Objects.equals(state.userAgentHash(), bindings.userAgentHash())
                || sessionMismatch) {
            atomicOps.recordFailure(token, lockValue, properties.getMaxAttempts(), properties.getLockoutTtl());
            String reason = sessionMismatch ? "session_mismatch" : "context_mismatch";
            SecurityAuditContext failure = new SecurityAuditContext(state.userId(), state.sessionId(), context.deviceId(), context.requestId(), context.ipAddress(), bindings.userAgentHash(), state.primaryAmr(), providerType.name(), "failure", reason, "challenge_ref=" + Integer.toHexString(token.hashCode()));
            auditEventLogger.logSecurity(SecurityAuditEvent.AUTH_MFA_CHALLENGE_FAILED, failure);
            if (sessionMismatch) {
                auditEventLogger.logSecurity(SecurityAuditEvent.AUTH_SESSION_BINDING_FAILURE, failure);
            } else {
                auditEventLogger.logSecurity(SecurityAuditEvent.AUTH_MFA_SUSPICIOUS_CONTEXT_MISMATCH, failure);
            }
            return VerificationResult.failure(state.userId(), state.primaryAmr(), VerificationResult.FailureReason.CONTEXT_MISMATCH);
        }

        List<ProviderType> allowedProviders = state.allowedProviders() == null ? List.of() : state.allowedProviders();
        if (!allowedProviders.contains(providerType)) {
            atomicOps.recordFailure(token, lockValue, properties.getMaxAttempts(), properties.getLockoutTtl());
            auditEventLogger.logSecurity(SecurityAuditEvent.AUTH_MFA_CHALLENGE_FAILED, new SecurityAuditContext(state.userId(), state.sessionId(), context.deviceId(), context.requestId(), context.ipAddress(), bindings.userAgentHash(), state.primaryAmr(), providerType.name(), "failure", "provider_mismatch", "challenge_ref=" + Integer.toHexString(token.hashCode())));
            return VerificationResult.failure(state.userId(), state.primaryAmr(), VerificationResult.FailureReason.PROVIDER_MISMATCH);
        }

        var providerVerification = providerVerifier.verify(state.userId(), providerType, code.trim());
        if (!providerVerification.success()) {
            atomicOps.recordFailure(token, lockValue, properties.getMaxAttempts(), properties.getLockoutTtl());
            auditEventLogger.logSecurity(SecurityAuditEvent.AUTH_MFA_CHALLENGE_FAILED, new SecurityAuditContext(state.userId(), state.sessionId(), context.deviceId(), context.requestId(), context.ipAddress(), bindings.userAgentHash(), state.primaryAmr(), providerType.name(), "failure", providerVerification.failureReason(), "challenge_ref=" + Integer.toHexString(token.hashCode())));
            return VerificationResult.failure(state.userId(), state.primaryAmr(), VerificationResult.FailureReason.INVALID_VERIFICATION_CODE);
        }

        boolean consumed = atomicOps.consumeSuccess(token, lockValue);
        if (!consumed) {
            auditEventLogger.logSecurity(SecurityAuditEvent.AUTH_MFA_CHALLENGE_REPLAY_DETECTED, new SecurityAuditContext(state.userId(), state.sessionId(), context.deviceId(), context.requestId(), context.ipAddress(), bindings.userAgentHash(), state.primaryAmr(), providerType.name(), "failure", "consume_race", "challenge_ref=" + Integer.toHexString(token.hashCode())));
            return VerificationResult.failure(state.userId(), state.primaryAmr(), VerificationResult.FailureReason.CHALLENGE_CONSUME_RACE);
        }

        auditEventLogger.logSecurity(SecurityAuditEvent.AUTH_MFA_CHALLENGE_VERIFIED, new SecurityAuditContext(state.userId(), state.sessionId(), context.deviceId(), context.requestId(), context.ipAddress(), bindings.userAgentHash(), state.primaryAmr(), providerType.name(), "success", null, "challenge_ref=" + Integer.toHexString(token.hashCode())));
        return VerificationResult.success(state.userId(), state.primaryAmr());
        } finally {
            atomicOps.unlock(token, lockValue);
        }
    }

    @Override
    public Optional<ChallengeState> peek(String mfaToken) {
        if (mfaToken == null || mfaToken.isBlank()) {
            throw new IllegalArgumentException("mfaToken must not be blank");
        }
        return store.find(mfaToken.trim());
    }

    @Override
    public void invalidate(String mfaToken, AuthRequestContext context) {
        if (mfaToken == null || mfaToken.isBlank()) {
            throw new IllegalArgumentException("mfaToken must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        Optional<ChallengeState> state = store.find(mfaToken.trim());
        boolean deleted = store.deleteIfPresent(mfaToken.trim());

        UUID userId = state.map(ChallengeState::userId).orElse(null);
        String reason = deleted ? null : "not_found";
        auditEventLogger.log(SecurityAuditEvent.AUTH_MFA_CHALLENGE_INVALIDATED,
                userId,
                context.deviceId(),
                context.ipAddress(),
                "success",
                reason,
                context.requestId());
    }

}
