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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                primaryAmr.trim(),
                requestBinding.normalizeDeviceId(context.deviceId()),
                requestBinding.ipPrefix(context.ipAddress()),
                requestBinding.userAgentHash(context.userAgent()),
                0,
                createdAt,
                expiresAt
        );

        store.save(token, state);
        auditEventLogger.log("auth.mfa.challenge.issued",
                userId,
                context.deviceId(),
                context.ipAddress(),
                "success",
                null,
                context.requestId());

        return new MfaChallenge(token, expiresAt, availableMethods);
    }

    @Override
    public VerificationResult verifyAndConsume(String mfaToken,
                                               ProviderType providerType,
                                               String code,
                                               AuthRequestContext context) {
        if (mfaToken == null || mfaToken.isBlank()) throw new IllegalArgumentException("mfaToken must not be blank");
        if (providerType == null) throw new IllegalArgumentException("providerType must not be null");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        if (context == null) throw new IllegalArgumentException("context must not be null");

        String token = mfaToken.trim();
        String lockValue = UUID.randomUUID().toString();
        var claim = atomicOps.claim(token, lockValue, CLAIM_LOCK_TTL);
        if (claim.status() != ChallengeStateAtomicRedisOps.ClaimStatus.OK || claim.stateJson() == null) {
            auditEventLogger.log("auth.mfa.challenge.failed", null, context.deviceId(), context.ipAddress(), "failure", claim.status().name().toLowerCase(), context.requestId());
            return new VerificationResult(null, null, false);
        }

        ChallengeState state = store.decode(claim.stateJson());
        var bindings = requestBinding.fromContext(context);
        VerificationResult verificationResult = new VerificationResult(state.userId(), state.primaryAmr(), false);
        if (!java.util.Objects.equals(state.deviceId(), bindings.deviceId())
                || !java.util.Objects.equals(state.ipPrefix(), bindings.ipPrefix())
                || !java.util.Objects.equals(state.userAgentHash(), bindings.userAgentHash())) {
            var fail = atomicOps.recordFailure(token, lockValue, properties.getMaxAttempts(), properties.getLockoutTtl());
            String event = fail.status() == ChallengeStateAtomicRedisOps.FailStatus.LOCKED ? "auth.mfa.challenge.locked" : "auth.mfa.challenge.failed";
            auditEventLogger.log(event, state.userId(), context.deviceId(), context.ipAddress(), "failure", "context_mismatch", context.requestId());
            return verificationResult;
        }

        var providerVerification = providerVerifier.verify(state.userId(), providerType, code.trim());
        if (!providerVerification.success()) {
            var fail = atomicOps.recordFailure(token, lockValue, properties.getMaxAttempts(), properties.getLockoutTtl());
            String event = fail.status() == ChallengeStateAtomicRedisOps.FailStatus.LOCKED ? "auth.mfa.challenge.locked" : "auth.mfa.challenge.failed";
            auditEventLogger.log(event, state.userId(), context.deviceId(), context.ipAddress(), "failure", providerVerification.failureReason(), context.requestId());
            return verificationResult;
        }

        boolean consumed = atomicOps.consumeSuccess(token, lockValue);
        if (!consumed) {
            auditEventLogger.log("auth.mfa.challenge.failed", state.userId(), context.deviceId(), context.ipAddress(), "failure", "consume_race", context.requestId());
            return verificationResult;
        }

        auditEventLogger.log("auth.mfa.challenge.verified", state.userId(), context.deviceId(), context.ipAddress(), "success", null, context.requestId());
        return new VerificationResult(state.userId(), state.primaryAmr(), true);
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
        auditEventLogger.log("auth.mfa.challenge.invalidated",
                userId,
                context.deviceId(),
                context.ipAddress(),
                "success",
                reason,
                context.requestId());
    }

}
