package org.miniProjectTwo.DragonOfNorth.impl.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.config.security.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.config.security.JwtServices;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.RefreshTokenRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AuthenticationResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.RefreshTokenResponse;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.exception.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.CREATED;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.VERIFIED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthCommonServiceImplTest {

    @InjectMocks
    private AuthCommonServiceImpl authCommonService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtServices jwtServices;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RoleRepository roleRepository;

    @Test
    void login_shouldReturnAuthenticationResponse_whenCredentialsAreValid() {
        // arrange
        String identifier = "test@example.com";
        String password = "password";
        UUID userId = UUID.randomUUID();
        Set<Role> roles = Collections.emptySet();

        Authentication authentication = mock(Authentication.class);
        AppUser appUser = new AppUser();
        appUser.setId(userId);
        appUser.setRoles(roles);
        AppUserDetails appUserDetails = new AppUserDetails(appUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(appUserDetails);
        when(jwtServices.generateAccessToken(userId, roles)).thenReturn("access-token");
        when(jwtServices.generateRefreshToken(userId)).thenReturn("refresh-token");

        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);

        // act
        AuthenticationResponse response = authCommonService.login(identifier, password, httpServletResponse);

        // assert
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());

        // verify
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtServices).generateAccessToken(userId, roles);
        verify(jwtServices).generateRefreshToken(userId);
        verify(httpServletResponse).addCookie(any());
    }

    @Test
    void refreshToken_shouldReturnRefreshTokenResponse_whenTokenIsValid() {
        // arrange
        String refreshToken = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        UUID userId = UUID.randomUUID();
        Set<Role> roles = Collections.emptySet();

        when(jwtServices.extractUserId(refreshToken)).thenReturn(userId);
        when(appUserRepository.findRolesById(userId)).thenReturn(roles);
        when(jwtServices.refreshAccessToken(refreshToken, roles)).thenReturn("new-access-token");

        // act
        RefreshTokenResponse response = authCommonService.refreshToken(request);

        // assert
        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());

        // verify
        verify(jwtServices).extractUserId(refreshToken);
        verify(appUserRepository).findRolesById(userId);
        verify(jwtServices).refreshAccessToken(refreshToken, roles);
    }

    @Test
    void assignDefaultRole_shouldAssignUserRole_whenUserHasNoRoles() {
        // arrange
        AppUser appUser = new AppUser();
        appUser.setRoles(Collections.emptySet());
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
        appUser.setRoles(Collections.emptySet());

        when(roleRepository.findByRoleName(RoleName.USER)).thenReturn(Optional.empty());

        // act & assert
        BusinessException exception = assertThrows(BusinessException.class, () -> authCommonService.assignDefaultRole(appUser));
        assertEquals(ErrorCode.ROLE_NOT_FOUND, exception.getErrorCode());

        // verify
        verify(roleRepository).findByRoleName(RoleName.USER);
    }

    @Test
    void updateUserStatus_shouldUpdateToVerified_whenStatusIsCreated() {
        // arrange
        AppUser appUser = new AppUser();
        appUser.setAppUserStatus(CREATED);

        // act
        authCommonService.updateUserStatus(CREATED, appUser);

        // assert
        assertEquals(VERIFIED, appUser.getAppUserStatus());
    }

    @Test
    void updateUserStatus_shouldThrowException_whenUserAlreadyVerified() {
        // arrange
        AppUser appUser = new AppUser();
        appUser.setAppUserStatus(VERIFIED);

        // act & assert
        BusinessException exception = assertThrows(BusinessException.class, () -> authCommonService.updateUserStatus(CREATED, appUser));
        assertEquals(ErrorCode.USER_ALREADY_VERIFIED, exception.getErrorCode());
    }

    @Test
    void updateUserStatus_shouldThrowException_whenStatusIsNotCreated() {
        // arrange
        AppUser appUser = new AppUser();
        appUser.setAppUserStatus(CREATED);

        // act & assert
        BusinessException exception = assertThrows(BusinessException.class, () -> authCommonService.updateUserStatus(VERIFIED, appUser));
        assertEquals(ErrorCode.STATUS_MISMATCH, exception.getErrorCode());
    }
}