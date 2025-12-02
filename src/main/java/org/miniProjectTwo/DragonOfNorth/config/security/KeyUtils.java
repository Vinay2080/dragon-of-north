package org.miniProjectTwo.DragonOfNorth.config.security;

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
 * Utility class for loading and managing cryptographic keys from PEM files.
 * <p>
 * This class provides thread-safe methods to load and cache RSA private and public keys
 * from PEM files. It handles various PEM formats and provides efficient key loading
 * with double-checked locking for thread safety.
 * </p>
 *
 * <p>Supported key formats:
 * <ul>
 *   <li>Private Keys: PKCS#8 and traditional RSA private keys</li>
 *   <li>Public Keys: X.509 and RSA public keys</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * PrivateKey privateKey= KeyUtils.loadPrivateKey("keys/private.pem");
 * PublicKey publicKey = KeyUtils.loadPublicKey("keys/public.pem");
 * </pre>
 *
 * @see java.security.PrivateKey
 * @see java.security.PublicKey
 * @see java.security.KeyFactory
 */
public final class KeyUtils {

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
            throw new RuntimeException("Unable to load RSA KeyFactory", e);
        }
    }
    /**
     * Loads and caches the RSA private key from the specified PEM file.
     * <p>
     * This method uses double-checked locking to ensure thread safety while
     * maintaining performance. The key is loaded once and cached for subsequent calls.
     *
     * @param pemPath the classpath-relative path to the PEM file
     * @return the loaded PrivateKey
     * @throws Exception if the key cannot be loaded or is invalid
     * @throws IllegalArgumentException if the PEM content is empty or invalid
     */
    public static PrivateKey loadPrivateKey(String pemPath) throws Exception {
        if (PRIVATE_KEY == null) {
            synchronized (KeyUtils.class) {
                if (PRIVATE_KEY == null) {
                    String pem = readKey(pemPath);
                    pem = normalizePem(pem, PRIVATE_HEADERS);

                    byte[] decoded = Base64.getDecoder().decode(pem);
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);

                    PRIVATE_KEY = RSA_FACTORY.generatePrivate(keySpec);
                }
            }
        }
        return PRIVATE_KEY;
    }
    /**
     * Loads and caches the RSA public key from the specified PEM file.
     * <p>
     * This method uses double-checked locking to ensure thread safety while
     * maintaining performance. The key is loaded once and cached for subsequent calls.
     *
     * @param pemPath the classpath-relative path to the PEM file
     * @return the loaded PublicKey
     * @throws Exception if the key cannot be loaded or is invalid
     * @throws IllegalArgumentException if the PEM content is empty or invalid
     */
    public static PublicKey loadPublicKey(String pemPath) throws Exception {
        if (PUBLIC_KEY == null) {
            synchronized (KeyUtils.class) {
                if (PUBLIC_KEY == null) {
                    String pem = readKey(pemPath);
                    pem = normalizePem(pem, PUBLIC_HEADERS);

                    byte[] decoded = Base64.getDecoder().decode(pem);
                    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);

                    PUBLIC_KEY = RSA_FACTORY.generatePublic(keySpec);
                }
            }
        }
        return PUBLIC_KEY;
    }
    /**
     * Normalizes PEM content by removing headers and whitespace.
     *
     * @param pem the PEM content to normalize
     * @param headers the headers to remove from the PEM content
     * @return the normalized PEM content
     * @throws IllegalArgumentException if the PEM content is empty or invalid
     */
    private static String normalizePem(String pem, String[] headers) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("PEM content is empty");
        }

        for (String h : headers) {
            pem = pem.replace(h, "");
        }

        pem = pem.replaceAll("\\s", "");

        if (pem.isEmpty()) {
            throw new IllegalArgumentException("PEM content invalid after header removal");
        }

        return pem;
    }
    /**
     * Reads the key content from a classpath resource.
     *
     * @param path the classpath-relative path to the key file
     * @return the key content as a String
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if the file is not found
     */
    private static String readKey(String path) throws IOException {
        try (InputStream is = KeyUtils.class.getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                return new String(is.readAllBytes());
            }
        }

        var filePath = Path.of(path);
        if (Files.exists(filePath)) {
            return Files.readString(filePath);
        }

        throw new IllegalArgumentException("Key not found at: " + path);
    }

}

