package org.miniProjectTwo.DragonOfNorth.config.security;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;

@Service
public class JwtServices {
    private final String TOKEN_TYPE = "token_type";
    private final PrivateKey privateKey;
    private final PublicKey publicKey;


    @Value("${app.security.jwt.expiration.access-token}")
    private long accessTokenExpiration;

    @Value("${app.security.jwt.expiration.refresh-token}")
    private long refreshTokenExpiration;

    public JwtServices() throws Exception {
        privateKey = KeyUtils.loadPrivateKey("/local-keys/private_key.pem");
        publicKey = KeyUtils.loadPublicKey("/local-keys/public_key.pem");
    }

    public String extractUsername(String jwt) {
        return "";
    }

    public boolean isTokenValid(String jwt, UserDetails userDetails) {
        return true;
    }
}
