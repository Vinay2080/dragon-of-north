package org.miniProjectTwo.DragonOfNorth.config.security;


import org.springframework.stereotype.Service;

@Service
public class JwtServices {
    public String ExtractUsername(String jwt) {
        return "";
    }

    public boolean isTokenValid(String jwt, String username) {
        return true;
    }
}
