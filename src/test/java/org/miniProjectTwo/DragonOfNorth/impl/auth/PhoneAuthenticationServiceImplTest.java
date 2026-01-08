package org.miniProjectTwo.DragonOfNorth.impl.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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


    @Test
    void supports_ShouldReturnPHONE_WhenCalled() {
        IdentifierType type = phoneAuthenticationService.supports();

        assertEquals(IdentifierType.PHONE, type, "PhoneAuthenticationServiceImpl should support PHONE identifier type");

    }

    @Test
    void getUserStatus_ShouldReturnStatus_WhenCalled_OtherwiseNOT_EXISTS() {
        String phoneNumber = "9838291289";
        AppUserStatus expectedStatus = AppUserStatus.CREATED;

        when(appUserRepository.findAppUserStatusByPhone(phoneNumber)).thenReturn(Optional.of(expectedStatus));

        AppUserStatusFinderResponse response = phoneAuthenticationService.getUserStatus(phoneNumber);

        assertNotNull(response, "response should not be null");
        assertEquals(expectedStatus, response.appUserStatus(), "should match the expected value");
        verify(appUserRepository).findAppUserStatusByPhone(phoneNumber);

    }

    @Test
    void signUpUser() {
    }

    @Test
    void completeSignUp() {
    }
}