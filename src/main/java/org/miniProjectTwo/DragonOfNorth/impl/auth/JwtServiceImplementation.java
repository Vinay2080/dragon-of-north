package org.miniProjectTwo.DragonOfNorth.impl.auth;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.config.security.JwtServices;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.RefreshTokenRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AuthenticationResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.RefreshTokenResponse;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class JwtServiceImplementation {

    private final AuthenticationManager authenticationManager;
    private final JwtServices jwtServices;
    private final AppUserRepository appUserRepository;

    public AuthenticationResponse login(String identifier, String password) {
        final Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, password));
        final AppUser appUser = (AppUser) authentication.getPrincipal();

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

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        UUID uuid = jwtServices.extractUserId(request.refreshToken());
        Set<Role> roles = appUserRepository.findRolesById(uuid);
        final String newAccessToken = jwtServices.refreshAccessToken(request.refreshToken(), roles);
        final String tokenType = "Bearer";
        return RefreshTokenResponse.builder().accessToken(newAccessToken).tokenType(tokenType).build();
    }
}
