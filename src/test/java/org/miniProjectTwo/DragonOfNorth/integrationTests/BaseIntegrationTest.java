package org.miniProjectTwo.DragonOfNorth.integrationTests;

import org.miniProjectTwo.DragonOfNorth.services.otp.EmailOtpSender;
import org.miniProjectTwo.DragonOfNorth.services.otp.PhoneOtpSender;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

@Testcontainers
@ActiveProfiles("test")
@Import(BaseIntegrationTest.MockedBeansTestConfig.class)
public abstract class BaseIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testDB")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7")
                    .withExposedPorts(6379);

    private static volatile KeyPaths KEY_PATHS;
    @Autowired
    protected EmailOtpSender emailOtpSender;
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");

        registry.add("keys.private", () -> ensureLocalKeysExist().privateKeyPath().toString());
        registry.add("keys.public", () -> ensureLocalKeysExist().publicKeyPath().toString());
    }

    private static KeyPaths ensureLocalKeysExist() {
        if (KEY_PATHS == null) {
            synchronized (BaseIntegrationTest.class) {
                if (KEY_PATHS == null) {
                    try {
                        Path keysDir = Files.createTempDirectory("dragon-of-north-keys-it");
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

    @TestConfiguration
    static class MockedBeansTestConfig {

        @Bean
        @Primary
        SesClient mockedSesClient() {
            return Mockito.mock(SesClient.class);
        }

        @Bean
        @Primary
        SnsClient mockedSnsClient() {
            return Mockito.mock(SnsClient.class);
        }

        @Bean
        @Primary
        EmailOtpSender emailOtpSender() {
            return Mockito.mock(EmailOtpSender.class);
        }

        @Bean
        @Primary
        PhoneOtpSender phoneOtpSender() {
            return Mockito.mock(PhoneOtpSender.class);
        }
    }

    private record KeyPaths(Path privateKeyPath, Path publicKeyPath) {
    }
}
