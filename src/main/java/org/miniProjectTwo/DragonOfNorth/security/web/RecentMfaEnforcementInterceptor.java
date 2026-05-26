package org.miniProjectTwo.DragonOfNorth.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaProperties;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaService;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
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
        recentMfaService.requireRecentMfa(principal.mfaVerifiedAt(), recentMfaProperties.getMfaMaxAge());
        return true;
    }

    private boolean requiresRecentMfa(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(RequireRecentMfa.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireRecentMfa.class);
    }
}
