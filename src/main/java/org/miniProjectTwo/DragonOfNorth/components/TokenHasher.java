package org.miniProjectTwo.DragonOfNorth.components;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility component for secure token hashing.
 * <p>
 * Uses SHA-256 for deterministic hashing.
 * Suitable for refresh tokens which are already high en    tropy.
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

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}