package org.miniProjectTwo.DragonOfNorth.services;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.miniProjectTwo.DragonOfNorth.config.security.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


/**
 * Service implementation for loading user-specific data.
 * <p>
 * This service is responsible for retrieving user information from the database
 * and converting it into a format that Spring Security can understand.
 * It implements Spring Security's {@link UserDetailsService} to integrate with
 * the authentication process.
 * </p>
 *
 * <p>Key features:
 * <ul>
 *   <li>Loads user details by username (email or phone number)</li>
 *   <li>Throws {@link UsernameNotFoundException} if user is not found</li>
 *   <li>Wraps the {@link AppUser} entity in a Spring Security {@link UserDetails} implementation</li>
 * </ul>
 * </p>
 *
 * @see UserDetailsService
 * @see AppUserDetails
 * @see AppUser
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailService implements UserDetailsService {

    private final AppUserRepository repository;
    /**
     * Locates the user based on the provided username (email or phone number).
     * <p>
     * This method is called by Spring Security during the authentication process.
     * It performs a case-insensitive search for a user by either email or phone number.
     * </p>
     *
     * @param username the username (email or phone number) identifying the user whose data is required
     * @return a fully populated user record (never {@code null})
     * @throws UsernameNotFoundException if the user could not be found
     */
    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        AppUser appUser = repository.findByEmailIgnoreCaseOrPhoneNumberIgnoreCase(username, username);
        if (appUser == null) throw new UsernameNotFoundException("User not found");
        return new AppUserDetails(appUser);

    }
}
