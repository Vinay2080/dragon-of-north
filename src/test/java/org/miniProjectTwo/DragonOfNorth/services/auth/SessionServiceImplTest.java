package org.miniProjectTwo.DragonOfNorth.services.auth;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @InjectMocks
    private SessionServiceImpl sessionService;

    @Mock private TokenHasher tokenHasher;
    @Mock private SessionRepository sessionRepository;
    @Mock private JwtServices jwtServices;
    @Mock private AppUserRepository appUserRepository;
    @Mock private MeterRegistry meterRegistry;
    @Mock private Counter counter;

    @BeforeEach
    void setup() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        ReflectionTestUtils.setField(sessionService, "refreshTokenDurationMs", 60000L);
    }

    @Test
    void revokeAllOtherSessions_shouldReturnCount() {
        UUID userId = UUID.randomUUID();
        when(sessionRepository.revokeAllOtherSessions(userId, "dev-1")).thenReturn(3);

        int count = sessionService.revokeAllOtherSessions(userId, "dev-1");

        assertEquals(3, count);
        verify(sessionRepository).revokeAllOtherSessions(userId, "dev-1");
    }

    @Test
    void revokeAllOtherSessions_shouldFailWhenDeviceBlank() {
        UUID userId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.revokeAllOtherSessions(userId, "  "));

        assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
        verify(sessionRepository, never()).revokeAllOtherSessions(any(), any());
    }

    @Test
    void validateAndRotateSession_shouldRotateHashAndReturnUserId() {
        UUID userId = UUID.randomUUID();
        AppUser appUser = new AppUser();
        appUser.setId(userId);

        Session session = new Session();
        session.setAppUser(appUser);
        session.setExpiryDate(Instant.now().plusSeconds(120));
        session.setRevoked(false);

        when(jwtServices.extractUserId("old-token")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(appUser));
        when(tokenHasher.hashToken("old-token")).thenReturn("old-hash");
        when(tokenHasher.hashToken("new-token")).thenReturn("new-hash");
        when(sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser("old-hash", "dev-1", appUser))
                .thenReturn(Optional.of(session));

        UUID result = sessionService.validateAndRotateSession("old-token", "new-token", "dev-1");

        assertEquals(userId, result);
        assertEquals("new-hash", session.getRefreshTokenHash());
    }

    @Test
    void getSessionsForUser_shouldMarkCurrentSession() {
        UUID userId = UUID.randomUUID();
        Session s = new Session();
        s.setId(UUID.randomUUID());
        s.setDeviceId("dev-1");
        s.setIpAddress("127.0.0.1");
        s.setUserAgent("ua");
        s.setExpiryDate(Instant.now().plusSeconds(100));
        s.setLastUsedAt(Instant.now());

        when(sessionRepository.findAllByAppUserIdOrderByLastUsedAtDesc(userId)).thenReturn(List.of(s));

        var out = sessionService.getSessionsForUser(userId, "dev-1");

        assertEquals(1, out.size());
        assertTrue(out.getFirst().currentSession());
    }
}
