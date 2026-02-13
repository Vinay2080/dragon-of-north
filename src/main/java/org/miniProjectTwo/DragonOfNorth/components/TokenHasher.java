package org.miniProjectTwo.DragonOfNorth.components;

import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.RefreshTokenService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility component for secure token hashing and verification.
 * Used by {@link RefreshTokenService} to securely store and validate refresh tokens
 * in the database. Implements double hashing: SHA-256 followed by BCrypt
 * for enhanced security against rainbow table and brute force attacks.
 */
@Component
public class TokenHasher {
    private final PasswordEncoder passwordEncoder;

    public TokenHasher() {
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }


    /**
     * Hashes a raw token using SHA-256 followed by BCrypt.
     *
     * @param token the raw token to hash
     * @return the double-hashed token for secure storage
     */
    public String hashToken(String token) {
        String hashed = sha256(token);
        return passwordEncoder.encode(hashed);
    }


    /**
     * Verifies a raw token against a stored hash.
     *
     * @param rawToken    the raw token to verify
     * @param hashedToken the stored hash to compare against
     * @return true if the token matches, false otherwise
     */
    public boolean matches(String rawToken, String hashedToken) {
        String hashed = sha256(rawToken);
        return passwordEncoder.matches(hashed, hashedToken);
    }

    /**
     * Applies SHA-256 hashing to the input string.
     *
     * @param input the string to hash
     * @return SHA-256 hash as hexadecimal string
     * @throws RuntimeException if the SHA-256 algorithm is not available
     */
    private String sha256(String input) {
        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Converts a byte array to hexadecimal string representation.
     *
     * @param bytes the byte array to convert
     * @return hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
