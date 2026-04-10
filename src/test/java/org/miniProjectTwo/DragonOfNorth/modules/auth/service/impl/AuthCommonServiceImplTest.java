package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordChangeRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpService;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.Provider;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
class AuthCommonServiceImplTest {

    @InjectMocks
    private AuthCommonServiceImpl authCommonService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtServices jwtServices;

    @Mock
    private SessionService sessionService;

    @Mock
    private OtpService otpService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditEventLogger auditEventLogger;

    @Mock
    private UserStateValidator userStateValidator;

    @Mock
    private ProfileService profileService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assignDefaultRole_shouldAssignUserRole_whenUserHasNoRoles() {
        // arrange
        AppUser appUser = new AppUser();
        appUser.setRoles(new HashSet<>());
        Role userRole = new Role();
        userRole.setRoleName(RoleName.USER);

        when(roleRepository.findByRoleName(RoleName.USER)).thenReturn(Optional.of(userRole));

        // act
        authCommonService.assignDefaultRole(appUser);

        // assert
        assertEquals(1, appUser.getRoles().size());
        assertTrue(appUser.getRoles().contains(userRole));

        // verify
        verify(roleRepository).findByRoleName(RoleName.USER);
    }

    @Test
    void assignDefaultRole_shouldNotAssignRole_whenUserAlreadyHasRoles() {
        // arrange
        AppUser appUser = new AppUser();
        Role existingRole = new Role();
        existingRole.setRoleName(RoleName.ADMIN);
        appUser.setRoles(Set.of(existingRole));

        // act
        authCommonService.assignDefaultRole(appUser);

        // assert
        assertEquals(1, appUser.getRoles().size());
        assertTrue(appUser.getRoles().contains(existingRole));

        // verify
        verify(roleRepository, never()).findByRoleName(any());
    }

    @Test
    void assignDefaultRole_shouldThrowException_whenRoleNotFound() {
        // arrange
        AppUser appUser = new AppUser();
        appUser.setRoles(new HashSet<>());

        when(roleRepository.findByRoleName(RoleName.USER)).thenReturn(Optional.empty());

        // act & assert
        BusinessException exception = assertThrows(BusinessException.class, () -> authCommonService.assignDefaultRole(appUser));
        assertEquals(ErrorCode.ROLE_NOT_FOUND, exception.getErrorCode());

        // verify
        verify(roleRepository).findByRoleName(RoleName.USER);
    }

    @Test
    void updateUserStatus_shouldSetProvidedStatus() {
        // arrange
        AppUser appUser = new AppUser();
        appUser.setAppUserStatus(ACTIVE);

        // act
        authCommonService.updateUserStatus(LOCKED, appUser);

        // assert
        assertEquals(LOCKED, appUser.getAppUserStatus());
    }

    @Test
    void login_shouldThrowEmailNotVerified_whenEmailIsNotVerified() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");

        Authentication authentication = mock(Authentication.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Counter failureCounter = mock(Counter.class);
        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");

        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), Provider.LOCAL)).thenReturn(true);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new AppUserDetails(user));
        when(meterRegistry.counter(anyString())).thenReturn(failureCounter);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authCommonService.login("user@example.com", "Secret@123", response, context));

        assertEquals(ErrorCode.EMAIL_NOT_VERIFIED, exception.getErrorCode());
        verify(sessionService, never()).createSession(any(), anyString(), anyString(), anyString(), anyString());
        verify(meterRegistry).counter("auth.login.failure");
        verify(auditEventLogger).log(eq("auth.login"), eq(user.getId()), eq("device-1"), eq("127.0.0.1"), eq("failure"), argThat(msg -> msg != null && msg.toLowerCase().contains("not verified")), eq("req-1"));
    }

    @Test
    void login_shouldNormalizeEmailBeforeLookup_whenEmailHasWhitespaceAndUppercase() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setEmailVerified(true);
        user.setRoles(Set.of());

        Authentication authentication = mock(Authentication.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Counter successCounter = mock(Counter.class);
        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");

        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), Provider.LOCAL)).thenReturn(true);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new AppUserDetails(user));
        when(jwtServices.generateAccessToken(any(), any())).thenReturn("access");
        when(jwtServices.generateRefreshToken(any())).thenReturn("refresh");
        when(meterRegistry.counter(anyString())).thenReturn(successCounter);

        authCommonService.login(" USER@EXAMPLE.COM ", "Secret@123", response, context);

        verify(appUserRepository).findByEmail("user@example.com");
        verify(authenticationManager).authenticate(argThat(token ->
                token instanceof UsernamePasswordAuthenticationToken up &&
                        "user@example.com".equals(up.getPrincipal())
        ));
    }

    @Test
    void changePassword_shouldRejectGoogleOnlyAccounts() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setPassword("encoded-password");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, Set.of()));
        SecurityContextHolder.setContext(context);

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.LOCAL)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authCommonService.changePassword(new PasswordChangeRequest("Old@12345", "New@12345")));

        assertEquals(ErrorCode.PASSWORD_CHANGE_NOT_ALLOWED, exception.getErrorCode());
        assertEquals("Password change not allowed for Google accounts", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(appUserRepository, never()).save(any());
        verify(sessionService, never()).revokeAllSessionsByUserId(any());
    }

    @Test
    void changePassword_shouldRejectIncorrectCurrentPassword() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setPassword("encoded-password");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, Set.of()));
        SecurityContextHolder.setContext(context);

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.LOCAL)).thenReturn(true);
        when(passwordEncoder.matches("Wrong@123", "encoded-password")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authCommonService.changePassword(new PasswordChangeRequest("Wrong@123", "New@12345")));

        assertEquals(ErrorCode.INVALID_CURRENT_PASSWORD, exception.getErrorCode());
        assertEquals("Current password is incorrect", exception.getMessage());
        verify(appUserRepository, never()).save(any());
        verify(sessionService, never()).revokeAllSessionsByUserId(any());
    }

    @Test
    void changePassword_shouldRejectSamePasswordAsCurrentPassword() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setPassword("encoded-password");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, Set.of()));
        SecurityContextHolder.setContext(context);

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.LOCAL)).thenReturn(true);
        when(passwordEncoder.matches("Same@1234", "encoded-password")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authCommonService.changePassword(new PasswordChangeRequest("Same@1234", "Same@1234")));

        assertEquals(ErrorCode.SAME_PASSWORD, exception.getErrorCode());
        assertEquals("New password must be different from current password", exception.getMessage());
        verify(appUserRepository, never()).save(any());
        verify(sessionService, never()).revokeAllSessionsByUserId(any());
    }

    @Test
    void refreshToken_shouldValidateUserStateBeforeRotation() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Counter successCounter = mock(Counter.class);
        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");

        when(jwtServices.extractUserId("old-refresh")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtServices.generateRefreshToken(userId)).thenReturn("new-refresh");
        when(sessionService.validateAndRotateSession("old-refresh", "new-refresh", "device-1")).thenReturn(userId);
        when(roleRepository.findRolesById(userId)).thenReturn(Set.of());
        when(jwtServices.refreshAccessToken("new-refresh", Set.of())).thenReturn("new-access");
        when(meterRegistry.counter(anyString())).thenReturn(successCounter);

        authCommonService.refreshToken("old-refresh", response, context);

        verify(userStateValidator).validate(user, UserLifecycleOperation.SESSION_ROTATE_REFRESH);
        verify(sessionService).validateAndRotateSession("old-refresh", "new-refresh", "device-1");
    }

    @Test
    void logoutUser_shouldValidateUserStateBeforeSessionRevoke() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Counter successCounter = mock(Counter.class);
        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");

        when(jwtServices.extractUserId("refresh-token")).thenReturn(userId);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(meterRegistry.counter(anyString())).thenReturn(successCounter);

        authCommonService.logoutUser("refresh-token", response, context);

        verify(userStateValidator).validate(user, UserLifecycleOperation.SESSION_REVOKE_CURRENT);
        verify(sessionService).revokeSession("refresh-token", "device-1");
    }

    @Test
    void deleteAccount_shouldValidateStateAndSoftDelete() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setAppUserStatus(ACTIVE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Counter successCounter = mock(Counter.class);
        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, Set.of()));
        SecurityContextHolder.setContext(securityContext);

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(meterRegistry.counter(anyString())).thenReturn(successCounter);

        authCommonService.deleteAccount(response, context);

        assertEquals(DELETED, user.getAppUserStatus());
        verify(userStateValidator).validate(user, UserLifecycleOperation.ACCOUNT_DELETION);
        verify(profileService, never()).deleteProfileImage(any());
        verify(appUserRepository).save(user);
        verify(sessionService).revokeAllSessionsByUserId(userId);
    }

    @Test
    void deleteAccount_shouldNotDeleteWhenStateValidationFails() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, Set.of()));
        SecurityContextHolder.setContext(securityContext);

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        doThrow(new BusinessException(ErrorCode.USER_BLOCKED))
                .when(userStateValidator).validate(user, UserLifecycleOperation.ACCOUNT_DELETION);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authCommonService.deleteAccount(response, context));

        assertEquals(ErrorCode.USER_BLOCKED, exception.getErrorCode());
        verify(appUserRepository, never()).save(any());
        verify(sessionService, never()).revokeAllSessionsByUserId(any());
        verify(profileService, never()).deleteProfileImage(any());
    }
}
