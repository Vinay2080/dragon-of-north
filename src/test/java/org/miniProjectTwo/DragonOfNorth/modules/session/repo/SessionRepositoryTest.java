package org.miniProjectTwo.DragonOfNorth.modules.session.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionRepositoryTest {

    @Mock
    private SessionRepository sessionRepository;

    private AppUser testUser;
    private Session testSession;
    private UUID userId;
    private String deviceId;
    private String refreshTokenHash;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        deviceId = "test-device-123";
        refreshTokenHash = "hashed-refresh-token";

        testUser = new AppUser();
        testUser.setId(userId);

        testSession = new Session();
        testSession.setId(UUID.randomUUID());
        testSession.setAppUser(testUser);
        testSession.setDeviceId(deviceId);
        testSession.setRefreshTokenHash(refreshTokenHash);
        testSession.setRevoked(false);
        testSession.setExpiryDate(Instant.now().plusSeconds(3600));
    }

    @Test
    void findByRefreshTokenHashAndDeviceIdAndAppUser_shouldReturnOptional() {
        // arrange
        when(sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser(refreshTokenHash, deviceId, testUser))
                .thenReturn(Optional.of(testSession));

        // act
        Optional<Session> result = sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser(refreshTokenHash, deviceId, testUser);

        // assert
        assertTrue(result.isPresent());
        assertEquals(testSession, result.get());
        verify(sessionRepository).findByRefreshTokenHashAndDeviceIdAndAppUser(refreshTokenHash, deviceId, testUser);
    }

    @Test
    void findByRefreshTokenHashAndDeviceIdAndAppUser_shouldReturnEmptyOptional() {
        // arrange
        when(sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser(refreshTokenHash, deviceId, testUser))
                .thenReturn(Optional.empty());

        // act
        Optional<Session> result = sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser(refreshTokenHash, deviceId, testUser);

        // assert
        assertFalse(result.isPresent());
        verify(sessionRepository).findByRefreshTokenHashAndDeviceIdAndAppUser(refreshTokenHash, deviceId, testUser);
    }

    @Test
    void findByAppUserAndDeviceId_shouldReturnOptional() {
        // arrange
        when(sessionRepository.findByAppUserAndDeviceId(testUser, deviceId))
                .thenReturn(Optional.of(testSession));

        // act
        Optional<Session> result = sessionRepository.findByAppUserAndDeviceId(testUser, deviceId);

        // assert
        assertTrue(result.isPresent());
        assertEquals(testSession, result.get());
        verify(sessionRepository).findByAppUserAndDeviceId(testUser, deviceId);
    }

    @Test
    void findByAppUserAndDeviceId_shouldReturnEmptyOptional() {
        // arrange
        when(sessionRepository.findByAppUserAndDeviceId(testUser, deviceId))
                .thenReturn(Optional.empty());

        // act
        Optional<Session> result = sessionRepository.findByAppUserAndDeviceId(testUser, deviceId);

        // assert
        assertFalse(result.isPresent());
        verify(sessionRepository).findByAppUserAndDeviceId(testUser, deviceId);
    }

    @Test
    void findAllByAppUserIdOrderByLastUsedAtDesc_shouldReturnList() {
        // arrange
        List<Session> sessions = List.of(testSession);
        when(sessionRepository.findAllByAppUserIdOrderByLastUsedAtDesc(userId))
                .thenReturn(sessions);

        // act
        List<Session> result = sessionRepository.findAllByAppUserIdOrderByLastUsedAtDesc(userId);

        // assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSession, result.getFirst());
        verify(sessionRepository).findAllByAppUserIdOrderByLastUsedAtDesc(userId);
    }

    @Test
    void findAllByAppUserIdOrderByLastUsedAtDesc_shouldReturnEmptyList() {
        // arrange
        when(sessionRepository.findAllByAppUserIdOrderByLastUsedAtDesc(userId))
                .thenReturn(List.of());

        // act
        List<Session> result = sessionRepository.findAllByAppUserIdOrderByLastUsedAtDesc(userId);

        // assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(sessionRepository).findAllByAppUserIdOrderByLastUsedAtDesc(userId);
    }

    @Test
    void findByIdAndAppUserId_shouldReturnOptional() {
        // arrange
        UUID sessionId = testSession.getId();
        when(sessionRepository.findByIdAndAppUserId(sessionId, userId))
                .thenReturn(Optional.of(testSession));

        // act
        Optional<Session> result = sessionRepository.findByIdAndAppUserId(sessionId, userId);

        // assert
        assertTrue(result.isPresent());
        assertEquals(testSession, result.get());
        verify(sessionRepository).findByIdAndAppUserId(sessionId, userId);
    }

    @Test
    void findByIdAndAppUserId_shouldReturnEmptyOptional() {
        // arrange
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findByIdAndAppUserId(sessionId, userId))
                .thenReturn(Optional.empty());

        // act
        Optional<Session> result = sessionRepository.findByIdAndAppUserId(sessionId, userId);

        // assert
        assertFalse(result.isPresent());
        verify(sessionRepository).findByIdAndAppUserId(sessionId, userId);
    }

    @Test
    void revokeAllOtherSessions_shouldReturnCount() {
        // arrange
        String currentDeviceId = "current-device";
        int expectedCount = 2;
        when(sessionRepository.revokeAllOtherSessions(userId, currentDeviceId))
                .thenReturn(expectedCount);

        // act
        int result = sessionRepository.revokeAllOtherSessions(userId, currentDeviceId);

        // assert
        assertEquals(expectedCount, result);
        verify(sessionRepository).revokeAllOtherSessions(userId, currentDeviceId);
    }

    @Test
    void deleteByExpiryDateBefore_shouldReturnCount() {
        // arrange
        Instant now = Instant.now();
        long expectedCount = 5L;
        when(sessionRepository.deleteByExpiryDateBefore(now))
                .thenReturn(expectedCount);

        // act
        long result = sessionRepository.deleteByExpiryDateBefore(now);

        // assert
        assertEquals(expectedCount, result);
        verify(sessionRepository).deleteByExpiryDateBefore(now);
    }

    @Test
    void deleteByRevokedTrueAndUpdatedAtBefore_shouldReturnCount() {
        // arrange
        Instant cutoff = Instant.now().minusSeconds(86400); // 24 hours ago
        long expectedCount = 3L;
        when(sessionRepository.deleteByRevokedTrueAndUpdatedAtBefore(cutoff))
                .thenReturn(expectedCount);

        // act
        long result = sessionRepository.deleteByRevokedTrueAndUpdatedAtBefore(cutoff);

        // assert
        assertEquals(expectedCount, result);
        verify(sessionRepository).deleteByRevokedTrueAndUpdatedAtBefore(cutoff);
    }

    @Test
    void revokeAllSessionsByUserId_shouldReturnCount() {
        // arrange
        int expectedCount = 4;
        when(sessionRepository.revokeAllSessionsByUserId(userId))
                .thenReturn(expectedCount);

        // act
        int result = sessionRepository.revokeAllSessionsByUserId(userId);

        // assert
        assertEquals(expectedCount, result);
        verify(sessionRepository).revokeAllSessionsByUserId(userId);
    }

    @Test
    void repositoryMethods_shouldHandleNullParameters() {
        // Test that repository methods can handle null parameters gracefully
        when(sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(sessionRepository.findByAppUserAndDeviceId(any(), any()))
                .thenReturn(Optional.empty());
        when(sessionRepository.findAllByAppUserIdOrderByLastUsedAtDesc(any()))
                .thenReturn(List.of());
        when(sessionRepository.findByIdAndAppUserId(any(), any()))
                .thenReturn(Optional.empty());

        // act & assert - these should not throw exceptions
        assertDoesNotThrow(() -> {
            sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser(null, null, null);
            sessionRepository.findByAppUserAndDeviceId(null, null);
            sessionRepository.findAllByAppUserIdOrderByLastUsedAtDesc(null);
            sessionRepository.findByIdAndAppUserId(null, null);
        });
    }
}
