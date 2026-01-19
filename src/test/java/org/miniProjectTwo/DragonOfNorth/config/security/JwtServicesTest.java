package org.miniProjectTwo.DragonOfNorth.config.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtServicesTest {

    private JwtServices jwtServices;

    @BeforeEach
    void setUp() throws Exception {
        ensureLocalKeysExist();
        jwtServices = new JwtServices("keys/private_key.pem", "keys/public_key.pem");
        ReflectionTestUtils.setField(jwtServices, "accessTokenExpiration", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtServices, "refreshTokenExpiration", 86400000L); // 24 hours
    }

    private static void ensureLocalKeysExist() throws Exception {
        Path keysDir = Path.of("keys");
        Path privateKeyPath = keysDir.resolve("private_key.pem");
        Path publicKeyPath = keysDir.resolve("public_key.pem");

        if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
            return;
        }

        Files.createDirectories(keysDir);

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
