package org.miniProjectTwo.DragonOfNorth.impl.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.config.security.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.config.security.JwtServices;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.exception.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.services.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.CREATED;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.VERIFIED;

@RequiredArgsConstructor
@Service
public class AuthCommonServiceImpl implements AuthCommonServices {

    private final AuthenticationManager authenticationManager;
    private final JwtServices jwtServices;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void login(String identifier, String password, HttpServletResponse response) {
        final Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, password));

        if (authentication.getPrincipal() == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Authentication principal is null");
        }

        if (!(authentication.getPrincipal() instanceof AppUserDetails appUserDetails)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Invalid principal type");
        }

        AppUser appUser = appUserDetails.getAppUser();

        final String accessToken = jwtServices.generateAccessToken(appUser.getId(), appUser.getRoles());
        final String refreshToken = jwtServices.generateRefreshToken(appUser.getId());

        refreshTokenService.storeRefreshToken(appUser, refreshToken);

        setAccessToken(response, accessToken);
        setRefreshCookie(response, refreshToken);

    }

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token missing");
        }

        try {

            refreshTokenService.verifyAndUpdateToken(refreshToken);
            UUID userId = jwtServices.extractUserId(refreshToken);
            Set<Role> roles = appUserRepository.findRolesById(userId);

            String newAccessToken = jwtServices.refreshAccessToken(refreshToken, roles);

            setAccessToken(response, newAccessToken);

        } catch (BusinessException e) {
            clearRefreshTokenCookie(response);
            throw e;
        }
    }



    @Override
    public void assignDefaultRole(AppUser appUser) {
        if (!appUser.hasAnyRoles()) {
            Role userRole = roleRepository.findByRoleName(RoleName.USER).orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, RoleName.USER.toString()));
            appUser.getRoles().add(userRole);
        }
    }

    @Override
    public void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser) {
        if (appUser.getAppUserStatus() == VERIFIED) {
            throw new BusinessException(ErrorCode.USER_ALREADY_VERIFIED);
        }

        if (appUserStatus == CREATED) {
            appUser.setAppUserStatus(VERIFIED);
        } else {
            throw new BusinessException(ErrorCode.STATUS_MISMATCH, CREATED.toString());
        }
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void setAccessToken(HttpServletResponse response, String token) {
        Cookie accessCookie = new Cookie("access_token", token);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 15);
        accessCookie.setAttribute("SameSite", "None");
        response.addCookie(accessCookie);
    }


    private void setRefreshCookie(HttpServletResponse response, String token) {
        Cookie refreshCookie = new Cookie("refresh_token", token);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/jwt/refresh");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    //todo logout
}
