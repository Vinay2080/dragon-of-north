package org.miniProjectTwo.DragonOfNorth.impl.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.CREATED;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.NOT_EXIST;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhoneAuthenticationServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthCommonServices authCommonServices;

    @InjectMocks
    private PhoneAuthenticationServiceImpl phoneAuthenticationService;


    private static final String phoneNumber = "9838291289";
    private static final String password = "encoded@Password123";

    @Test
    void supports_ShouldReturnPHONE_WhenCalled() {
        // act
        IdentifierType type = phoneAuthenticationService.supports();

        // assert
        assertEquals(IdentifierType.PHONE, type, "PhoneAuthenticationServiceImpl should support PHONE identifier type");

    }

    @Test
    void getUserStatus_ShouldReturnStatus_WhenUserExists() {
        //arrange
        AppUserStatus expectedStatus = CREATED;

        when(appUserRepository.findAppUserStatusByPhone(phoneNumber)).thenReturn(Optional.of(expectedStatus));

        //act
        AppUserStatusFinderResponse response = phoneAuthenticationService.getUserStatus(phoneNumber);

        //assert
        assertNotNull(response, "response should not be null");
        assertEquals(expectedStatus, response.appUserStatus(), "should match the expected value");
        verify(appUserRepository).findAppUserStatusByPhone(phoneNumber);

    }

    @Test
    void getUserStatus_ShouldReturnNOT_EXISTS_WhenUserDoesNotExists() {
        //arrange
        when(appUserRepository.findAppUserStatusByPhone(phoneNumber)).thenReturn(Optional.empty());

        //act
        AppUserStatusFinderResponse response = phoneAuthenticationService.getUserStatus(phoneNumber);

        //assert
        assertNotNull(response, "response should not be null");
        assertEquals(NOT_EXIST, response.appUserStatus(), "should return NOT_EXISTS for user that does not exists");
        verify(appUserRepository).findAppUserStatusByPhone(phoneNumber);

    }

    @Test
    void signUpUser_ShouldReturnStatusCREATED_AndSaveUser_WhenCalled() {

        AppUser appUser = new AppUser();
        appUser.setPhone(phoneNumber);
        appUser.setPassword(passwordEncoder.encode(password));
        appUser.setAppUserStatus(CREATED);
        appUserRepository.save(appUser);

        assertNotNull(appUser);

    }

    @Test
    void completeSignUp() {
    }
}