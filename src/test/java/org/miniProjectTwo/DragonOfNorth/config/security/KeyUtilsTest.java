package org.miniProjectTwo.DragonOfNorth.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class KeyUtilsTest {

    @BeforeEach
    void clearCachedKeys() {
        ReflectionTestUtils.setField(KeyUtils.class, "PRIVATE_KEY", null);
        ReflectionTestUtils.setField(KeyUtils.class, "PUBLIC_KEY", null);
    }

    @Test
    void utilityConstructor_shouldThrowUnsupportedOperationException() throws Exception {
        Constructor<KeyUtils> constructor = KeyUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, ex.getCause());
    }

    @Test
    void loadPrivateAndPublicKey_shouldLoadAndCacheKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        Path dir = Files.createTempDirectory("keyutils-test");
        Path privatePath = dir.resolve("private.pem");
        Path publicPath = dir.resolve("public.pem");

        Files.writeString(privatePath, toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded()), StandardCharsets.UTF_8);
        Files.writeString(publicPath, toPem("PUBLIC KEY", keyPair.getPublic().getEncoded()), StandardCharsets.UTF_8);

        var privateKey1 = KeyUtils.loadPrivateKey(privatePath.toString());
        var publicKey1 = KeyUtils.loadPublicKey(publicPath.toString());
        var privateKey2 = KeyUtils.loadPrivateKey("does-not-matter-after-cache");
        var publicKey2 = KeyUtils.loadPublicKey("does-not-matter-after-cache");

        assertNotNull(privateKey1);
        assertNotNull(publicKey1);
        assertSame(privateKey1, privateKey2);
        assertSame(publicKey1, publicKey2);
    }

    @Test
    void loadPrivateKey_shouldThrow_whenPathDoesNotExist() {
        assertThrows(IllegalArgumentException.class,
                () -> KeyUtils.loadPrivateKey("missing-private.pem"));
    }

    private static String toPem(String type, byte[] derBytes) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(derBytes);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }
}
