package org.miniProjectTwo.DragonOfNorth.security.filter;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates SID sensitive-route patterns against actual mapped routes to prevent drift.
 *
 * <p>This validator ensures that configured sensitive routes align with the actual routes
 * mapped by the application. It checks for any discrepancies and logs warnings for stale
 * patterns that no longer match any mapped routes.</p>
 */
@Component
@Slf4j
public class SidSensitiveRouteValidator {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final SidEnforcementMode enforcementMode;
    private final List<String> sensitivePatterns;
    private final RequestMappingHandlerMapping handlerMapping;

    public SidSensitiveRouteValidator(
            @Value("${app.security.sid-enforcement.mode:disabled}") String enforcementMode,
            @Value("${app.security.sid-enforcement.sensitive-patterns:/api/v1/sessions/**,/api/v1/auth/password/forgot/reset,/api/v1/auth/password/change,/api/v1/auth/account/delete,/api/v1/auth/enable/mfa/**,/api/v1/auth/step-up/**}") String sensitivePatterns,
            @Qualifier("requestMappingHandlerMapping")
            RequestMappingHandlerMapping handlerMapping
    ) {
        this.enforcementMode = SidEnforcementMode.from(enforcementMode);
        this.sensitivePatterns = Arrays.stream(sensitivePatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isBlank())
                .toList();
        this.handlerMapping = handlerMapping;
    }

    @PostConstruct
    void validatePatterns() {
        if (enforcementMode != SidEnforcementMode.SENSITIVE_ONLY) {
            return;
        }

        Set<String> mappedPaths = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPatternValues().stream())
                .collect(Collectors.toSet());

        List<String> stalePatterns = sensitivePatterns.stream()
                .filter(pattern -> mappedPaths.stream().noneMatch(path -> PATH_MATCHER.match(pattern, path)))
                .toList();

        if (!stalePatterns.isEmpty()) {
            throw new IllegalStateException("Stale SID sensitive route patterns detected: " + stalePatterns);
        }

        log.info("SID sensitive-route patterns validated successfully: {}", sensitivePatterns);
    }
}
