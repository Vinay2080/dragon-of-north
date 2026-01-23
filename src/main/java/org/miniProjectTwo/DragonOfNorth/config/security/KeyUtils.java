package org.miniProjectTwo.DragonOfNorth.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utility class for loading RSA private and public keys from PEM files.
 *
 * <p>This class supports PEM files stored either on the classpath or on the filesystem.
 * Keys are loaded once and cached using double-checked locking for high-performance,
 * thread-safe access.</p>
 *
 * <p>Supported formats:</p>
 * <ul>
 *     <li>Private Keys: PKCS#8, RSA PRIVATE KEY</li>
 *     <li>Public Keys: X.509, RSA PUBLIC KEY</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * PrivateKey privateKey = KeyUtils.loadPrivateKey("keys/private.pem");
 * PublicKey publicKey = KeyUtils.loadPublicKey("keys/public.pem");
 * </pre>
 */
public final class KeyUtils {

    private static final Logger log = LoggerFactory.getLogger(KeyUtils.class);

    private KeyUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static volatile PrivateKey PRIVATE_KEY;
    private static volatile PublicKey PUBLIC_KEY;

    private static final String[] PRIVATE_HEADERS = {
            "-----BEGIN PRIVATE KEY-----",
            "-----END PRIVATE KEY-----",
            "-----BEGIN RSA PRIVATE KEY-----",
            "-----END RSA PRIVATE KEY-----"
    };

    private static final String[] PUBLIC_HEADERS = {
            "-----BEGIN PUBLIC KEY-----",
            "-----END PUBLIC KEY-----",
            "-----BEGIN RSA PUBLIC KEY-----",
            "-----END RSA PUBLIC KEY-----"
    };

    private static final KeyFactory RSA_FACTORY;

    static {
        try {
            RSA_FACTORY = KeyFactory.getInstance("RSA");
        } catch (Exception e) {
            log.error("Failed to initialize RSA KeyFactory: {}", e.getMessage());
            throw new RuntimeException("Unable to load RSA KeyFactory", e);
        }
    }

    /**
     * Loads and caches the RSA private key from a PEM file.
     *
     * @param pemPath path to the private key file (classpath or filesystem)
     * @return the loaded {@link PrivateKey}
     * @throws Exception if loading or parsing fails
     */
    public static PrivateKey loadPrivateKey(String pemPath) throws Exception {
        if (PRIVATE_KEY == null) {
            synchronized (KeyUtils.class) {
                if (PRIVATE_KEY == null) {
                    log.info("Loading RSA private key from: {}", pemPath);
                    String pem = readKey(pemPath);
                    pem = normalizePem(pem, PRIVATE_HEADERS);

                    byte[] decoded = Base64.getDecoder().decode(pem);
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);

                    PRIVATE_KEY = RSA_FACTORY.generatePrivate(keySpec);
                    log.info("Private key successfully loaded and cached.");
                } else {
                    log.debug("Private key returned from cache.");
                }
            }
        }
        return PRIVATE_KEY;
    }

    /**
     * Loads and caches the RSA public key from a PEM file.
     *
     * @param pemPath path to the public key file (classpath or filesystem)
     * @return the loaded {@link PublicKey}
     * @throws Exception if loading or parsing fails
     */
    public static PublicKey loadPublicKey(String pemPath) throws Exception {
        if (PUBLIC_KEY == null) {
            synchronized (KeyUtils.class) {
                if (PUBLIC_KEY == null) {
                    log.info("Loading RSA public key from: {}", pemPath);
                    String pem = readKey(pemPath);
                    pem = normalizePem(pem, PUBLIC_HEADERS);

                    byte[] decoded = Base64.getDecoder().decode(pem);
                    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);

                    PUBLIC_KEY = RSA_FACTORY.generatePublic(keySpec);
                    log.info("Public key successfully loaded and cached.");
                } else {
                    log.debug("Public key returned from cache.");
                }
            }
        }
        return PUBLIC_KEY;
    }

    /**
     * Normalizes PEM content by stripping headers/footers and removing whitespace.
     *
     * @param pem         raw PEM file content
     * @param headers     acceptable headers for removal
     * @return normalized base64 content
     */
    private static String normalizePem(String pem, String[] headers) {
        if (pem == null || pem.isBlank()) {
            log.error("PEM content is empty or null");
            throw new IllegalArgumentException("PEM content is empty");
        }

        for (String h : headers) {
            pem = pem.replace(h, "");
        }

        pem = pem.replaceAll("\\s", "");

        if (pem.isEmpty()) {
            log.error("PEM content invalid after header removal");
            throw new IllegalArgumentException("Invalid PEM format");
        }

        return pem;
    }

    /**
     * Reads a key file from the classpath or filesystem.
     *
     * @param path path to the key file
     * @return file content as UTF-8 string
     * @throws IOException if the file cannot be read
     */
    private static String readKey(String path) throws IOException {

        // Try the classpath first
        try (InputStream is = KeyUtils.class.getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                log.debug("Reading key from classpath: {}", path);
                return new String(is.readAllBytes());
            }
        }

        // Then filesystem
        Path fsPath = Path.of(path);
        if (Files.exists(fsPath)) {
            log.debug("Reading key from filesystem: {}", fsPath);
            return Files.readString(fsPath);
        }

        log.error("Key file not found at: {}", path);
        throw new IllegalArgumentException("Key not found at: " + path);
    }
}
