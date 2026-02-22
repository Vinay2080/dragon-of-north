package org.miniProjectTwo.DragonOfNorth.services.auth;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.components.TokenHasher;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Session;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.JwtServices;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final TokenHasher tokenHasher;
    private final SessionRepository sessionRepository;
    private final JwtServices jwtServices;
    private final AppUserRepository appUserRepository;
    private final MeterRegistry meterRegistry;

    @Value("${app.security.jwt.expiration.refresh-token}")
    private long refreshTokenDurationMs;

    @Override
    @Transactional
    public void createSession(AppUser appUser, String rawRefreshToken, String ipAddress, String deviceId, String userAgent) {
        String tokenHash = tokenHasher.hashToken(rawRefreshToken);

        sessionRepository.findByAppUserAndDeviceId(appUser, deviceId).ifPresent(sessionRepository::delete);
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
        meterRegistry.counter("session.created").increment();
        log.info("audit=session_created user_id={} device_id={} ip_address={} user_agent={}", appUser.getId(), deviceId, ipAddress, userAgent);
    }

    @Override
    @Transactional
    public UUID validateAndUpdateSession(String refreshToken, String deviceId) {
        Session session = resolveValidSession(refreshToken, deviceId);
        session.setLastUsedAt(Instant.now());
        meterRegistry.counter("session.refresh.validated").increment();
        return session.getAppUser().getId();
    }

    @Override
    @Transactional
    public UUID validateAndRotateSession(String oldRefreshToken, String newRefreshToken, String deviceId) {
        Session session = resolveValidSession(oldRefreshToken, deviceId);
        session.setRefreshTokenHash(tokenHasher.hashToken(newRefreshToken));
        session.setLastUsedAt(Instant.now());
        meterRegistry.counter("session.refresh.rotated").increment();
        log.info("audit=session_rotated user_id={} device_id={}", session.getAppUser().getId(), deviceId);
        return session.getAppUser().getId();
    }

    @Override
    @Transactional
    public void revokeSession(String refreshToken, String deviceId) {
        Session session = resolveValidSession(refreshToken, deviceId);
        session.setRevoked(true);
        meterRegistry.counter("session.revoked.single").increment();
        log.info("audit=session_revoked user_id={} device_id={} reason=logout", session.getAppUser().getId(), deviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getSessionsForUser(UUID userId, String currentDeviceId) {
        return sessionRepository.findAllByAppUserIdOrderByLastUsedAtDesc(userId).stream()
                .map(session -> new SessionSummaryResponse(
                        session.getId(),
                        session.getDeviceId(),
                        session.getIpAddress(),
                        session.getUserAgent(),
                        session.getLastUsedAt(),
                        session.getExpiryDate(),
                        session.isRevoked(),
                        currentDeviceId != null && currentDeviceId.equals(session.getDeviceId())
                ))
                .toList();
    }

    @Override
    @Transactional
    public void revokeSessionById(UUID userId, UUID sessionId) {
        Session session = sessionRepository.findByIdAndAppUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "Session not found for current user"));
        session.setRevoked(true);
        meterRegistry.counter("session.revoked.by_id").increment();
        log.info("audit=session_revoked user_id={} session_id={} reason=device_management", userId, sessionId);
    }

    @Override
    @Transactional
    public int revokeAllOtherSessions(UUID userId, String currentDeviceId) {
        if (currentDeviceId == null || currentDeviceId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "device ID missing");
        }
        int revokedCount = sessionRepository.revokeAllOtherSessions(userId, currentDeviceId);
        if (revokedCount > 0) {
            meterRegistry.counter("session.revoked.other_devices").increment(revokedCount);
        }
        log.info("audit=session_revoked_others user_id={} current_device_id={} revoked_count={}", userId, currentDeviceId, revokedCount);
        return revokedCount;
    }

    private Session resolveValidSession(String refreshToken, String deviceId) {
        UUID userId = jwtServices.extractUserId(refreshToken);
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));

        String tokenHash = tokenHasher.hashToken(refreshToken);
        Session session = sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser(tokenHash, deviceId, appUser)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid session: token not found"));

        if (session.isExpired()) {
            sessionRepository.delete(session);
            meterRegistry.counter("session.refresh.expired").increment();
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Session expired");
        }

        if (session.isRevoked()) {
            meterRegistry.counter("session.refresh.revoked").increment();
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Session revoked");
        }

        return session;
    }
}
