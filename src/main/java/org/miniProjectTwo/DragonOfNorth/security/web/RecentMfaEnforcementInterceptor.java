package org.miniProjectTwo.DragonOfNorth.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaProperties;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaService;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.SecurityAuditEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RecentMfaEnforcementInterceptor implements HandlerInterceptor {

    private final RecentMfaService recentMfaService;
    private final RecentMfaProperties recentMfaProperties;
    private final AuditEventLogger auditEventLogger;

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
        if (!recentMfaService.isRecentMfaSatisfied(principal.mfaVerifiedAt(), recentMfaProperties.getMfaMaxAge())) {
            auditEventLogger.log(SecurityAuditEvent.AUTH_MFA_STEPUP_REQUIRED, principal.userId(), null, request.getRemoteAddr(), "failure", "recent_mfa_stale_or_missing", null);
            recentMfaService.requireRecentMfa(principal.mfaVerifiedAt(), recentMfaProperties.getMfaMaxAge());
        }
        return true;
    }

    private boolean requiresRecentMfa(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(RequireRecentMfa.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireRecentMfa.class);
    }
}
