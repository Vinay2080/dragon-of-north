package org.miniProjectTwo.DragonOfNorth.modules.session.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.session.dto.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
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
import java.util.UUID;

/**
 * Default implementation of session lifecycle operations.
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
     * Creates a new session for a device and replaces any existing device session.
     */
    @Override
    @Transactional
    public void createSession(AppUser appUser, String rawRefreshToken, String ipAddress, String deviceId, String userAgent) {
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
        sessionRepository.save(session);
        auditEventLogger.log("session.create", appUser.getId(), deviceId, ipAddress, "success",
                "replaced_existing=" + replacedExisting, null);
    }


    /**
     * Revokes the current-device session if found.
     */
    @Override
    @Transactional
    public void revokeSession(String refreshToken, String deviceId) {
        UUID userId = jwtServices.extractUserId(refreshToken);
        AppUser appUser = loadAndValidateUser(userId, UserLifecycleOperation.SESSION_REVOKE_CURRENT);

        String tokenHash = tokenHasher.hashToken(refreshToken);

        var sessionOptional = sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser(tokenHash, deviceId, appUser);
        if (sessionOptional.isEmpty()) {
            meterRegistry.counter("session.revoked.failure").increment();
            auditEventLogger.log("session.revoke.current", userId, deviceId, null, "failure", "session not found", null);
            return;
        }

        sessionOptional.get().setRevoked(true);
        meterRegistry.counter("session.revoked.current").increment();
        auditEventLogger.log("session.revoke.current", userId, deviceId, null, "success", null, null);

    }

    /**
     * Retrieves sessions for a user ordered by last usage time.
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
     * Revokes a single user-owned session by id.
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
     * Revokes all sessions except the provided current device id.
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
     * Validates and rotates the refresh-token hash for an existing session.
     */
    @Override
    @Transactional
    public UUID validateAndRotateSession(String oldRefreshToken, String newRefreshToken, String deviceId) {
        UUID userId = jwtServices.extractUserId(oldRefreshToken);
        AppUser appUser = loadAndValidateUser(userId, UserLifecycleOperation.SESSION_ROTATE_REFRESH);
        String oldTokenHash = tokenHasher.hashToken(oldRefreshToken);
        Session session = sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser(oldTokenHash, deviceId, appUser)
                .orElseThrow(() -> {
                    auditEventLogger.log("session.rotate", userId, deviceId, null, "failure", "session not found", null);
                    return new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid session: token not found");
                });

        if (session.isExpired()) {
            sessionRepository.delete(session);
            auditEventLogger.log("session.rotate", userId, deviceId, null, "failure", "session expired", null);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Session expired");
        }

        if (session.isRevoked()) {
            auditEventLogger.log("session.rotate", userId, deviceId, null, "failure", "session revoked", null);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Session revoked");
        }

        String newTokenHash = tokenHasher.hashToken(newRefreshToken);
        session.setRefreshTokenHash(newTokenHash);
        session.setLastUsedAt(Instant.now());
        auditEventLogger.log("session.rotate", userId, deviceId, null, "success", null, null);

        return appUser.getId();

    }

    /**
     * Revokes every active session for the given user id.
     */
    @Override
    @Transactional
    public void revokeAllSessionsByUserId(UUID userId) {
        int revoked = sessionRepository.revokeAllSessionsByUserId(userId);
        meterRegistry.counter("session.revoked.all_user").increment(revoked);
        auditEventLogger.log("session.revoke.all_user", userId, null, null, "success", "revoked_count=" + revoked, null);
    }

    private AppUser loadAndValidateUser(UUID userId, UserLifecycleOperation operation) {
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));
        userStateValidator.validate(appUser, operation);
        return appUser;
    }
}
