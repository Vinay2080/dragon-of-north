package org.miniProjectTwo.DragonOfNorth.services.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.components.TokenHasher;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Session;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.JwtServices;
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
import static org.mockito.Mockito.*;

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

        when(sessionRepository.findAllByAppUserIdOrderByLastUsedAtDesc(userId)).thenReturn(List.of(session));

        var result = sessionService.getSessionsForUser(userId);

        assertEquals(1, result.size());
        assertEquals("d1", result.get(0).deviceId());
    }

    @Test
    void revokeAllOtherSessions_shouldThrow_whenDeviceIdBlank() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.revokeAllOtherSessions(UUID.randomUUID(), " "));

        assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
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
}
