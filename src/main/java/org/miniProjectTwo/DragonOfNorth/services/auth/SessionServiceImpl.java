package org.miniProjectTwo.DragonOfNorth.services.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.components.TokenHasher;
import org.miniProjectTwo.DragonOfNorth.dto.session.response.SessionSummaryResponse;
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
        log.info("Created session for user {} on device {}", appUser.getId(), deviceId);
    }


    @Override
    @Transactional
    public void revokeSession(String refreshToken, String deviceId) {
        UUID userId = jwtServices.extractUserId(refreshToken);
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));

        String tokenHash = tokenHasher.hashToken(refreshToken);

        sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser(tokenHash, deviceId, appUser)
                .ifPresent(session ->
                        session.setRevoked(true));
        log.info("Revoked session for user {} on device {}", userId, deviceId);

    }

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

    @Override
    @Transactional
    public void revokeSessionById(UUID userId, UUID sessionId) {
        Session session = sessionRepository.findByIdAndAppUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "Session not found"));

        if (!session.isRevoked()) {
            session.setRevoked(true);
        }

        log.info("Revoked session {} for user {}", sessionId, userId);
    }

    @Override
    @Transactional
    public int revokeAllOtherSessions(UUID userId, String currentDeviceId) {
        if (currentDeviceId == null || currentDeviceId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "device ID missing");
        }
        int revokedCount = sessionRepository.revokeAllOtherSessions(userId, currentDeviceId);
        log.info("Revoked {} other sessions for user {} (kept devices{})", revokedCount, userId, currentDeviceId);
        return revokedCount;
    }

    @Override
    @Transactional
    public UUID validateAndRotateSession(String oldRefreshToken, String newRefreshToken, String deviceId) {
        UUID userId = jwtServices.extractUserId(oldRefreshToken);
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "user not found"));
        String oldTokenHash = tokenHasher.hashToken(oldRefreshToken);
        Session session = sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser(oldTokenHash, deviceId, appUser)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid session: token not found"));

        if (session.isExpired()) {
            sessionRepository.delete(session);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Session expired");
        }

        if (session.isRevoked()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Session revoked");
        }

        String newTokenHash = tokenHasher.hashToken(newRefreshToken);
        session.setRefreshTokenHash(newTokenHash);
        session.setLastUsedAt(Instant.now());

        return appUser.getId();

    }
}
