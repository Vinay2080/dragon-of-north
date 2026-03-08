package org.miniProjectTwo.DragonOfNorth.services.auth;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl.PhoneAuthenticationServiceImpl;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.ACTIVE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType.PHONE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneAuthenticationServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthCommonServices authCommonServices;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private Counter counter;
    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;

    @Mock
    private AuditEventLogger auditEventLogger;


    @InjectMocks
    private PhoneAuthenticationServiceImpl phoneAuthenticationService;


    private final String phoneNumber = "9838291289";

    @Test
    void supports_ShouldReturnPHONE_WhenCalled() {
        // act
        IdentifierType type = phoneAuthenticationService.supports();

        // assert
        assertEquals(PHONE, type, "PhoneAuthenticationServiceImpl should support PHONE identifier type");

    }

    @Test
    void getUserStatus_ShouldReturnStatus_WhenUserExists() {

        when(meterRegistry.counter(anyString())).thenReturn(counter);
        //arrange
        AppUser appUser = new AppUser();
        appUser.setPhone(phoneNumber);
        appUser.setAppUserStatus(ACTIVE);
        appUser.setId(java.util.UUID.randomUUID());

        when(appUserRepository.findByPhone(phoneNumber)).thenReturn(Optional.of(appUser));
        when(userAuthProviderRepository.findAllByUserId(appUser.getId())).thenReturn(List.of());

        //act
        AppUserStatusFinderResponse response = phoneAuthenticationService.getUserStatus(phoneNumber);

        //assert
        assertNotNull(response, "status should be returned upon calling this method");
        assertTrue(response.exists());
        assertEquals(ACTIVE, response.appUserStatus(), "user status should be ACTIVE");

        //verify
        verify(appUserRepository).findByPhone(phoneNumber);

    }

    @Test
    void getUserStatus_ShouldReturnNotFound_WhenUserDoesNotExists() {

        when(meterRegistry.counter(anyString())).thenReturn(counter);
        //arrange
        when(appUserRepository.findByPhone(phoneNumber)).thenReturn(Optional.empty());

        //act
        AppUserStatusFinderResponse response = phoneAuthenticationService.getUserStatus(phoneNumber);

        //assert
        assertNotNull(response, "status should be returned upon calling this method");
        assertFalse(response.exists());
        assertNull(response.appUserStatus());

        //verify
        verify(appUserRepository).findByPhone(phoneNumber);

    }


    //Use ArgumentCaptor ONLY when ALL 3 are true
    //
    //A method receives an object
    //
    //Your code mutates/builds that object
    //
    //You must verify what’s inside that object

    @Test
    void signUpUser_ShouldReturnStatusACTIVE_AndSaveUser_WhenCalled() {

        when(meterRegistry.counter(anyString())).thenReturn(counter);
        //arrange
        String password = "encoded@Password123";
        AppUserSignUpRequest request = new AppUserSignUpRequest(phoneNumber, PHONE, password);

        AppUser appUser = new AppUser();
        appUser.setPhone(request.identifier());
        appUser.setPassword(request.password());
        appUser.setAppUserStatus(ACTIVE);
        appUser.setId(java.util.UUID.randomUUID());

        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(appUser);
        when(appUserRepository.findByPhone(request.identifier())).thenReturn(Optional.of(appUser));
        when(userAuthProviderRepository.findAllByUserId(appUser.getId())).thenReturn(List.of());

        //act
        AppUserStatusFinderResponse response = phoneAuthenticationService.signUpUser(request);

        //assert
        assertNotNull(response, "response should not be null");
        assertEquals(ACTIVE, response.appUserStatus(), "user status should be ACTIVE");

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
        assertEquals(ACTIVE, capturedUser.getAppUserStatus(), "Status should be created");

        verify(userAuthProviderRepository).save(any(UserAuthProvider.class));
        verify(auditEventLogger).log("auth.signup", null, null, null, "success", "identifier_type=PHONE", null);
        verify(auditEventLogger, never()).log(eq("auth.signup"), isNull(), isNull(), isNull(), eq("failure"), anyString(), isNull());

    }

    @Test
    void completeSignUp_shouldSetEmailVerifiedTrue_andAssignDefaultUSER_whenCalledWithValidPhoneNumber() {

        when(meterRegistry.counter(anyString())).thenReturn(counter);
        //arrange
        AppUser appUser = new AppUser();
        appUser.setId(java.util.UUID.randomUUID());
        appUser.setPhone(phoneNumber);
        appUser.setAppUserStatus(ACTIVE);

        when(appUserRepository.findByPhone(phoneNumber)).thenReturn(Optional.of(appUser));
        when(userAuthProviderRepository.findAllByUserId(appUser.getId())).thenReturn(List.of());

        //act
        AppUserStatusFinderResponse response = phoneAuthenticationService.completeSignUp(phoneNumber);

        //assert
        assertEquals(ACTIVE, response.appUserStatus(), "method should return status ACTIVE for valid input (i.e. valid phone number, user exists and user status is ACTIVE");

        //verify
        verify(authCommonServices).assignDefaultRole(appUser);
        verify(appUserRepository).save(appUser);
        verify(auditEventLogger).log("auth.signup.complete", appUser.getId(), null, null, "success", "identifier_type=PHONE", null);
    }

    @Test
    void completeSignUp_returnUSER_NOT_FOUND_whenCalledWithInvalidPhoneNumber() {

        when(meterRegistry.counter(anyString())).thenReturn(counter);
        //arrange
        when(appUserRepository.findByPhone(phoneNumber)).thenReturn(Optional.empty());

        assertThrows(
                BusinessException.class,
                () -> phoneAuthenticationService.completeSignUp(phoneNumber)
        );

        //verify
        verify(appUserRepository, never()).save(any());
        verify(authCommonServices, never()).assignDefaultRole(any());

        verify(auditEventLogger).log(eq("auth.signup.complete"), isNull(), isNull(), isNull(), eq("failure"), anyString(), isNull());
    }
}
