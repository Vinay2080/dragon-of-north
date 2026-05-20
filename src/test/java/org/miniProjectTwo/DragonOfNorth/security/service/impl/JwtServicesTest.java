package org.miniProjectTwo.DragonOfNorth.security.service.impl;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.security.service.AuthnFacts;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JwtServicesTest {

    private JwtServices jwtServices;

    private static KeyPaths ensureLocalKeysExist() throws Exception {
        Path keysDir = Files.createTempDirectory("dragon-of-north-keys-jwt-test");
        Path privateKeyPath = keysDir.resolve("private_key.pem");
        Path publicKeyPath = keysDir.resolve("public_key.pem");

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        String privatePem = toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
        String publicPem = toPem("PUBLIC KEY", keyPair.getPublic().getEncoded());

        Files.writeString(privateKeyPath, privatePem, StandardCharsets.UTF_8);
        Files.writeString(publicKeyPath, publicPem, StandardCharsets.UTF_8);

        privateKeyPath.toFile().deleteOnExit();
        publicKeyPath.toFile().deleteOnExit();
        keysDir.toFile().deleteOnExit();

        return new KeyPaths(privateKeyPath, publicKeyPath);
    }

    @BeforeEach
    void setUp() throws Exception {
        KeyPaths paths = ensureLocalKeysExist();
        jwtServices = new JwtServicesImpl(paths.privateKeyPath().toString(), paths.publicKeyPath().toString());
        ReflectionTestUtils.setField(jwtServices, "accessTokenExpiration", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtServices, "refreshTokenExpiration", 86400000L); // 24 hours
    }

    private record KeyPaths(Path privateKeyPath, Path publicKeyPath) {
    }

    private static String toPem(String type, byte[] derBytes) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(derBytes);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
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
    void generateAccessToken_withAuthnFacts_shouldIncludeMfaClaims() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant verifiedAt = Instant.parse("2026-05-20T00:00:00Z");

        AuthnFacts facts = new AuthnFacts(
                userId,
                List.of(RoleName.USER.name()),
                true,
                verifiedAt,
                List.of("pwd"),
                sessionId
        );

        String token = jwtServices.generateAccessToken(facts);

        Claims claims = jwtServices.extractAllClaims(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("access_token", claims.get("token_type"));
        assertEquals(true, claims.get("mfa_verified"));
        assertEquals("pwd", ((List<?>) claims.get("amr")).getFirst());
        assertEquals(sessionId.toString(), claims.get("sid"));

        assertTrue(jwtServices.extractMfaVerified(token));
        assertEquals(verifiedAt, jwtServices.extractMfaVerifiedAt(token));
        assertEquals(List.of("pwd"), jwtServices.extractAmr(token));
        assertEquals(sessionId, jwtServices.extractSessionId(token));
    }

    @Test
    void extractMfaClaims_shouldTolerateMissingClaims() {
        UUID userId = UUID.randomUUID();
        Set<Role> roles = Collections.emptySet();

        String token = jwtServices.generateAccessToken(userId, roles);

        assertFalse(jwtServices.extractMfaVerified(token));
        assertEquals(List.of(), jwtServices.extractAmr(token));
        assertNull(jwtServices.extractMfaVerifiedAt(token));
        assertNull(jwtServices.extractSessionId(token));
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
