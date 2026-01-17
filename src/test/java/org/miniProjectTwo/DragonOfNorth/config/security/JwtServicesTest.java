package org.miniProjectTwo.DragonOfNorth.config.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtServicesTest {

    private JwtServices jwtServices;

    @BeforeEach
    void setUp() throws Exception {
        jwtServices = new JwtServices();
        ReflectionTestUtils.setField(jwtServices, "accessTokenExpiration", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtServices, "refreshTokenExpiration", 86400000L); // 24 hours
    }

    @Test
    void generateAccessToken_shouldCreateValidToken() {
        // arrange
        UUID userId = UUID.randomUUID();
        Set<Role> roles = new HashSet<>();
        Role role = new Role();
        role.setRoleName(RoleName.USER);
        roles.add(role);

        // act
        String token = jwtServices.generateAccessToken(userId, roles);

        // assert
        assertNotNull(token);
        Claims claims = jwtServices.extractAllClaims(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("access_token", claims.get("token_type"));
        assertNotNull(claims.get("roles"));
    }

    @Test
    void generateRefreshToken_shouldCreateValidToken() {
        // arrange
        UUID userId = UUID.randomUUID();

        // act
        String token = jwtServices.generateRefreshToken(userId);

        // assert
        assertNotNull(token);
        Claims claims = jwtServices.extractAllClaims(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("refresh_token", claims.get("token_type"));
    }

    @Test
    void extractUserId_shouldReturnCorrectId() {
        // arrange
        UUID userId = UUID.randomUUID();
        String token = jwtServices.generateRefreshToken(userId);

        // act
        UUID extractedId = jwtServices.extractUserId(token);

        // assert
        assertEquals(userId, extractedId);
    }

    @Test
    void refreshAccessToken_shouldGenerateNewAccessToken_whenRefreshTokenIsValid() {
        // arrange
        UUID userId = UUID.randomUUID();
        String refreshToken = jwtServices.generateRefreshToken(userId);
        Set<Role> roles = Collections.emptySet();

        // act
        String accessToken = jwtServices.refreshAccessToken(refreshToken, roles);

        // assert
        assertNotNull(accessToken);
        Claims claims = jwtServices.extractAllClaims(accessToken);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("access_token", claims.get("token_type"));
    }
}
