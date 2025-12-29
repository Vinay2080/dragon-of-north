package org.miniProjectTwo.DragonOfNorth.impl.auth;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.config.security.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.config.security.JwtServices;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.RefreshTokenRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AuthenticationResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.RefreshTokenResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.exception.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
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

    @Override
    public AuthenticationResponse login(String identifier, String password) {
        final Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, password));
        AppUserDetails appUserDetails = (AppUserDetails) authentication.getPrincipal();
        assert appUserDetails != null;
        AppUser appUser = appUserDetails.getAppUser();

        assert appUser != null;
        final String accessToken = jwtServices.generateAccessToken(appUser.getId(), appUser.getRoles());
        final String refreshToken = jwtServices.generateRefreshToken(appUser.getId());
        final String tokenType = "Bearer";
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(tokenType)
                .build();
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        UUID uuid = jwtServices.extractUserId(request.refreshToken());
        Set<Role> roles = appUserRepository.findRolesById(uuid);
        final String newAccessToken = jwtServices.refreshAccessToken(request.refreshToken(), roles);
        final String tokenType = "Bearer";
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType(tokenType)
                .build();
    }


    @Override
    public void assignDefaultRole(AppUser appUser) {
        if (!appUser.hasAnyRoles()) {
            Role userRole = roleRepository.findByRoleName(RoleName.USER)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, RoleName.USER.toString()));
            appUser.setRoles(Set.of(userRole));
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

}
