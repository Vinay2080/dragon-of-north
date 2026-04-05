package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.otp.model.OtpToken;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpService;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.ACTIVE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.PENDING_VERIFICATION;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode.OTP_VERIFICATION_REQUIRED;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType.EMAIL;
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
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private Counter counter;
    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;

    @Mock
    private AuditEventLogger auditEventLogger;

    @Mock
    private ProfileService profileService;
    @Mock
    private OtpService otpService;
    @Mock
    private UserStateValidator userStateValidator;

    private final String email = "test@mockito.com";


    @Test
    void support_ShouldReturnEMAIL_whenCalled() {
        //act
        IdentifierType identifierType = emailAuthenticationService.supports();

        //assert
        assertEquals(EMAIL, identifierType, "support method should return type EMAIL");
    }

    @Test
    void getUserStatus_shouldReturnACTIVE_whenCalledWithValidEmail() {

        when(meterRegistry.counter(anyString())).thenReturn(counter);

        // arrange
        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setAppUserStatus(ACTIVE);
        appUser.setEmailVerified(true);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(appUser));
        when(userAuthProviderRepository.findAllByUserId(appUser.getId())).thenReturn(List.of());

        //act
        AppUserStatusFinderResponse response = emailAuthenticationService.getUserStatus(email);

        //
        assertNotNull(response, "returned object cannot be null");
        assertTrue(response.exists());
        assertEquals(ACTIVE, response.appUserStatus(), "method should be returning status ACTIVE if called with valid email");

        //verify
        verify(appUserRepository).findByEmail(email);
    }

    @Test
    void getUserStatus_shouldReturnNotFound_whenCalledWithInvalidEmail() {

        when(meterRegistry.counter(anyString())).thenReturn(counter);

        //arrange
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        //act
        AppUserStatusFinderResponse response = emailAuthenticationService.getUserStatus(email);

        //assert
        assertNotNull(response, "response object should not be null");
        assertFalse(response.exists());
        assertNull(response.appUserStatus());

        //verify
        verify(appUserRepository).findByEmail(email);
    }


    @Test
    void signUpUser_shouldSaveUserWithEncodedPassword_AndSetUserStatusAsPENDING_VERIFICATION_whenCalled() {

        when(meterRegistry.counter(anyString())).thenReturn(counter);

        //arrange
        String password = "encoded@Password123";
        AppUserSignUpRequest request = new AppUserSignUpRequest(email, EMAIL, password);

        AppUser appUser = new AppUser();
        appUser.setEmail(request.identifier());
        appUser.setPassword(request.password());
        appUser.setAppUserStatus(PENDING_VERIFICATION);
        appUser.setId(java.util.UUID.randomUUID());

        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(appUser);
        when(appUserRepository.findByEmail("test@mockito.com")).thenReturn(Optional.of(appUser));
        when(appUserRepository.findByEmailForUpdate("test@mockito.com")).thenReturn(Optional.empty());
        when(userAuthProviderRepository.findAllByUserId(appUser.getId())).thenReturn(List.of());

        //act
        AppUserStatusFinderResponse response = emailAuthenticationService.signUpUser(request);

        //assert
        assertNotNull(response, "method response should not be null");
        assertEquals(PENDING_VERIFICATION, response.appUserStatus(), "method should return status PENDING_VERIFICATION");

        //verify
        verify(passwordEncoder).encode(request.password());
        verify(appUserRepository).findByEmailForUpdate("test@mockito.com");

        ArgumentCaptor<AppUser> argumentCaptor = ArgumentCaptor.forClass(AppUser.class);

        verify(appUserRepository).save(argumentCaptor.capture());

        AppUser capturedUser = argumentCaptor.getValue();

        assertEquals("encodedPassword", capturedUser.getPassword(), "password must be encoded before saving to the database");
        assertEquals(request.identifier(), capturedUser.getEmail(), "saved email and received email should be same");
        assertEquals(PENDING_VERIFICATION, capturedUser.getAppUserStatus(), "user status should be PENDING_VERIFICATION once the user is saved");

        verify(userAuthProviderRepository).save(any(UserAuthProvider.class));
        verify(profileService, never()).ensureProfileExists(any(UUID.class), any());
        verify(auditEventLogger).log("auth.signup", null, null, null, "success", "identifier_type=EMAIL", null);
        verify(auditEventLogger, never()).log(eq("auth.signup"), isNull(), isNull(), isNull(), eq("failure"), anyString(), isNull());
    }

    @Test
    void completeSignUp_shouldSetEmailVerifiedTrue_andAssignDefaultUSER_whenCalledWithValidEmail() {

        when(meterRegistry.counter(anyString())).thenReturn(counter);

        //arrange
        AppUser appUser = new AppUser();
        appUser.setId(java.util.UUID.randomUUID());
        appUser.setEmail(email);
        appUser.setAppUserStatus(ACTIVE);
        appUser.setEmailVerified(true);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(appUser));
        when(userAuthProviderRepository.findAllByUserId(appUser.getId())).thenReturn(List.of());
        OtpToken otpToken = OtpToken.builder()
                .consumed(true)
                .expiresAt(Instant.now().plusSeconds(60))
                .verifiedAt(Instant.now())
                .build();
        when(otpService.fetchLatest(email, EMAIL, OtpPurpose.SIGNUP)).thenReturn(otpToken);
        when(otpService.fetchLatest(email, EMAIL, OtpPurpose.LOGIN_UNVERIFIED)).thenThrow(new BusinessException(org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode.OTP_NOT_FOUND));

        //act
        AppUserStatusFinderResponse response = emailAuthenticationService.completeSignUp(email);

        //assert
        assertNotNull(response, "method response should not be null");
        assertEquals(ACTIVE, response.appUserStatus(), "method should return status ACTIVE when called");

        //verify
        verify(userStateValidator).validate(appUser, UserLifecycleOperation.LOCAL_SIGNUP_COMPLETE);
        verify(authCommonServices).assignDefaultRole(appUser);
        verify(appUserRepository).save(appUser);
        verify(profileService).ensureProfileExists(appUser.getId(), null);
        verify(auditEventLogger).log("auth.signup.complete", appUser.getId(), null, null, "success", "identifier_type=EMAIL", null);

    }

    @Test
    void completeSignUp_shouldRejectWhenSignupOtpNotVerifiedEvenIfConsumed_whenVerifiedAtMissing() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        AppUser appUser = new AppUser();
        appUser.setId(UUID.randomUUID());
        appUser.setEmail(email);
        appUser.setAppUserStatus(PENDING_VERIFICATION);

        OtpToken otpToken = OtpToken.builder()
                .consumed(true)
                .expiresAt(Instant.now().plusSeconds(60))
                .verifiedAt(null)
                .build();

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(appUser));
        when(otpService.fetchLatest(email, EMAIL, OtpPurpose.SIGNUP)).thenReturn(otpToken);
        when(otpService.fetchLatest(email, EMAIL, OtpPurpose.LOGIN_UNVERIFIED)).thenThrow(new BusinessException(org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode.OTP_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class, () -> emailAuthenticationService.completeSignUp(email));
        assertEquals(OTP_VERIFICATION_REQUIRED, ex.getErrorCode());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void completeSignUp_shouldThrowErrorUSER_NOT_FOUND_ForUserThatDoesNotExists_whenCalledWithInvalidEmail() {

        //arrange
        when(meterRegistry.counter(anyString())).thenReturn(mock(Counter.class));
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        //act + assert
        assertThrows(BusinessException.class,
                () -> emailAuthenticationService.completeSignUp(email));

        //verify
        verify(appUserRepository, never()).save(any());
        verify(profileService, never()).ensureProfileExists(any(UUID.class), any());
        verify(auditEventLogger).log(eq("auth.signup.complete"), isNull(), isNull(), isNull(), eq("failure"), anyString(), isNull());
    }

}
