package org.miniProjectTwo.DragonOfNorth.modules.session.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.SessionCreationSpec;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

        user.setMfaEnabled(false);
        SessionCreationSpec spec = SessionCreationSpec.fromAppUser(user, "pwd");

        sessionService.createSession(user, "refresh", "127.0.0.1", "device-1", "ua", spec);

        verify(sessionRepository).delete(existing);
        verify(sessionRepository).flush();
        verify(sessionRepository).save(argThat(session ->
                !session.isMfaRequired()
                        && session.getMfaVerifiedAt() == null
                        && "pwd".equals(session.getPrimaryAmr())
        ));
    }

    @Test
    void createSession_shouldLeaveMfaUnverified_whenMfaRequired() {
        ReflectionTestUtils.setField(sessionService, "refreshTokenDurationMs", 60000L);

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setMfaEnabled(true);

        when(tokenHasher.hashToken("refresh")).thenReturn("hash");
        when(sessionRepository.findByAppUserAndDeviceId(user, "device-1")).thenReturn(Optional.empty());
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionCreationSpec spec = SessionCreationSpec.fromAppUser(user, "pwd");
        sessionService.createSession(user, "refresh", "127.0.0.1", "device-1", "ua", spec);

        verify(sessionRepository).save(argThat(session ->
                session.isMfaRequired()
                        && session.getMfaVerifiedAt() == null
                        && "pwd".equals(session.getPrimaryAmr())
        ));
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
        assertEquals("d1", result.getFirst().deviceId());
    }

    @Test
    void revokeAllOtherSessions_shouldThrow_whenDeviceIdBlank() {
        when(meterRegistry.counter(any())).thenReturn(counter);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.revokeAllOtherSessions(UUID.randomUUID(), " "));

        assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
        verify(auditEventLogger).log(eq("session.revoke.others"), any(UUID.class), eq(" "), isNull(), eq("failure"), eq("device ID missing"), isNull());

    }

    @Test
    void validateAndRotateSession_shouldThrow_whenSessionRevoked() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        when(jwtServices.extractUserId("old")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("old")).thenReturn("oldHash");
        when(tokenHasher.hashToken("new")).thenReturn("newHash");
        when(sessionRepository.rotateRefreshTokenIfActive(eq(userId), eq("d1"), eq("oldHash"), eq("newHash"), any(Instant.class), any(Instant.class)))
                .thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.validateAndRotateSession("old", "new", "d1"));

        assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    void validateAndRotateSession_shouldRotateHash_whenValid() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        when(jwtServices.extractUserId("old")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("old")).thenReturn("oldHash");
        when(tokenHasher.hashToken("new")).thenReturn("newHash");
        when(sessionRepository.rotateRefreshTokenIfActive(eq(userId), eq("d1"), eq("oldHash"), eq("newHash"), any(Instant.class), any(Instant.class)))
                .thenReturn(1);
        Session rotated = new Session();
        rotated.setAppUser(user);
        rotated.setId(UUID.randomUUID());
        when(sessionRepository.findLiveByAppUserIdAndDeviceIdAndRefreshTokenHash(eq(userId), eq("d1"), eq("newHash"), any(Instant.class)))
                .thenReturn(Optional.of(rotated));

        Session actual = sessionService.validateAndRotateSession("old", "new", "d1");

        assertEquals(rotated, actual);
        verify(sessionRepository).rotateRefreshTokenIfActive(eq(userId), eq("d1"), eq("oldHash"), eq("newHash"), any(Instant.class), any(Instant.class));
    }

    @Test
    void revokeSession_shouldLogSuccess_whenSessionExists() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        when(jwtServices.extractUserId("refresh")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("refresh")).thenReturn("hash");
        when(sessionRepository.revokeCurrentSessionIfActive(userId, "device1", "hash")).thenReturn(1);
        when(meterRegistry.counter(any())).thenReturn(counter);

        sessionService.revokeSession("refresh", "device1");

        // Remove this line - save() is not explicitly called
        // verify(sessionRepository).save(session);

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
        when(sessionRepository.revokeCurrentSessionIfActive(userId, "device1", "hash")).thenReturn(0);
        when(sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser("hash", "device1", user)).thenReturn(Optional.empty());
        when(meterRegistry.counter(any())).thenReturn(counter);

        sessionService.revokeSession("refresh", "device1");

        verify(auditEventLogger).log("session.revoke.current", userId, "device1", null, "failure", "session not found", null);
    }

    @Test
    void revokeSession_shouldBeIdempotent_whenAlreadyRevoked() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        when(jwtServices.extractUserId("refresh")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("refresh")).thenReturn("hash");
        when(sessionRepository.revokeCurrentSessionIfActive(userId, "device1", "hash")).thenReturn(0);
        when(sessionRepository.findByRefreshTokenHashAndDeviceIdAndAppUser("hash", "device1", user)).thenReturn(Optional.of(new Session()));

        sessionService.revokeSession("refresh", "device1");

        verify(auditEventLogger).log("session.revoke.current", userId, "device1", null, "success", "already_revoked", null);
    }

    @Test
    void revokeAllOtherSessions_shouldLogSuccess_whenSessionsExist() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        when(meterRegistry.counter(any())).thenReturn(counter);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sessionRepository.revokeAllOtherSessions(userId, "currentDevice")).thenReturn(3);

        int result = sessionService.revokeAllOtherSessions(userId, "currentDevice");

        assertEquals(3, result);
        verify(userStateValidator).validate(user, UserLifecycleOperation.SESSION_REVOKE_OTHERS);
        verify(auditEventLogger).log("session.revoke.others", userId, "currentDevice", null, "success", "revoked_count=3", null);
    }

    @Test
    void revokeSession_shouldThrow_whenUserStateNotAllowed() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        when(jwtServices.extractUserId("refresh")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        doThrow(new BusinessException(ErrorCode.USER_BLOCKED))
                .when(userStateValidator).validate(user, UserLifecycleOperation.SESSION_REVOKE_CURRENT);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.revokeSession("refresh", "device1"));

        assertEquals(ErrorCode.USER_BLOCKED, ex.getErrorCode());
    }

    @Test
    void validateAndRotateSession_shouldAllowExactlyOneWinnerUnderConcurrency() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        when(jwtServices.extractUserId("old")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("old")).thenReturn("oldHash");
        when(tokenHasher.hashToken("new")).thenReturn("newHash");

        AtomicBoolean cas = new AtomicBoolean(false);
        when(sessionRepository.rotateRefreshTokenIfActive(eq(userId), eq("d1"), eq("oldHash"), eq("newHash"), any(Instant.class), any(Instant.class)))
                .thenAnswer(invocation -> cas.compareAndSet(false, true) ? 1 : 0);
        Session rotated = new Session();
        rotated.setAppUser(user);
        when(sessionRepository.findLiveByAppUserIdAndDeviceIdAndRefreshTokenHash(eq(userId), eq("d1"), eq("newHash"), any(Instant.class)))
                .thenReturn(Optional.of(rotated));

        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            CountDownLatch ready = new CountDownLatch(8);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger(0);

            List<Future<Boolean>> futures = java.util.stream.IntStream.range(0, 8)
                    .mapToObj(i -> pool.submit(() -> {
                        ready.countDown();
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            return false;
                        }
                        try {
                            sessionService.validateAndRotateSession("old", "new", "d1");
                            return true;
                        } catch (BusinessException e) {
                            return false;
                        }
                    }))
                    .toList();

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            for (Future<Boolean> future : futures) {
                if (future.get(5, TimeUnit.SECONDS)) successCount.incrementAndGet();
            }

            assertEquals(1, successCount.get());

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void revokeCurrentSession_vs_refreshRotation_shouldBeDeterministicWhenRevokeWins() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        when(jwtServices.extractUserId("refresh")).thenReturn(userId);
        when(jwtServices.extractUserId("old")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("refresh")).thenReturn("oldHash");
        when(tokenHasher.hashToken("old")).thenReturn("oldHash");
        when(tokenHasher.hashToken("new")).thenReturn("newHash");
        when(sessionRepository.revokeCurrentSessionIfActive(userId, "d1", "oldHash")).thenReturn(1);
        when(meterRegistry.counter(any())).thenReturn(counter);

        AtomicBoolean revoked = new AtomicBoolean(false);
        when(sessionRepository.rotateRefreshTokenIfActive(eq(userId), eq("d1"), eq("oldHash"), eq("newHash"), any(Instant.class), any(Instant.class)))
                .thenAnswer(invocation -> revoked.get() ? 0 : 1);

        sessionService.revokeSession("refresh", "d1");
        revoked.set(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.validateAndRotateSession("old", "new", "d1"));
        assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    void revokeAll_vs_refreshRotation_shouldRejectRotationAfterRevokeAllWins() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        when(jwtServices.extractUserId("old")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("old")).thenReturn("oldHash");
        when(tokenHasher.hashToken("new")).thenReturn("newHash");
        when(sessionRepository.revokeAllSessionsByUserId(userId)).thenReturn(2);
        when(meterRegistry.counter(any())).thenReturn(counter);

        AtomicBoolean allRevoked = new AtomicBoolean(false);
        when(sessionRepository.rotateRefreshTokenIfActive(eq(userId), eq("d1"), eq("oldHash"), eq("newHash"), any(Instant.class), any(Instant.class)))
                .thenAnswer(invocation -> allRevoked.get() ? 0 : 1);

        sessionService.revokeAllSessionsByUserId(userId);
        allRevoked.set(true);

        assertThrows(BusinessException.class, () -> sessionService.validateAndRotateSession("old", "new", "d1"));
    }

    @Test
    void concurrentRefreshAttemptsAfterRevoke_shouldAllFail() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        when(jwtServices.extractUserId("old")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("old")).thenReturn("oldHash");
        when(tokenHasher.hashToken("new")).thenReturn("newHash");
        when(sessionRepository.rotateRefreshTokenIfActive(eq(userId), eq("d1"), eq("oldHash"), eq("newHash"), any(Instant.class), any(Instant.class)))
                .thenReturn(0);

        try (ExecutorService pool = Executors.newFixedThreadPool(6)) {
            CountDownLatch ready = new CountDownLatch(6);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger(0);

            List<Future<Boolean>> futures = java.util.stream.IntStream.range(0, 6)
                    .mapToObj(i -> pool.submit(() -> {
                        ready.countDown();
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            return false;
                        }
                        try {
                            sessionService.validateAndRotateSession("old", "new", "d1");
                            return true;
                        } catch (BusinessException e) {
                            return false;
                        }
                    }))
                    .toList();

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            for (Future<Boolean> f : futures) if (f.get(5, TimeUnit.SECONDS)) successCount.incrementAndGet();
            assertEquals(0, successCount.get());
        }
    }

    @Test
    void concurrentExpiredRefreshAttempts_shouldAllFailDeterministically() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        when(jwtServices.extractUserId("old")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken("old")).thenReturn("oldHash");
        when(tokenHasher.hashToken("new")).thenReturn("newHash");
        when(sessionRepository.rotateRefreshTokenIfActive(eq(userId), eq("d1"), eq("oldHash"), eq("newHash"), any(Instant.class), any(Instant.class)))
                .thenReturn(0);

        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            List<Future<Boolean>> futures = java.util.stream.IntStream.range(0, 4)
                    .mapToObj(i -> pool.submit(() -> {
                        try {
                            sessionService.validateAndRotateSession("old", "new", "d1");
                            return true;
                        } catch (BusinessException e) {
                            return false;
                        }
                    }))
                    .toList();
            for (Future<Boolean> f : futures) {
                assertFalse(f.get(5, TimeUnit.SECONDS));
            }
        }
    }

    // ------------------------------------------------------------------ refreshMfaVerifiedAt

    @Test
    void refreshMfaVerifiedAt_shouldReturnUpdatedSession_whenSessionIsLive() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant verifiedAt = Instant.now();

        AppUser user = new AppUser();
        user.setId(userId);

        Session session = new Session();
        session.setId(sessionId);
        session.setAppUser(user);

        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(sessionRepository.refreshMfaVerifiedAt(eq(sessionId), eq(userId), eq(verifiedAt), eq("totp"), any(Instant.class)))
                .thenReturn(1);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        Session result = sessionService.refreshMfaVerifiedAt(sessionId, userId, verifiedAt, "totp");

        assertNotNull(result);
        assertEquals(sessionId, result.getId());
        verify(sessionRepository).refreshMfaVerifiedAt(eq(sessionId), eq(userId), eq(verifiedAt), eq("totp"), any(Instant.class));
        verify(sessionRepository).findById(sessionId);
        verify(auditEventLogger).log(eq("session.mfa.step_up"), eq(userId), any(), any(), eq("success"), any(), any());
    }

    @Test
    void refreshMfaVerifiedAt_shouldThrowBusinessException_whenSessionNotFound() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant verifiedAt = Instant.now();

        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(sessionRepository.refreshMfaVerifiedAt(eq(sessionId), eq(userId), eq(verifiedAt), eq("totp"), any(Instant.class)))
                .thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.refreshMfaVerifiedAt(sessionId, userId, verifiedAt, "totp"));

        assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
        verify(sessionRepository).refreshMfaVerifiedAt(eq(sessionId), eq(userId), eq(verifiedAt), eq("totp"), any(Instant.class));
        verify(sessionRepository, never()).findById(any());
    }
}
