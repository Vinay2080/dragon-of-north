package org.miniProjectTwo.DragonOfNorth.impl;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.miniProjectTwo.DragonOfNorth.config.security.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
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


        Optional<AppUser> appUser;

        if (identifier.contains("@")) {
            appUser = repository.findByEmail(identifier);
        } else {
            appUser = repository.findByPhone(identifier);
        }

        AppUser user = appUser.orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );

        return new AppUserDetails(user);

    }
}