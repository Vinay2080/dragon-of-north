package org.miniProjectTwo.DragonOfNorth.modules.session.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.impl.SessionServiceImpl;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.TokenHasher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @InjectMocks
    private SessionServiceImpl sessionService;

    @Mock
    private TokenHasher tokenHasher;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private JwtServices jwtServices;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private Counter counter;
    @Mock
    private AuditEventLogger auditEventLogger;
    @Mock
    private UserStateValidator userStateValidator;

    @Test
    void createSession_shouldReplaceExistingDeviceSession() {
        ReflectionTestUtils.setField(sessionService, "refreshTokenDurationMs", 60000L);

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        Session existing = new Session();

        when(tokenHasher.hashToken("refresh")).thenReturn("hash");
        when(sessionRepository.findByAppUserAndDeviceId(user, "device-1")).thenReturn(Optional.of(existing));

        sessionService.createSession(user, "refresh", "127.0.0.1", "device-1", "ua");

        verify(sessionRepository).delete(existing);
        verify(sessionRepository).flush();
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void getSessionsForUser_shouldMapToResponse() {
        UUID userId = UUID.randomUUID();
        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setDeviceId("d1");
        session.setIpAddress("127.0.0.1");
        session.setUserAgent("ua");
        session.setLastUsedAt(Instant.now());
        session.setExpiryDate(Instant.now().plusSeconds(60));

        AppUser user = new AppUser();
        user.setId(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sessionRepository.findAllByAppUserIdOrderByLastUsedAtDesc(userId)).thenReturn(List.of(session));

        var result = sessionService.getSessionsForUser(userId);

        assertEquals(1, result.size());
        assertEquals("d1", result.getFirst().deviceId());
    }

    @Test
    void revokeAllOtherSessions_shouldThrow_whenDeviceIdBlank() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(meterRegistry.counter(any())).thenReturn(counter);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.revokeAllOtherSessions(user.getId(), " "));

        assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
        verify(auditEventLogger).log(eq("session.revoke.others"), eq(user.getId()), eq(" "), isNull(), eq("failure"), eq("device ID missing"), isNull());

    }

    @Test
    void validateAndRotateSession_shouldThrow_whenSessionRevoked() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        Session session = new Session();
        session.setRevoked(true);
        session.setExpiryDate(Instant.now().plusSeconds(60));

        when(jwtServices.extractUserId("old")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("old")).thenReturn("oldHash");
        when(sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser("oldHash", "d1", user)).thenReturn(Optional.of(session));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.validateAndRotateSession("old", "new", "d1"));

        assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    void validateAndRotateSession_shouldRotateHash_whenValid() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        Session session = new Session();
        session.setRevoked(false);
        session.setExpiryDate(Instant.now().plusSeconds(60));

        when(jwtServices.extractUserId("old")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("old")).thenReturn("oldHash");
        when(tokenHasher.hashToken("new")).thenReturn("newHash");
        when(sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser("oldHash", "d1", user)).thenReturn(Optional.of(session));

        UUID actual = sessionService.validateAndRotateSession("old", "new", "d1");

        assertEquals(userId, actual);
        assertEquals("newHash", session.getRefreshTokenHash());
        assertNotNull(session.getLastUsedAt());
    }

    @Test
    void revokeSession_shouldLogSuccess_whenSessionExists() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        Session session = new Session();

        when(jwtServices.extractUserId("refresh")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("refresh")).thenReturn("hash");
        when(sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser("hash", "device1", user)).thenReturn(Optional.of(session));
        when(meterRegistry.counter(any())).thenReturn(counter);

        sessionService.revokeSession("refresh", "device1");

        // Remove this line - save() is not explicitly called
        // verify(sessionRepository).save(session);

        assertTrue(session.isRevoked());
        verify(auditEventLogger).log("session.revoke.current", userId, "device1", null, "success", null, null);
    }

    @Test
    void revokeSession_shouldLogFailure_whenSessionNotFound() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        when(jwtServices.extractUserId("refresh")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("refresh")).thenReturn("hash");
        when(sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser("hash", "device1", user)).thenReturn(Optional.empty());
        when(meterRegistry.counter(any())).thenReturn(counter);

        sessionService.revokeSession("refresh", "device1");

        verify(auditEventLogger).log("session.revoke.current", userId, "device1", null, "failure", "session not found", null);
    }

    @Test
    void revokeAllOtherSessions_shouldLogSuccess_whenSessionsExist() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(meterRegistry.counter(any())).thenReturn(counter);
        when(sessionRepository.revokeAllOtherSessions(userId, "currentDevice")).thenReturn(3);

        int result = sessionService.revokeAllOtherSessions(userId, "currentDevice");

        assertEquals(3, result);
        verify(auditEventLogger).log("session.revoke.others", userId, "currentDevice", null, "success", "revoked_count=3", null);
    }
}
