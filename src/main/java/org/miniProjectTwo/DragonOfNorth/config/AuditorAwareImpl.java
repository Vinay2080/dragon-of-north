package org.miniProjectTwo.DragonOfNorth.config;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Provides the current authenticated user for JPA auditing.
 *
 * <p>This class determines which username should be recorded inside auditing fields
 * such as {@code createdBy} and {@code modifiedBy}. If a request has an authenticated
 * user (via Spring Security), that username is returned. Otherwise, the fallback
 * auditor {@code SYSTEM} is used for internal or background operations.</p>
 *
 * <p>Designed to integrate seamlessly with JWT-based authentication.</p>
 */
@NullMarked
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final Logger log = LoggerFactory.getLogger(AuditorAwareImpl.class);

    @Override
    public Optional<String> getCurrentAuditor() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // No authentication or anonymous user → fallback to SYSTEM
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {

            log.debug("Auditor resolved as SYSTEM (no authenticated user found)");
            return Optional.of("SYSTEM");
        }

        String username = authentication.getName();
        log.debug("Auditor resolved as user: {}", username);

        return Optional.of(username);
    }
}
