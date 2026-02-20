package org.miniProjectTwo.DragonOfNorth.config;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.miniProjectTwo.DragonOfNorth.config.security.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

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
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final Logger log = LoggerFactory.getLogger(AuditorAwareImpl.class);
    private final AppUserRepository appUserRepository;


    /**
     * Retrieves the current auditor for JPA auditing operations.
     * Checks the Spring Security context for an authenticated user and returns
     * their username. If no authenticated user is found or the user is anonymous, it
     * returns "SYSTEM" as the fallback auditor for background operations.
     *
     * @return Optional containing the username of the authenticated user, or "SYSTEM" if no user is authenticated
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
                log.debug("Auditor resolved as email from AppUserDetails: {}", email);
                return Optional.of(email);
            }
            String phone = appUserDetails.getAppUser().getPhone();
            if (StringUtils.hasText(phone)) {
                log.debug("Auditor resolved as phone from AppUserDetails: {}", phone);
                return Optional.of(phone);
            }
        }
        if (principal instanceof UUID userId) {
            Optional<String> emailOrPhone = appUserRepository.findById(userId)
                    .map(user -> StringUtils.hasText(user.getEmail()) ? user.getEmail() : user.getPhone())
                    .filter(StringUtils::hasText);

            if (emailOrPhone.isPresent()) {
                log.debug("Auditor resolved via UUID principal lookup: {}", emailOrPhone.get());
                return emailOrPhone;
            }
        }

        String fallbackPrincipal = authentication.getName();
        log.debug("Auditor resolved from authentication name: {}", fallbackPrincipal);
        return Optional.of(fallbackPrincipal);

    }
}
