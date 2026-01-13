package org.miniProjectTwo.DragonOfNorth.impl.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.CREATED;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.VERIFIED;
import static org.miniProjectTwo.DragonOfNorth.enums.IdentifierType.EMAIL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailAuthenticationServiceImplTest {

    @InjectMocks
    private EmailAuthenticationServiceImpl emailAuthenticationService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthCommonServices authCommonServices;

    private final String email = "test@mockito.com";

    @Test
    void support_ShouldReturnEMAIL_whenCalled() {
        //act
        IdentifierType identifierType = emailAuthenticationService.supports();

        //assert
        assertEquals(EMAIL, identifierType, "support method should return type EMAIL");
    }

    @Test
    void getUserStatus_shouldReturnCREATED_whenCalledWithValidEmail() {

        // arrange
        AppUserStatus appUserStatus = CREATED;

        when(appUserRepository.findAppUserStatusByEmail(email)).thenReturn(Optional.of(appUserStatus));

        //act
        AppUserStatusFinderResponse response = emailAuthenticationService.getUserStatus(email);

        //
        assertNotNull(response, "returned object cannot be null");
        assertEquals(appUserStatus, response.appUserStatus(), "method should be returning status CREATED if called with valid email");

        //verify
        verify(appUserRepository).findAppUserStatusByEmail(email);
    }

    @Test
    void getUserStatus_shouldReturnNOT_EXISTS_whenCalledWithInvalidEmail() {

        //arrange
        when(appUserRepository.findAppUserStatusByEmail(email)).thenReturn(Optional.empty());

        //act
        AppUserStatusFinderResponse response = emailAuthenticationService.getUserStatus(email);

        //assert
        assertNotNull(response, "response object should not be null");
        assertEquals(AppUserStatus.NOT_EXIST, response.appUserStatus(), "if the user does not exists method should return status NOT_EXISTS");

        //verify
        verify(appUserRepository).findAppUserStatusByEmail(email);
    }


    @Test
    void signUpUser_shouldSaveUserWithEncodedPassword_AndSetUserStatusAsCREATED_whenCalled() {

        //arrange
        String password = "encoded@Password123";
        AppUserSignUpRequest request = new AppUserSignUpRequest(email, EMAIL, password);

        AppUser appUser = new AppUser();
        appUser.setEmail(request.identifier());
        appUser.setPassword(request.password());
        appUser.setAppUserStatus(CREATED);

        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(appUserRepository.findAppUserStatusByEmail(request.identifier())).thenReturn(Optional.of(CREATED));
        when(appUserRepository.save(any(AppUser.class))).thenReturn(appUser);

        //act
        AppUserStatusFinderResponse response = emailAuthenticationService.signUpUser(request);

        //assert
        assertNotNull(response, "method response should not be null");
        assertEquals(CREATED, response.appUserStatus(), "method should return status CREATED");

        //verify
        verify(passwordEncoder).encode(request.password());

        ArgumentCaptor<AppUser> argumentCaptor = ArgumentCaptor.forClass(AppUser.class);

        verify(appUserRepository).save(argumentCaptor.capture());

        AppUser capturedUser = argumentCaptor.getValue();

        assertEquals("encodedPassword", capturedUser.getPassword(), "password must be encoded before saving to the database");
        assertEquals(request.identifier(), capturedUser.getEmail(), "saved email and received email should be same");
        assertEquals(CREATED, capturedUser.getAppUserStatus(), "user status should be CREATED once the user is saved");


        verify(appUserRepository).findAppUserStatusByEmail(request.identifier());
    }

    @Test
    void completeSignUp_shouldUpdateUserStatusToVERIFIED_andShouldAssignDefaultUSER_whenCalledWithInvalidEmail() {

        //arrange

        AppUser appUser = new AppUser();

        appUser.setEmail(email);
        appUser.setAppUserStatus(CREATED);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(appUser));
        when(appUserRepository.findAppUserStatusByEmail(appUser.getEmail())).thenReturn(Optional.of(VERIFIED));

        //act
        AppUserStatusFinderResponse response = emailAuthenticationService.completeSignUp(email);

        //assert
        assertNotNull(response, "method response should not be null");
        assertEquals(VERIFIED, response.appUserStatus(), "method should return status VERIFIED when called");

        //
        verify(authCommonServices).updateUserStatus(CREATED, appUser);
        verify(authCommonServices).assignDefaultRole(appUser);
        verify(appUserRepository).save(appUser);

    }

    @Test
    void completeSignUp_shouldThrowErrorUSER_NOT_FOUND_ForUserThatDoesNotExists_whenCalledWithInvalidEmail() {

        //arrange
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        //act + assert
        assertThrows(BusinessException.class,
                () -> emailAuthenticationService.completeSignUp(email));

        //verify
        verify(appUserRepository, never()).save(any());
        verify(appUserRepository, never()).findAppUserStatusByEmail(any());
    }

}