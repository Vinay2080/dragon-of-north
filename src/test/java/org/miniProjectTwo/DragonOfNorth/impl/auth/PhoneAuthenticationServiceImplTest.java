package org.miniProjectTwo.DragonOfNorth.impl.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.*;
import static org.miniProjectTwo.DragonOfNorth.enums.IdentifierType.PHONE;
import static org.mockito.ArgumentMatchers.any;
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
        assertEquals(PHONE, type, "PhoneAuthenticationServiceImpl should support PHONE identifier type");

    }

    @Test
    void getUserStatus_ShouldReturnStatus_WhenUserExists() {
        //arrange
        AppUserStatus expectedStatus = CREATED;

        when(appUserRepository.findAppUserStatusByPhone(phoneNumber)).thenReturn(Optional.of(expectedStatus));

        //act
        AppUserStatusFinderResponse response = phoneAuthenticationService.getUserStatus(phoneNumber);

        //assert
        assertNotNull(response, "status should be returned upon calling this method");
        assertEquals(expectedStatus, response.appUserStatus(), "user status should be CREATED");
        verify(appUserRepository).findAppUserStatusByPhone(phoneNumber);

    }

    @Test
    void getUserStatus_ShouldReturnNOT_EXISTS_WhenUserDoesNotExists() {
        //arrange
        when(appUserRepository.findAppUserStatusByPhone(phoneNumber)).thenReturn(Optional.empty());

        //act
        AppUserStatusFinderResponse response = phoneAuthenticationService.getUserStatus(phoneNumber);

        //assert
        assertNotNull(response, "status should be returned upon calling this method");
        assertEquals(NOT_EXIST, response.appUserStatus(), "should return NOT_EXISTS for user that does not exists");

        //verify
        verify(appUserRepository).findAppUserStatusByPhone(phoneNumber);

    }

    @Test
    void getUserStatus_shouldReturnStatusVERIFIED_whenUserIsAlreadyVerified() {

        // arrange
        AppUserStatus appUserStatus = VERIFIED;
        when(appUserRepository.findAppUserStatusByPhone(phoneNumber)).thenReturn(Optional.of(appUserStatus));

        //act
        AppUserStatusFinderResponse response = phoneAuthenticationService.getUserStatus(phoneNumber);

        //assert
        assertNotNull(response, "status should be returned upon calling this method");
        assertEquals(appUserStatus, response.appUserStatus(), "user status should be VERIFIED");

        //verify
        verify(appUserRepository).findAppUserStatusByPhone(phoneNumber);

    }

    @Test
    void getAppUserStatus_shouldReturnDELETED_whenUserIsDELETED() {

        //arrange
        AppUserStatus appUserStatus = DELETED;

        when(appUserRepository.findAppUserStatusByPhone(phoneNumber)).thenReturn(Optional.of(appUserStatus));

        //act
        AppUserStatusFinderResponse response = phoneAuthenticationService.getUserStatus(phoneNumber);

        //assert
        assertNotNull(response, "status should be returned upon calling this method");
        assertEquals(appUserStatus, response.appUserStatus(), "returned status should be DELETED for the user that is deleted.");

        //verify
        verify(appUserRepository).findAppUserStatusByPhone(phoneNumber);

    }

    //Use ArgumentCaptor ONLY when ALL 3 are true
    //
    //A method receives an object
    //
    //Your code mutates/builds that object
    //
    //You must verify what’s inside that object

    @Test
    void signUpUser_ShouldReturnStatusCREATED_AndSaveUser_WhenCalled() {
        //arrange
        AppUserSignUpRequest request = new AppUserSignUpRequest(phoneNumber, PHONE, password);

        AppUser appUser = new AppUser();
        appUser.setPhone(request.identifier());
        appUser.setPassword(request.password());
        appUser.setAppUserStatus(CREATED);

        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(appUserRepository.findAppUserStatusByPhone(request.identifier())).thenReturn(Optional.of(CREATED));
        when(appUserRepository.save(any(AppUser.class))).thenReturn(appUser);

        //act
        AppUserStatusFinderResponse response = phoneAuthenticationService.signUpUser(request);

        //assert
        assertNotNull(response, "response should not be null");
        assertEquals(CREATED, response.appUserStatus(), "user status should be CREATED");

        //verify
        verify(passwordEncoder).encode(request.password());

        // user ArgumentCaptor when the data/Object that is passed needs to be varified.
        // the object is created in the current method.
        // don't use when a method returns an expected result / dependencies need not be checked.
        ArgumentCaptor<AppUser> userArgumentCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userArgumentCaptor.capture());

        AppUser capturedUser = userArgumentCaptor.getValue();
        assertEquals(request.identifier(), capturedUser.getPhone(), "phone number should match");
        assertEquals("encodedPassword", capturedUser.getPassword(), "password should be encoded");
        assertEquals(CREATED, capturedUser.getAppUserStatus(), "Status should be created");

        verify(appUserRepository).findAppUserStatusByPhone(request.identifier());

    }

    @Test
    void completeSignUp_UpdateUserStatusAndSetRolesAndSaveUser_whenCalledWithValidPhoneNumber() {
        //arrange
        AppUser appUser = new AppUser();
        appUser.setPhone(phoneNumber);
        appUser.setAppUserStatus(CREATED);

        when(appUserRepository.findByPhone(phoneNumber)).thenReturn(Optional.of(appUser));
        when(appUserRepository.findAppUserStatusByPhone(phoneNumber)).thenReturn(Optional.of(VERIFIED));

        //act
        AppUserStatusFinderResponse response = phoneAuthenticationService.completeSignUp(phoneNumber);

        //assert
        assertNotNull(appUser, "method should not return null object (appUser) when called with valid phone number");
        assertEquals(VERIFIED, response.appUserStatus(), "method should return status VERIFIED for valid input (i.e. valid phone number, user exists and user status is CREATED");

        //verify
        verify(authCommonServices).updateUserStatus(appUser.getAppUserStatus(), appUser);
        verify(authCommonServices).assignDefaultRole(appUser);
        verify(appUserRepository).save(appUser);
        verify(appUserRepository).findAppUserStatusByPhone(phoneNumber);
    }

    //todo for user does not exists, ..
}