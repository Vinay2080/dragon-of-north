package org.miniProjectTwo.DragonOfNorth.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaProperties;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.security.service.SessionAccessTokenIssuer;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.SecurityAuditEvent;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecentMfaEnforcementInterceptor implements HandlerInterceptor {

    private final RecentMfaService recentMfaService;
    private final RecentMfaProperties recentMfaProperties;
    private final AuditEventLogger auditEventLogger;
    private final AppUserRepository appUserRepository;
    private final SessionRepository sessionRepository;
    private final SessionAccessTokenIssuer sessionAccessTokenIssuer;
    private final RoleRepository roleRepository;
    private final AuthCommonServices authCommonServices;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (!requiresRecentMfa(handlerMethod)) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityPrincipal principal)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "User not authenticated");
        }
        RequireRecentMfa policy = resolvePolicy(handlerMethod);
        if (policy == null) {
            return true;
        }
        Session session = resolveLiveSession(principal);
        if (policy.onlyWhenMfaEnabled() && !isMfaEnabled(principal.userId())) {
            return true;
        }

        Duration maxAge = recentMfaProperties.resolveMaxAge(policy.policy());
        Instant jwtVerifiedAt = principal.mfaVerifiedAt();
        boolean jwtFresh = recentMfaService.isRecentMfaSatisfied(jwtVerifiedAt, maxAge);
        Instant sessionVerifiedAt = session.getMfaVerifiedAt();
        boolean sessionFresh = recentMfaService.isRecentMfaSatisfied(sessionVerifiedAt, maxAge);
        if (!sessionFresh) {
            auditEventLogger.log(SecurityAuditEvent.AUTH_MFA_STEPUP_REQUIRED, principal.userId(), null, request.getRemoteAddr(), "failure", "recent_mfa_stale_or_missing", null);
            recentMfaService.requireRecentMfa(sessionVerifiedAt, maxAge);
        }

        refreshAccessTokenIfStale(jwtFresh, sessionFresh, session, principal.userId(), response);
        return true;
    }

    private Session resolveLiveSession(SecurityPrincipal principal) {
        UUID sessionId = principal.sessionId();
        UUID userId = principal.userId();
        if (sessionId == null || userId == null) {
            auditEventLogger.log(SecurityAuditEvent.AUTH_SESSION_BINDING_FAILURE, principal.userId(), null, null, "failure", "sid_missing", null);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Session ID missing from token claims");
        }
        Instant now = Instant.now();
        return sessionRepository.findLiveByIdAndAppUserId(sessionId, userId, now)
                .orElseThrow(() -> {
                    auditEventLogger.log(SecurityAuditEvent.AUTH_SESSION_SUSPICIOUS, userId, null, null, "failure", "sid_not_live", null);
                    return new BusinessException(ErrorCode.INVALID_TOKEN, "Session not found or no longer live");
                });
    }

    private void refreshAccessTokenIfStale(boolean jwtFresh,
                                           boolean sessionFresh,
                                           Session session,
                                           UUID userId,
                                           HttpServletResponse response) {
        if (jwtFresh || !sessionFresh) {
            return;
        }
        String refreshedToken = sessionAccessTokenIssuer.mintAccessToken(session, roleRepository.findRolesById(userId));
        authCommonServices.setAccessToken(response, refreshedToken);
    }

    private boolean requiresRecentMfa(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), RequireRecentMfa.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), RequireRecentMfa.class);
    }

    private RequireRecentMfa resolvePolicy(HandlerMethod handlerMethod) {
        RequireRecentMfa methodPolicy = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireRecentMfa.class);
        if (methodPolicy != null) {
            return methodPolicy;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireRecentMfa.class);
    }

    private boolean isMfaEnabled(java.util.UUID userId) {
        return appUserRepository.findById(userId)
                .map(user -> user.isMfaEnabled())
                .orElse(false);
    }
}
