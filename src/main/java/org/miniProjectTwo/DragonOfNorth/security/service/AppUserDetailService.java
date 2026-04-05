package org.miniProjectTwo.DragonOfNorth.security.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.shared.util.IdentifierNormalizer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;


/**
 * Spring Security UserDetailsService for multi-method authentication.
 * <p>
 * Loads user details by email or phone for Spring Security authentication.
 * Supports both identifier types with case-insensitive email lookup.
 * Critical for integrating custom user entities with Spring Security.
 *
 * @see AppUserDetails for Spring Security wrapper
 * @see AppUserRepository for user data access
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailService implements UserDetailsService {

    private final AppUserRepository repository;

    /**
     * Loads user by identifier for Spring Security authentication.
     * <p>
     * Determines identifier type (email vs. phone) by '@' presence.
     * Performs case-insensitive email lookup and exact phone match.
     * Critical for authentication credential validation.
     *
     * @param identifier user email or phone number
     * @return Spring Security UserDetails wrapper
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String normalizedIdentifier = identifier != null && identifier.contains("@")
                ? IdentifierNormalizer.normalizeEmail(identifier)
                : IdentifierNormalizer.normalizePhone(identifier);
        Optional<AppUser> appUser;

        if (normalizedIdentifier != null && normalizedIdentifier.contains("@")) {
            appUser = repository.findByEmail(normalizedIdentifier);
        } else {
            appUser = repository.findByPhone(normalizedIdentifier);
        }

        AppUser user = appUser.orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );

        return new AppUserDetails(user);

    }
}
