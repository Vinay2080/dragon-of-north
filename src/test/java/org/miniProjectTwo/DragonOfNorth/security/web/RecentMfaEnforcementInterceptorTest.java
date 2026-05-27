package org.miniProjectTwo.DragonOfNorth.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaProperties;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
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
    private RecentMfaProperties recentMfaProperties;
    private AuditEventLogger auditEventLogger;
    private AppUserRepository appUserRepository;
    private RecentMfaEnforcementInterceptor interceptor;

    @BeforeEach
    void setUp() {
        recentMfaService = mock(RecentMfaService.class);
        recentMfaProperties = mock(RecentMfaProperties.class);
        auditEventLogger = mock(AuditEventLogger.class);
        appUserRepository = mock(AppUserRepository.class);
        interceptor = new RecentMfaEnforcementInterceptor(recentMfaService, recentMfaProperties, auditEventLogger, appUserRepository);
        when(recentMfaProperties.getMfaMaxAge()).thenReturn(Duration.ofMinutes(15));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void firstTimeEnrollment_allowsAuthenticatedUserWithoutMfa() throws Exception {
        UUID userId = UUID.randomUUID();
        SecurityPrincipal principal = new SecurityPrincipal(userId, List.of(), false, null, UUID.randomUUID(), List.of("pwd"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        AppUser user = new AppUser();
        user.setMfaEnabled(false);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));

        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("enrollRequest"));

        boolean allowed = interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), handlerMethod);

        assertTrue(allowed);
        verify(recentMfaService, never()).requireRecentMfa(any(), any());
    }

    @Test
    void firstTimeEnrollment_requiresRecentMfaWhenAlreadyEnabled() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant stale = Instant.now().minus(Duration.ofHours(2));
        SecurityPrincipal principal = new SecurityPrincipal(userId, List.of(), true, stale, UUID.randomUUID(), List.of("pwd", "mfa"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        AppUser user = new AppUser();
        user.setMfaEnabled(true);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recentMfaService.isRecentMfaSatisfied(eq(stale), any())).thenReturn(false);
        doThrow(new BusinessException(ErrorCode.MFA_REQUIRED_STEP_UP, "Recent MFA required"))
                .when(recentMfaService).requireRecentMfa(eq(stale), any());

        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), TestController.class.getMethod("enrollConfirm"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), handlerMethod)
        );

        assertEquals(ErrorCode.MFA_REQUIRED_STEP_UP, exception.getErrorCode());
        verify(recentMfaService).requireRecentMfa(eq(stale), any());
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

    static class TestController {
        @RequireRecentMfa(onlyWhenMfaEnabled = true)
        public void enrollRequest() {}

        @RequireRecentMfa(onlyWhenMfaEnabled = true)
        public void enrollConfirm() {}
    }
}
