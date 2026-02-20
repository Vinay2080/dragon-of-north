package org.miniProjectTwo.DragonOfNorth.config.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * Custom implementation of Spring Security's {@link UserDetails} that represents
 * an authenticated user in the application. This class wraps an {@link AppUser} entity
 * and provides the necessary user details required by Spring Security.
 */

@NamedInterface
@RequiredArgsConstructor
@Getter
public class AppUserDetails implements UserDetails {


    private final AppUser appUser;

    /**
     * Returns the authorities granted to the user.
     * Currently returns an empty list. TODO: Implement role-based authorities.
     *
     * @return a collection of granted authorities
     */

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Set<GrantedAuthority> authorities = new HashSet<>();

        for (Role role : appUser.getRoles()) {
            authorities.add(
                    new SimpleGrantedAuthority
                            ("ROLE_" + role.getRoleName().name()));

            role.getPermissions().forEach(permission ->
                    authorities.add(
                            new SimpleGrantedAuthority("PERM_" + permission.getName())
                    ));
        }
        return authorities;
    }

    /**
     * Returns the password used to authenticate the user.
     *
     * @return the password
     */

    @Override
    public @Nullable String getPassword() {
        return appUser.getPassword();
    }

    @Override
    @NullMarked
    public String getUsername() {
        if (StringUtils.hasText(appUser.getEmail())) {
            return appUser.getEmail();
        }
        if (StringUtils.hasText(appUser.getPhone())) {
            return appUser.getPhone();
        }
        return appUser.getId().toString();
    }
    // todo following methods need to return specific values will be defined later...

    /**
     * Indicates whether the user's account has expired.
     * TODO: Implement proper account expiration logic.
     *
     * @return true if the user's account is valid (non-expired), false otherwise
     */

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked.
     * TODO: Implement proper account locking logic.
     *
     * @return true if the user is not locked, false otherwise
     */

    @Override
    public boolean isAccountNonLocked() {
        return appUser.getFailedLoginAttempts() < 5;
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     * TODO: Implement proper credentials expiration logic.
     *
     * @return true if the user's credentials are valid (non-expired), false otherwise
     */

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled.
     * TODO: Implement proper user enabling/disabling logic.
     *
     * @return true if the user is enabled, false otherwise
     */

    @Override
    public boolean isEnabled() {
        return true;
    }
}