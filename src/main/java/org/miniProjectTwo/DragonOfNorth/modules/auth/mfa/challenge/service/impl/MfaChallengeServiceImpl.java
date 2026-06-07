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
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.SecurityAuditContext;
import org.miniProjectTwo.DragonOfNorth.shared.util.SecurityAuditEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

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


    /**
     * Create a new MFA challenge for a user after primary authentication. This is used by {@code /auth/mfa/challenge} to initiate the MFA process when a user successfully authenticates with their primary method but requires additional verification. The method validates the input parameters, generates a unique challenge token, constructs the ChallengeState with all necessary context (user ID, primary AMR, device ID, IP address, user agent hash, available MFA methods), and saves it to Redis with an expiration time. We also log a security audit event for the challenge issuance, providing visibility into when challenges are created and their associated context (user ID, device ID, IP address, user agent hash, primary AMR). The returned MfaChallenge contains the opaque token and expiration time for the client to proceed with MFA verification.
     *
     * @param userId           The unique identifier of the user for whom the challenge is being created.
     * @param primaryAmr       The primary authentication method reference for the user.
     * @param context          The authentication request context, providing additional information about the authentication attempt.
     * @param availableMethods The list of available MFA methods for the user, based on their enrollment and the authentication context.
     * @return An MfaChallenge object containing the token and expiration time for the client to use in MFA verification.
     */
    @Override
    public MfaChallenge createChallenge(UUID userId,
                                        String primaryAmr,
                                        AuthRequestContext context,
                                        List<ProviderType> availableMethods) {
        return createChallengeInternal(userId, null, primaryAmr, context, availableMethods);
    }

    /**
     * Create a step-up MFA challenge tied to an existing authenticated session. This is used by {@code /auth/mfa/step-up} when an already authenticated user attempts to perform an action that requires additional verification (e.g., accessing sensitive information, performing a high-risk transaction, etc.). The method validates the input parameters, ensuring that the sessionId is provided for step-up challenges and then delegates to the internal challenge creation logic. By tying the challenge to an existing session, we can enforce that the MFA verification is directly linked to the user's current authenticated context, providing stronger security guarantees for sensitive operations.
     *
     * @param userId           The unique identifier of the user for whom the MFA challenge is being created.
     * @param sessionId        The unique identifier of the authenticated session to which the challenge will be bound.
     * @param context          The authentication request context, providing additional information about the authentication attempt (device ID, IP address, user agent, etc.) for logging and risk analysis.
     * @param availableMethods The list of MFA provider types that are available for this challenge, determined based on the user's enrolled methods and the context of the authentication attempt, used to inform the client of which MFA options to present to the user.
     * @return An MfaChallenge object containing the opaque token, expiration time, and available methods for the client to proceed with MFA verification.
     */
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

    /**
     * Peek at the current state of an MFA challenge without modifying it. This is used by {@code /auth/mfa/challenge/status} to allow clients to check the status of an MFA challenge (e.g., to see if it has expired or if certain context has changed) without consuming or altering the challenge state. We validate the input token, attempt to retrieve the challenge state from Redis, and handle any Redis errors gracefully while logging them for security monitoring. The returned Optional will be empty if the challenge does not exist or has expired, allowing the client to handle those cases appropriately.
     *
     * @param mfaToken The opaque token identifying the MFA challenge.
     * @return An Optional containing the ChallengeState if found, or empty if the challenge does not exist or has expired.
     */
    @Override
    public Optional<ChallengeState> peek(String mfaToken) {
        if (mfaToken == null || mfaToken.isBlank()) {
            throw new IllegalArgumentException("mfaToken must not be blank");
        }
        return withRedisGuard(
                () -> store.find(mfaToken.trim()),
                null,
                null,
                null,
                "mfa_challenge",
                null,
                "challenge_peek_redis_error"
        );
    }

    @Override
    public VerificationResult verifyAndConsume(String mfaToken,
                                               ProviderType providerType,
                                               String code,
                                               AuthRequestContext context) {
        return verifyAndConsume(mfaToken, providerType, code, context, null);
    }

    /**
     *  Verify an MFA code for a given challenge token and provider type, consuming the challenge if successful. This is used by {@code /auth/mfa/verify} to process MFA verification attempts. We validate the input parameters, attempt to claim the challenge in Redis to ensure exclusive access, and handle various failure scenarios (e.g., challenge not found, locked out, context mismatch, invalid code) with appropriate logging and audit events. If the verification is successful, we consume the challenge to prevent reuse and return a success result with the user's ID and primary AMR for session creation. The optional sessionId parameter allows this method to be used for both initial MFA verification (where the session is not yet established) and step-up MFA (where the session already exists and must be validated against the challenge context). We ensure that all Redis interactions are wrapped with error handling to maintain security monitoring and infrastructure resilience. The method returns a VerificationResult indicating success or the specific reason for failure, allowing the calling code to respond appropriately (e.g., prompting for another attempt, showing an error message, etc.).
     * @param mfaToken The opaque token identifying the MFA challenge.
     * @param providerType The type of MFA provider used for verification.
     * @param code The verification code provided by the user.
     * @param context The authentication request context, providing additional information about the authentication attempt.
     * @param sessionId The unique identifier of the authenticated session to which the challenge is bound.
     * @return A VerificationResult indicating whether the verification was successful or the specific reason for failure, along with user ID and primary AMR if successful.
     */
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
        var claim = withRedisGuard(
                () -> atomicOps.claim(token, lockValue, CLAIM_LOCK_TTL),
                context,
                null,
                sessionId,
                "mfa_challenge",
                providerType,
                "challenge_claim_redis_error"
        );
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
        return VerificationResult.success(state.userId(), state.primaryAmr(), providerType.getKey());
        } finally {
            withRedisGuard(() -> {
                atomicOps.unlock(token, lockValue);
                return null;
            }, context, null, sessionId, "mfa_challenge", providerType, "challenge_unlock_redis_error");
        }
    }

    /**
     *  Invalidate an MFA challenge by its token. This is used to clean up challenges that are no longer needed (e.g., after successful authentication or when the user cancels the login process). We attempt to look up the challenge state first to gather context for auditing, then delete the challenge from Redis. We log a security event for the invalidation action, including whether it was successful or if the challenge was not found, to maintain an audit trail of MFA lifecycle events.
     * @param mfaToken The opaque token identifying the MFA challenge to invalidate.
     * @param context The authentication request context, providing additional information about the authentication attempt.
     */
    @Override
    public void invalidate(String mfaToken, AuthRequestContext context) {
        if (mfaToken == null || mfaToken.isBlank()) {
            throw new IllegalArgumentException("mfaToken must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        Optional<ChallengeState> state = withRedisGuard(
                () -> store.find(mfaToken.trim()),
                context,
                null,
                null,
                "mfa_challenge",
                null,
                "challenge_invalidate_lookup_redis_error"
        );
        boolean deleted = withRedisGuard(
                () -> store.deleteIfPresent(mfaToken.trim()),
                context,
                state.map(ChallengeState::userId).orElse(null),
                state.map(ChallengeState::sessionId).orElse(null),
                state.map(ChallengeState::primaryAmr).orElse("mfa_challenge"),
                null,
                "challenge_invalidate_delete_redis_error"
        );

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

    /**
     * Internal method to create an MFA challenge, used by both initial and step-up challenge creation. This method centralizes the logic for generating the challenge token, constructing the ChallengeState with all necessary context, and saving it to Redis. We validate the input parameters to ensure we have the required information to create a valid challenge. The method also handles logging a security audit event for the challenge issuance, providing visibility into when challenges are created and their associated context (user ID, session ID, device ID, IP address, user agent hash, primary AMR). By having a single internal method, we can ensure consistent behavior and logging for both types of challenges while keeping the public interface clean and focused on the specific use cases.
     *
     * @param userId           The unique identifier of the user for whom the MFA challenge is being created.
     * @param sessionId        The unique identifier of the authenticated session to which the challenge will be bound (optional for initial challenges, required for step-up challenges).
     * @param primaryAmr       The primary authentication method used that triggered the need for MFA (e.g., "password", "oauth", "step_up"), used for context and risk analysis.
     * @param context          The authentication request context, providing additional information about the authentication attempt (device ID, IP address, user agent, etc.) for logging and risk analysis.
     * @param availableMethods The list of MFA provider types that are available for this challenge, determined based on the user's enrolled methods and the context of the authentication attempt, used to inform the client of which MFA options to present to the user.
     * @return An MfaChallenge object containing the opaque token, expiration time, and available methods for the client to proceed with MFA verification.
     */
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

        withRedisGuard(() -> {
            store.save(token, state);
            return null;
        }, context, userId, sessionId, primaryAmr, null, "challenge_create_redis_error");
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

    /**
     *  Wrapper to handle Redis errors gracefully and log them for security monitoring. If a Redis operation fails, we log a security event with as much context as possible (user ID, session ID, device ID, IP address, user agent hash, primary AMR, provider type) to help identify potential issues or attacks. We then throw a generic BusinessException to signal an infrastructure issue without exposing internal details to the client.
     *
     * @param operation the Redis operation to execute, wrapped as a Supplier to allow for flexible return types
     * @param context the authentication request context containing device and request metadata for auditing
     * @param userId the user ID associated with the MFA challenge, if available, for auditing purposes
     * @param sessionId the session ID associated with the MFA challenge, if available, for auditing purposes
     * @param primaryAmr the primary authentication method used for the MFA challenge, if available, for auditing purposes
     * @param providerType the type of MFA provider used for the challenge, if available, for auditing purposes
     * @param reason the reason for the operation, if applicable, for auditing purposes
     * @return the result of the Redis operation, if successful
     * @param <T> the return type of the Redis operation
     */
    private <T> T withRedisGuard(Supplier<T> operation,
                                 AuthRequestContext context,
                                 UUID userId,
                                 UUID sessionId,
                                 String primaryAmr,
                                 ProviderType providerType,
                                 String reason) {
        try {
            return operation.get();
        } catch (RuntimeException ex) {
            String provider = providerType == null ? null : providerType.name();
            String deviceId = context == null ? null : context.deviceId();
            String requestId = context == null ? null : context.requestId();
            String ipAddress = context == null ? null : context.ipAddress();
            String userAgentHash = context == null ? null : requestBinding.userAgentHash(context.userAgent());
            auditEventLogger.logSecurity(SecurityAuditEvent.AUTH_MFA_CHALLENGE_FAILED,
                    new SecurityAuditContext(userId, sessionId, deviceId, requestId, ipAddress, userAgentHash,
                            primaryAmr, provider, "failure", "infrastructure_unavailable",
                            "reason=" + reason));
            throw new BusinessException(ErrorCode.MFA_CHALLENGE_INFRASTRUCTURE_UNAVAILABLE);
        }
    }

}
