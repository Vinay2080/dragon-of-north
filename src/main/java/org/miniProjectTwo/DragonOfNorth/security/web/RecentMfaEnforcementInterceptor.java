package org.miniProjectTwo.DragonOfNorth.security.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.StepUpRequiredResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaProperties;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.security.service.SessionAccessTokenIssuer;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.SecurityAuditEvent;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Interceptor to enforce Multi-Factor Authentication (MFA) for recent login attempts.
 */
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
    private final ObjectMapper objectMapper;

    /**
     * Pre-handle method to enforce recent MFA verification for applicable requests. Checks for the presence of the @RequireRecentMfa annotation on the handler method or class, and if present, verifies that the authenticated user's last MFA verification time satisfies the policy requirements. If the session's MFA verification is stale, issues a step-up challenge response. If the JWT's MFA verification is stale but the session is fresh, refreshes the access token to update claims.
     */
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
        AppUser user = resolveUser(principal.userId());

        if (policy.onlyWhenMfaEnabled() && !user.isMfaEnabled()) {
            return true;
        }

        Duration maxAge = recentMfaProperties.resolveMaxAge(policy.policy());
        Instant jwtVerifiedAt = principal.mfaVerifiedAt();
        boolean jwtFresh = recentMfaService.isRecentMfaSatisfied(jwtVerifiedAt, maxAge);
        Instant sessionVerifiedAt = session.getMfaVerifiedAt();
        boolean sessionFresh = recentMfaService.isRecentMfaSatisfied(sessionVerifiedAt, maxAge);
        if (!sessionFresh) {
            auditEventLogger.log(SecurityAuditEvent.AUTH_MFA_STEPUP_REQUIRED, principal.userId(), null, request.getRemoteAddr(), "failure", "recent_mfa_stale_or_missing", null);
            return issueStepUpChallenge(request, response, user, session);
        }

        refreshAccessTokenIfStale(jwtFresh, sessionFresh, session, principal.userId(), response);
        return true;
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

    /**
     * Resolves the live session for the authenticated principal. Validates that the session ID from the principal maps to an active session in the repository. If the session is missing or not live, logs an audit event and throws an exception to indicate an invalid token.
     */
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

    private AppUser resolveUser(UUID userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));
    }

    /**
     * Issues a step-up MFA challenge and returns a 403 response with challenge details.
     */
    private boolean issueStepUpChallenge(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AppUser user,
                                         Session session) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, resolveDeviceId(request, session));
        MfaChallenge challenge = authCommonServices.issueStepUpChallenge(user, session.getId(), context);
        StepUpRequiredResponse payload = StepUpRequiredResponse.from(ErrorCode.MFA_STEP_UP_REQUIRED, challenge);
        response.setStatus(ErrorCode.MFA_STEP_UP_REQUIRED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            objectMapper.writeValue(response.getWriter(), ApiResponse.failed(payload));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.MFA_STEP_UP_REQUIRED, "Recent MFA verification required");
        }
        return false;
    }

    private String resolveDeviceId(HttpServletRequest request, Session session) {
        String headerDeviceId = request.getHeader("X-Device-Id");
        if (headerDeviceId != null && !headerDeviceId.isBlank()) {
            return headerDeviceId;
        }
        return session.getDeviceId();
    }

    /**
     * If the JWT's MFA verification is stale but the session's MFA verification is fresh, issues a new access token with updated claims to avoid forcing an unnecessary step-up challenge on later requests.
     */
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
}
