package org.miniProjectTwo.DragonOfNorth.shared.dto.oauth;

import lombok.Builder;

@Builder
public record OAuthUserInfo(
        String sub,
        String email,
        boolean emailVerified,
        String name,
        String picture,
        String issuer,
        String audience,
        Long expirationTime,
        Long issuedAtTime
) {
}
