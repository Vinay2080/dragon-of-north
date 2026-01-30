package org.miniProjectTwo.DragonOfNorth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DragonOfNorthApplicationTests {

    private static volatile KeyPaths KEY_PATHS;

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("testDB")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("keys.private", () -> ensureLocalKeysExist().privateKeyPath().toString());
        registry.add("keys.public", () -> ensureLocalKeysExist().publicKeyPath().toString());
    }

    private static KeyPaths ensureLocalKeysExist() {
        if (KEY_PATHS == null) {
            synchronized (DragonOfNorthApplicationTests.class) {
                if (KEY_PATHS == null) {
                    try {
                        Path keysDir = Files.createTempDirectory("dragon-of-north-keys");
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
    @Test
    void contextLoads() {
    }

}
