package org.miniProjectTwo.DragonOfNorth.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.config.security.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserDetailServiceTest {

    @InjectMocks
    private AppUserDetailService appUserDetailService;

    @Mock
    private AppUserRepository repository;

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenEmailIsProvidedAndUserExists() {
        // arrange
        String email = "test@example.com";
        AppUser appUser = new AppUser();
        appUser.setId(UUID.randomUUID());
        appUser.setEmail(email);

        when(repository.findByEmail(email)).thenReturn(Optional.of(appUser));

        // act
        UserDetails userDetails = appUserDetailService.loadUserByUsername(email);

        // assert
        assertNotNull(userDetails);
        assertEquals(appUser.getId().toString(), userDetails.getUsername());
        assertInstanceOf(AppUserDetails.class, userDetails);
        assertEquals(appUser, ((AppUserDetails) userDetails).getAppUser());

        // verify
        verify(repository).findByEmail(email);
        verify(repository, never()).findByPhone(anyString());
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenPhoneIsProvidedAndUserExists() {
        // arrange
        String phone = "1234567890";
        AppUser appUser = new AppUser();
        appUser.setId(UUID.randomUUID());
        appUser.setPhone(phone);

        when(repository.findByPhone(phone)).thenReturn(Optional.of(appUser));

        // act
        UserDetails userDetails = appUserDetailService.loadUserByUsername(phone);

        // assert
        assertNotNull(userDetails);
        assertEquals(appUser.getId().toString(), userDetails.getUsername());

        // verify
        verify(repository).findByPhone(phone);
        verify(repository, never()).findByEmail(anyString());
    }

    @Test
    void loadUserByUsername_shouldThrowUsernameNotFoundException_whenUserDoesNotExist() {
        // arrange
        String email = "nonexistent@example.com";
        when(repository.findByEmail(email)).thenReturn(Optional.empty());

        // act & assert
        assertThrows(UsernameNotFoundException.class, () -> appUserDetailService.loadUserByUsername(email));

        // verify
        verify(repository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_shouldThrowUsernameNotFoundException_whenUserDoesNotExists(){
        //arrange
        String phone = "9922240924";
        when(repository.findByPhone(phone)).thenReturn(Optional.empty());

        //act & assert
        assertThrows(UsernameNotFoundException.class,
                ()-> appUserDetailService.loadUserByUsername(phone));

        //verify
        verify(repository).findByPhone(phone);


    }

}
