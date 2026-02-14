package org.miniProjectTwo.DragonOfNorth.services.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.config.security.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.config.security.JwtServicesImpl;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.RefreshTokenService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthCommonServiceRevocationTest {

    @InjectMocks
    private AuthCommonServiceImpl authCommonService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtServicesImpl jwtServices;


    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Test
    void login_shouldCreateNewToken_whenExistingTokensFound() {
        // arrange
        String identifier = "test@example.com";
        String password = "password";
        AppUser appUser = new AppUser();
        appUser.setId(UUID.randomUUID());
        appUser.setRoles(new HashSet<>());


        AppUserDetails appUserDetails = new AppUserDetails(appUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(appUserDetails, password);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);


        when(jwtServices.generateAccessToken(any(UUID.class), anySet()))
                .thenReturn("access-token");
        when(jwtServices.generateRefreshToken(any(UUID.class)))
                .thenReturn("refresh-token");

        // act
        authCommonService.login(identifier, password, httpServletResponse);

        // assert
        verify(refreshTokenService).storeRefreshToken(appUser, "refresh-token");
        verify(jwtServices).generateAccessToken(appUser.getId(), appUser.getRoles());
        verify(jwtServices).generateRefreshToken(appUser.getId());
        verify(httpServletResponse, times(2)).addCookie(any(jakarta.servlet.http.Cookie.class));
    }

    @Test
    void login_shouldCreateNewToken_whenNoExistingToken() {
        // arrange
        String identifier = "test@example.com";
        String password = "password";
        AppUser appUser = new AppUser();
        appUser.setId(UUID.randomUUID());
        appUser.setRoles(new HashSet<>());

        AppUserDetails appUserDetails = new AppUserDetails(appUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(appUserDetails, password);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);


        when(jwtServices.generateAccessToken(any(UUID.class), anySet()))
                .thenReturn("access-token");
        when(jwtServices.generateRefreshToken(any(UUID.class)))
                .thenReturn("refresh-token");

        // act
        authCommonService.login(identifier, password, httpServletResponse);

        // assert
        verify(refreshTokenService).storeRefreshToken(appUser, "refresh-token");
        verify(jwtServices).generateAccessToken(appUser.getId(), appUser.getRoles());
        verify(jwtServices).generateRefreshToken(appUser.getId());
        verify(httpServletResponse, times(2)).addCookie(any(jakarta.servlet.http.Cookie.class));
    }

    @Test
    void logoutUser_shouldRevokeToken_andClearCookies() {
        // arrange
        String refreshToken = "refresh-token-value";
        jakarta.servlet.http.Cookie[] cookies = {
                new jakarta.servlet.http.Cookie("refresh_token", refreshToken),
                new jakarta.servlet.http.Cookie("other_cookie", "value")
        };

        when(httpServletRequest.getCookies()).thenReturn(cookies);

        // act
        authCommonService.logoutUser(httpServletRequest, httpServletResponse);

        // assert
        verify(refreshTokenService).revokeTokenByRawToken(refreshToken);
        verify(httpServletResponse).addCookie(argThat(cookie ->
                "refresh_token".equals(cookie.getName()) && cookie.getMaxAge() == 0
        ));
        verify(httpServletResponse).addCookie(argThat(cookie ->
                "access_token".equals(cookie.getName()) && cookie.getMaxAge() == 0
        ));
    }

    @Test
    void logoutUser_shouldThrowException_whenRefreshTokenMissing() {
        // arrange
        jakarta.servlet.http.Cookie[] cookies = {
                new jakarta.servlet.http.Cookie("other_cookie", "value")
        };

        when(httpServletRequest.getCookies()).thenReturn(cookies);

        // act & assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> authCommonService.logoutUser(httpServletRequest, httpServletResponse));
        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());

        // verify
        verify(refreshTokenService, never()).revokeTokenByRawToken(any());
    }

    @Test
    void logoutUser_shouldThrowException_whenNoCookies() {
        // arrange
        when(httpServletRequest.getCookies()).thenReturn(null);

        // act & assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> authCommonService.logoutUser(httpServletRequest, httpServletResponse));
        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());

        // verify
        verify(refreshTokenService, never()).revokeTokenByRawToken(any());
    }
}
