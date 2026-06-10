package org.miniProjectTwo.DragonOfNorth.modules.session.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.session.dto.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.SessionCreationSpec;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.TokenHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent session management implementation used by token issuance and SID enforcement paths.
 */
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final TokenHasher tokenHasher;
    private final SessionRepository sessionRepository;
    private final JwtServices jwtServices;
    private final AppUserRepository appUserRepository;
    private final MeterRegistry meterRegistry;
    private final AuditEventLogger auditEventLogger;
    private final UserStateValidator userStateValidator;

    @Value("${app.security.jwt.expiration.refresh-token}")
    private long refreshTokenDurationMs;


    /**
     * Creates a new session for the given user and device, replacing any existing session for the same device.
     * The refresh token is hashed before storage, and the session is initialized with metadata such as IP address and user agent.
     * The method also logs an audit event indicating whether an existing session was replaced and if MFA is required.
     *
     * @param appUser          The user for whom the session is being created. Must not be null.
     * @param rawRefreshToken  The raw refresh token string to be hashed and stored. Must not be null or empty.
     * @param ipAddress        The IP address from which the session is being created. Optional.
     * @param deviceId         The unique identifier of the device for this session. Must not be null or empty.
     * @param userAgent        The user agent string of the client's device. Optional.
     * @param creationSpec     Additional specifications for session creation, such as MFA requirements. Must not be null.
     * @return The created Session entity with all relevant metadata and hashed refresh token.
     * @throws BusinessException If there is an issue during session creation, such as database errors or invalid input.
     */
    @Override
    @Transactional
    public Session createSession(AppUser appUser,
                                 String rawRefreshToken,
                                 String ipAddress,
                                 String deviceId,
                                 String userAgent,
                                 SessionCreationSpec creationSpec) {
        Objects.requireNonNull(creationSpec, "creationSpec must not be null");
        String tokenHash = tokenHasher.hashToken(rawRefreshToken);

        boolean replacedExisting = sessionRepository.findByAppUserAndDeviceId(appUser, deviceId)
                .map(existing -> {
                    sessionRepository.delete(existing);
                    return true;
                })
                .orElse(false);
        sessionRepository.flush();

        Session session = new Session();
        session.setAppUser(appUser);
        session.setRefreshTokenHash(tokenHash);
        session.setDeviceId(deviceId);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        session.setLastUsedAt(Instant.now());
        session.setMfaRequired(creationSpec.mfaRequired());
        session.setMfaVerifiedAt(creationSpec.mfaVerifiedAt());
        session.setPrimaryAmr(creationSpec.primaryAmr());
        session.setMfaMethodAmr(creationSpec.mfaMethodAmr());
        Session saved = sessionRepository.save(session);
        auditEventLogger.log("session.create", appUser.getId(), deviceId, ipAddress, "success",
                "replaced_existing=" + replacedExisting + ",mfa_required=" + creationSpec.mfaRequired(), null);
        return saved;
    }


    /**
     * Revokes the session associated with the given refresh token and device ID. The method first extracts the user ID from the refresh token,
     * validates the user's state for session revocation, and then attempts to revoke the session if it is active. If no active session is found for the given token and device, it checks if a session exists at all to determine whether to log a failure or a success event (already revoked).
     *
     * @param refreshToken The raw refresh token string associated with the session to revoke. Must not be null or empty.
     * @param deviceId     The unique identifier of the device for which to revoke the session. Must not be null or empty.
     * @throws BusinessException If there is an issue during session revocation, such as invalid token, user not found, or database errors.
     */
    @Override
    @Transactional
    public void revokeSession(String refreshToken, String deviceId) {
        UUID userId = jwtServices.extractUserId(refreshToken);
        AppUser appUser = loadAndValidateUser(userId, UserLifecycleOperation.SESSION_REVOKE_CURRENT);

        String tokenHash = tokenHasher.hashToken(refreshToken);

        int updated = sessionRepository.revokeCurrentSessionIfActive(userId, deviceId, tokenHash);
        if (updated == 0) {
            boolean exists = sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser(tokenHash, deviceId, appUser).isPresent();
            if (!exists) {
                meterRegistry.counter("session.revoked.failure").increment();
                auditEventLogger.log("session.revoke.current", userId, deviceId, null, "failure", "session not found", null);
                return;
            }
            auditEventLogger.log("session.revoke.current", userId, deviceId, null, "success", "already_revoked", null);
            return;
        }

        meterRegistry.counter("session.revoked.current").increment();
        auditEventLogger.log("session.revoke.current", userId, deviceId, null, "success", null, null);

    }

    /**
     * Retrieves a list of session summaries for the given user ID, ordered by the last-used timestamp descending. Each summary includes session metadata such as device ID, IP address, user agent, last used time, expiry date, and revocation status.
     *
     * @param userId The UUID of the user for whom to retrieve sessions. Must not be null.
     * @return A list of SessionSummaryResponse objects representing the user's sessions.
     * @throws BusinessException If there is an issue during retrieval, such as user not found or database errors.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getSessionsForUser(UUID userId) {
        return sessionRepository.findAllByAppUserIdOrderByLastUsedAtDesc(userId)
                .stream()
                .map(session -> new SessionSummaryResponse(
                        session.getId(),
                        session.getDeviceId(),
                        session.getIpAddress(),
                        session.getUserAgent(),
                        session.getLastUsedAt(),
                        session.getExpiryDate(),
                        session.isRevoked()
                )).toList();
    }

    /**
     * Revokes a specific session by its ID for the given user. The method first validates the user's state for session revocation, then attempts to find the session by ID and user ID. If the session is found and is not yet revoked, it marks it as revoked. If the session is not found, it logs a failure event and throws a BusinessException.
     *
     * @param userId    The UUID of the user who owns the session to revoke. Must not be null.
     * @param sessionId The UUID of the session to revoke. Must not be null.
     * @throws BusinessException If there is an issue during session revocation, such as user not found, session not found, or database errors.
     */
    @Override
    @Transactional
    public void revokeSessionById(UUID userId, UUID sessionId) {
        loadAndValidateUser(userId, UserLifecycleOperation.SESSION_REVOKE_BY_ID);
        Session session = sessionRepository.findByIdAndAppUserId(sessionId, userId)
                .orElseThrow(() -> {
                    meterRegistry.counter("session.revoked.failure").increment();
                    auditEventLogger.log("session.revoke.by_id", userId, null, null, "failure", "Session not found", null);
                    return new BusinessException(ErrorCode.INVALID_TOKEN, "Session not found");
                });

        if (!session.isRevoked()) {
            session.setRevoked(true);
        }

        meterRegistry.counter("session.revoked.by_id").increment();
        auditEventLogger.log("session.revoke.by_id", userId, session.getDeviceId(), null, "success", null, null);
    }

    /**
     * Revokes all sessions for the given user except the one associated with the current device ID. The method first validates the user's state for session revocation, then attempts to revoke all other sessions. If the current device ID is missing or empty, it logs a failure event and throws a BusinessException.
     *
     * @param userId          The UUID of the user whose sessions are to be revoked. Must not be null.
     * @param currentDeviceId The unique identifier of the current device whose session should not be revoked. Must not be null or empty.
     * @return The number of sessions that were revoked.
     * @throws BusinessException If there is an issue during session revocation, such as user not found, invalid device ID, or database errors.
     */
    @Override
    @Transactional
    public int revokeAllOtherSessions(UUID userId, String currentDeviceId) {
        if (currentDeviceId == null || currentDeviceId.trim().isEmpty()) {
            meterRegistry.counter("session.revoked.failure").increment();
            auditEventLogger.log("session.revoke.others", userId, currentDeviceId, null, "failure", "device ID missing", null);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "device ID missing");
        }
        loadAndValidateUser(userId, UserLifecycleOperation.SESSION_REVOKE_OTHERS);
        int revokedCount = sessionRepository.revokeAllOtherSessions(userId, currentDeviceId);
        meterRegistry.counter("session.revoked.others").increment(revokedCount);
        auditEventLogger.log("session.revoke.others", userId, currentDeviceId, null, "success", "revoked_count=" + revokedCount, null);
        return revokedCount;
    }

    /**
     * Validates the old refresh token and device ID, then rotates the refresh token by replacing the old token hash with the new token hash in the session record. The method first extracts the user ID from the old refresh token, validates the user's state for session rotation, and then attempts to rotate the token if the session is active. If no active session is found for the given token and device, it logs a failure event and throws a BusinessException. If successful, it returns the updated Session entity.
     *
     * @param oldRefreshToken The raw old refresh token string to validate and rotate. Must not be null or empty.
     * @param newRefreshToken The raw new refresh token string to replace the old one. Must not be null or empty.
     * @param deviceId        The unique identifier of the device for which to rotate the session. Must not be null or empty.
     * @return The updated Session entity after successful rotation.
     * @throws BusinessException If there is an issue during session rotation, such as invalid token, user not found, session not rotatable, or database errors.
     */
    @Override
    @Transactional
    public Session validateAndRotateSession(String oldRefreshToken, String newRefreshToken, String deviceId) {
        UUID userId = jwtServices.extractUserId(oldRefreshToken);
        AppUser appUser = loadAndValidateUser(userId, UserLifecycleOperation.SESSION_ROTATE_REFRESH);
        String oldTokenHash = tokenHasher.hashToken(oldRefreshToken);
        String newTokenHash = tokenHasher.hashToken(newRefreshToken);
        Instant now = Instant.now();
        int updated = sessionRepository.rotateRefreshTokenIfActive(
                appUser.getId(),
                deviceId,
                oldTokenHash,
                newTokenHash,
                now,
                now
        );

        if (updated != 1) {
            auditEventLogger.log("session.rotate", userId, deviceId, null, "failure", "session not rotatable", null);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid session: token not rotatable");
        }

        auditEventLogger.log("session.rotate", userId, deviceId, null, "success", null, null);

        return sessionRepository.findLiveByAppUserIdAndDeviceIdAndRefreshTokenHash(appUser.getId(), deviceId, newTokenHash, now)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "Session not found after rotation"));
    }

    /**
     * Revokes all sessions for the given user ID. The method first validates the user's state for session revocation, then attempts to revoke all sessions. It logs an audit event with the count of revoked sessions.
     *
     * @param userId The UUID of the user whose sessions are to be revoked. Must not be null.
     * @throws BusinessException If there is an issue during session revocation, such as user not found or database errors.
     */
    @Override
    @Transactional
    public void revokeAllSessionsByUserId(UUID userId) {
        int revoked = sessionRepository.revokeAllSessionsByUserId(userId);
        meterRegistry.counter("session.revoked.all_user").increment(revoked);
        auditEventLogger.log("session.revoke.all_user", userId, null, null, "success", "revoked_count=" + revoked, null);
    }

    /**
     * Updates the mfaVerifiedAt timestamp on a live session after step-up MFA verification.
     *
     * <p>The update is constrained to sessions that are not revoked, not deleted, and not expired,
     * so a stale session cannot silently receive a refreshed trust timestamp.</p>
     */
    @Override
    @Transactional
    public Session refreshMfaVerifiedAt(UUID sessionId, UUID userId, Instant verifiedAt, String mfaMethodAmr) {
        Instant now = Instant.now();
        int updated = sessionRepository.refreshMfaVerifiedAt(sessionId, userId, verifiedAt, mfaMethodAmr, now);
        if (updated != 1) {
            auditEventLogger.log("session.mfa.step_up", userId, null, null, "failure", "session not found or not live", null);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Session not found or no longer live");
        }
        auditEventLogger.log("session.mfa.step_up", userId, null, null, "success", "session_id=" + sessionId, null);
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "Session not found after mfa refresh"));
    }

    /**
     * Loads the user by ID and validates their state for the given lifecycle operation. If the user is not found or fails validation, a BusinessException is thrown.
     *
     * @param userId    The UUID of the user to load and validate. Must not be null.
     * @param operation The lifecycle operation for which to validate the user's state. Must not be null.
     * @return The loaded AppUser entity if found and valid.
     * @throws BusinessException If the user is not found or fails validation for the specified operation.
     */
    private AppUser loadAndValidateUser(UUID userId, UserLifecycleOperation operation) {
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));
        userStateValidator.validate(appUser, operation);
        return appUser;
    }
}
