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

@NamedInterface
@RequiredArgsConstructor
public class AppUserDetails implements UserDetails {


    private final AppUser appUser;

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
        // todo add roles
    }

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
    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
