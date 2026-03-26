package org.miniProjectTwo.DragonOfNorth.infrastructure.audit;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the current auditor for JPA auditing fields.
 *
 * <p>Uses the Spring Security principal when available, otherwise falls back to {@code SYSTEM}.</p>
 */
@NullMarked
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final Logger log = LoggerFactory.getLogger(AuditorAwareImpl.class);
    @SuppressWarnings("unused")
    private final AppUserRepository appUserRepository;


    /**
     * Returns the identity to store in audit columns.
     *
     * @return authenticated identifier, or {@code SYSTEM} when no user is authenticated
     */
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
        Object principal = authentication.getPrincipal();

        if (principal instanceof AppUserDetails appUserDetails) {
            String email = appUserDetails.getAppUser().getEmail();
            if (StringUtils.hasText(email)) {
                log.debug("Auditor resolved from AppUserDetails email");
                return Optional.of(email);
            }
            String phone = appUserDetails.getAppUser().getPhone();
            if (StringUtils.hasText(phone)) {
                log.debug("Auditor resolved from AppUserDetails phone");
                return Optional.of(phone);
            }
        }
        if (principal instanceof UUID userId) {
            log.debug("Auditor resolved directly from UUID principal");
            return Optional.of(userId.toString());
        }

        if (principal instanceof String rawPrincipal && StringUtils.hasText(rawPrincipal) && !"anonymousUser".equals(rawPrincipal)) {
            try {
                UUID parsed = UUID.fromString(rawPrincipal);
                log.debug("Auditor resolved from UUID authentication name");
                return Optional.of(parsed.toString());
            } catch (IllegalArgumentException ignored) {
                // Keep fallback for non-UUID principals.
            }
        }

        String fallbackPrincipal = authentication.getName();
        log.debug("Auditor resolved from authentication name");
        return Optional.of(fallbackPrincipal);

    }
}
