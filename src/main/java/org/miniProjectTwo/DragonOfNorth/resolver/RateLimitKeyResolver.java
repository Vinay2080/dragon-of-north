package org.miniProjectTwo.DragonOfNorth.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.miniProjectTwo.DragonOfNorth.enums.RateLimitType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RateLimitKeyResolver {

    public String resolve(HttpServletRequest request, RateLimitType type) {
        String identifier = extractIdentifier(request);
        return type.name() + ":" + identifier;
    }

    private String extractIdentifier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If authenticated, use userId from JWT
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() != null
                && !authentication.getPrincipal().equals("anonymousUser")) {
            return "user:" + authentication.getName();
        }

        // Fallback to IP for unauthenticated requests
        return "ip:" + getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
