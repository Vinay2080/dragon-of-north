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

@Service
@RequiredArgsConstructor
public class AppUserDetailService implements UserDetailsService {

    private final AppUserRepository repository;

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        AppUser appUser = repository.findByEmailIgnoreCaseOrPhoneNumberIgnoreCase(username, username);
        if (appUser == null) throw new UsernameNotFoundException("User not found");
        return new AppUserDetails(appUser);

    }
}
