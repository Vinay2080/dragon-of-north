package org.miniProjectTwo.DragonOfNorth.components;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class TokenHasher {
    private final PasswordEncoder passwordEncoder;

    public TokenHasher() {
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }

    public String hashToken(String token) {
        String hashed = sha256(token);
        return passwordEncoder.encode(hashed);
    }

    public boolean matches(String rawToken, String hashedToken) {
        String hashed = sha256(rawToken);
        return passwordEncoder.matches(hashed, hashedToken);
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
