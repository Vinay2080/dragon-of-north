package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpService;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.Provider;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.ACTIVE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.LOCKED;
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
        when(appUserRepository.isEmailVerified(user.getId())).thenReturn(false);
        when(meterRegistry.counter(anyString())).thenReturn(failureCounter);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                authCommonService.login("user@example.com", "Secret@123", response, context));

        assertEquals(ErrorCode.EMAIL_NOT_VERIFIED, exception.getErrorCode());
        verify(sessionService, never()).createSession(any(), anyString(), anyString(), anyString(), anyString());
        verify(meterRegistry).counter("auth.login.failure");
        verify(auditEventLogger).log(eq("auth.login"), eq(user.getId()), eq("device-1"), eq("127.0.0.1"), eq("failure"), argThat(msg -> msg != null && msg.toLowerCase().contains("not verified")), eq("req-1"));
    }
}
