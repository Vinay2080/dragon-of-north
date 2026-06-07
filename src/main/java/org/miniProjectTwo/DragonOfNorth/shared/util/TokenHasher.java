package org.miniProjectTwo.DragonOfNorth.shared.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * One-way hashing utility for storing token references (e.g., passwordless tokens) without plaintext.
 */
@Component
public class TokenHasher {

    /**
     * Hashes a raw token using SHA-256.
     *
     * @param token the raw token to hash
     * @return deterministic SHA-256 hash
     */
    public String hashToken(String token) {
        return sha256(token);
    }

    /**
     * Hashes a raw token using SHA-256.
     *
     * @param input The raw token to hash.
     * @return The deterministic SHA-256 hash of the token.
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
     * Converts a byte array to a hexadecimal string representation.
     *
     * @param bytes The byte array to convert.
     * @return The hexadecimal string representation of the byte array.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}