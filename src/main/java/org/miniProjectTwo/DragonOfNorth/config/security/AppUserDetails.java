package org.miniProjectTwo.DragonOfNorth.config.security;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom implementation of Spring Security's {@link UserDetails} that represents
 * an authenticated user in the application. This class wraps an {@link AppUser} entity
 * and provides the necessary user details required by Spring Security.
 */

@NamedInterface
@RequiredArgsConstructor
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
        return List.of();
        // todo add roles
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
        if (StringUtils.isNotBlank(appUser.getEmail())){
            return appUser.getEmail();
        }
        return appUser.getPhoneNumber();
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
        return UserDetails.super.isAccountNonExpired();
    }

    /**
     * Indicates whether the user is locked or unlocked.
     * TODO: Implement proper account locking logic.
     *
     * @return true if the user is not locked, false otherwise
     */

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     * TODO: Implement proper credentials expiration logic.
     *
     * @return true if the user's credentials are valid (non-expired), false otherwise
     */

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    /**
     * Indicates whether the user is enabled or disabled.
     * TODO: Implement proper user enabling/disabling logic.
     *
     * @return true if the user is enabled, false otherwise
     */

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
