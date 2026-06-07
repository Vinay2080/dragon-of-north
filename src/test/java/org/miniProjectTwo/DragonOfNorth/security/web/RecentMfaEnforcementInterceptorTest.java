package org.miniProjectTwo.DragonOfNorth.security.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaPolicy;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaProperties;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.security.service.SessionAccessTokenIssuer;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;


import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class RecentMfaEnforcementInterceptorTest {

    private RecentMfaService recentMfaService;
    private AppUserRepository appUserRepository;
    private SessionRepository sessionRepository;
    private SessionAccessTokenIssuer sessionAccessTokenIssuer;
    private AuthCommonServices authCommonServices;
    private RecentMfaEnforcementInterceptor interceptor;


    @BeforeEach
    void setUp() {
        recentMfaService = mock(RecentMfaService.class);
        RecentMfaProperties recentMfaProperties = new RecentMfaProperties();
        recentMfaProperties.setMfaMaxAge(Duration.ofMinutes(15));
        recentMfaProperties.setAccountDeleteMaxAge(Duration.ofSeconds(60));
        AuditEventLogger auditEventLogger = mock(AuditEventLogger.class);
        appUserRepository = mock(AppUserRepository.class);
        sessionRepository = mock(SessionRepository.class);
        sessionAccessTokenIssuer = mock(SessionAccessTokenIssuer.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        authCommonServices = mock(AuthCommonServices.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);

        interceptor = new RecentMfaEnforcementInterceptor(

                recentMfaService,
                recentMfaProperties,
                auditEventLogger,
                appUserRepository,
                sessionRepository,
                sessionAccessTokenIssuer,
                roleRepository,
                authCommonServices,
                objectMapper
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void firstTimeEnrollment_allowsAuthenticatedUserWithoutMfa() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        SecurityPrincipal principal = new SecurityPrincipal(userId, List.of(), false, null, sessionId, List.of("pwd"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        AppUser user = new AppUser();
        user.setMfaEnabled(false);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        Session session = buildSession(sessionId, null);
        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any())).thenReturn(Optional.of(session));

        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("enrollRequest"));

        boolean allowed = interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), handlerMethod);

        assertTrue(allowed);
        verify(recentMfaService, never()).requireRecentMfa(any(), any());
    }

    @Test
    void firstTimeEnrollment_requiresRecentMfaWhenAlreadyEnabled() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant stale = Instant.now().minus(Duration.ofHours(2));
        UUID sessionId = UUID.randomUUID();
        SecurityPrincipal principal = new SecurityPrincipal(userId, List.of(), true, stale, sessionId, List.of("pwd", "mfa"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        AppUser user = new AppUser();
        user.setMfaEnabled(true);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        Session session = buildSession(sessionId, stale);
        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any())).thenReturn(Optional.of(session));
        when(recentMfaService.isRecentMfaSatisfied(eq(stale), any())).thenReturn(false);

        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("enrollConfirm"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        boolean allowed = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(allowed);
        verify(authCommonServices).issueStepUpChallenge(eq(user), eq(sessionId), any());
        verify(response).setStatus(ErrorCode.MFA_STEP_UP_REQUIRED.getHttpStatus().value());
    }

    @Test
    void firstTimeEnrollment_stillRequiresAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("enrollRequest"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), handlerMethod)
        );

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verifyNoInteractions(appUserRepository);
    }

    @Test
    void accountDeletionPolicy_usesStrictWindow() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant verifiedAt = Instant.now().minusSeconds(10);
        SecurityPrincipal principal = new SecurityPrincipal(userId, List.of(), true, verifiedAt, sessionId, List.of("pwd", "mfa"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(buildUser()));
        Session session = buildSession(sessionId, verifiedAt);
        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any())).thenReturn(Optional.of(session));
        when(recentMfaService.isRecentMfaSatisfied(eq(verifiedAt), eq(Duration.ofSeconds(60)))).thenReturn(true);

        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("accountDelete"));

        assertTrue(interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), handlerMethod));
        verify(recentMfaService, never()).requireRecentMfa(any(), any());
        verify(authCommonServices, never()).setAccessToken(any(), any());
    }

    private AppUser buildUser() {
        AppUser user = new AppUser();
        user.setMfaEnabled(true);
        return user;
    }

    @Test
    void jwtStale_sessionFresh_remintsAccessToken() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant jwtStale = Instant.now().minus(Duration.ofMinutes(30));
        Instant sessionFresh = Instant.now().minusSeconds(20);
        SecurityPrincipal principal = new SecurityPrincipal(userId, List.of(), true, jwtStale, sessionId, List.of("pwd", "mfa"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(buildUser()));
        Session session = buildSession(sessionId, sessionFresh);
        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any())).thenReturn(Optional.of(session));
        when(recentMfaService.isRecentMfaSatisfied(eq(jwtStale), any())).thenReturn(false);
        when(recentMfaService.isRecentMfaSatisfied(eq(sessionFresh), any())).thenReturn(true);
        when(sessionAccessTokenIssuer.mintAccessToken(eq(session), any())).thenReturn("refreshed-token");

        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("accountDelete"));

        assertTrue(interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), handlerMethod));
        verify(authCommonServices).setAccessToken(any(), eq("refreshed-token"));
    }

    @Test
    void revokedSession_deniesAccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        SecurityPrincipal principal = new SecurityPrincipal(userId, List.of(), true, Instant.now(), sessionId, List.of("pwd", "mfa"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any())).thenReturn(Optional.empty());

        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("accountDelete"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), handlerMethod)
        );

        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }

    @Test
    void jwtFresh_sessionFresh_doesNotRemintAccessToken() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant verifiedAt = Instant.now().minusSeconds(15);
        SecurityPrincipal principal = new SecurityPrincipal(userId, List.of(), true, verifiedAt, sessionId, List.of("pwd", "mfa"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(buildUser()));
        Session session = buildSession(sessionId, verifiedAt);
        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any())).thenReturn(Optional.of(session));
        when(recentMfaService.isRecentMfaSatisfied(eq(verifiedAt), any())).thenReturn(true);

        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("accountDelete"));

        assertTrue(interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), handlerMethod));
        verify(authCommonServices, never()).setAccessToken(any(), any());
    }

    static class TestController {
        @RequireRecentMfa(onlyWhenMfaEnabled = true)
        public void enrollRequest() {}

        @RequireRecentMfa(onlyWhenMfaEnabled = true)
        public void enrollConfirm() {}

        @SensitiveAccountOperation(policy = RecentMfaPolicy.ACCOUNT_DELETE)
        public void accountDelete() {}
    }

    private Session buildSession(UUID sessionId, Instant mfaVerifiedAt) {
        Session session = new Session();
        session.setId(sessionId);
        session.setMfaVerifiedAt(mfaVerifiedAt);
        return session;
    }

    @Test
    void sessionStale_deniesEvenWhenJwtFresh() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant jwtFresh = Instant.now().minusSeconds(20);
        Instant sessionStale = Instant.now().minus(Duration.ofHours(2));
        SecurityPrincipal principal = new SecurityPrincipal(userId, List.of(), true, jwtFresh, sessionId, List.of("pwd", "mfa"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        AppUser user = buildUser();
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        Session session = buildSession(sessionId, sessionStale);
        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any())).thenReturn(Optional.of(session));
        when(recentMfaService.isRecentMfaSatisfied(eq(jwtFresh), any())).thenReturn(true);
        when(recentMfaService.isRecentMfaSatisfied(eq(sessionStale), any())).thenReturn(false);

        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("accountDelete"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        boolean allowed = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(allowed);
        verify(authCommonServices).issueStepUpChallenge(eq(user), eq(sessionId), any());
        verify(response).setStatus(ErrorCode.MFA_STEP_UP_REQUIRED.getHttpStatus().value());
    }
}
