package org.miniProjectTwo.DragonOfNorth.redis;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Testcontainers(disabledWithoutDocker = true)
public class RedisTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7")
                    .withExposedPorts(6379);

    private static volatile KeyPaths KEY_PATHS;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("keys.private", () -> ensureLocalKeysExist().privateKeyPath().toString());
        registry.add("keys.public", () -> ensureLocalKeysExist().publicKeyPath().toString());
    }

    private static KeyPaths ensureLocalKeysExist() {
        if (KEY_PATHS == null) {
            synchronized (RedisTest.class) {
                if (KEY_PATHS == null) {
                    try {
                        Path keysDir = Files.createTempDirectory("dragon-of-north-keys-redis");
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

                        KEY_PATHS = new KeyPaths(privateKeyPath, publicKeyPath);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to generate RSA keys for tests", e);
                    }
                }
            }
        }
        return KEY_PATHS;
    }

    private static String toPem(String type, byte[] derBytes) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(derBytes);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }

    private record KeyPaths(Path privateKeyPath, Path publicKeyPath) {
    }

    private final StringRedisTemplate redisTemplate;

    RedisTest(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Test
    void shouldStoreAndReadValueFromRedis() {
        redisTemplate.opsForValue().set("k", "v");
        assertEquals("v", redisTemplate.opsForValue().get("k"));
    }

    @AfterEach
    void cleanup() {
        redisTemplate.delete("k");
    }


}
