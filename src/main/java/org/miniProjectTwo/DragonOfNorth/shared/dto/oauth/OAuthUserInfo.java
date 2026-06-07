package org.miniProjectTwo.DragonOfNorth.shared.dto.oauth;

import lombok.Builder;

/**
 * DTO representing user information obtained from Google OAuth.
 *
 * @param sub            Unique identifier for the user.
 * @param email          User's email address.
 * @param emailVerified  Indicates if the email has been verified.
 * @param name           User's full name.
 * @param picture        URL of the user's profile picture.
 * @param issuer         Issuer of the ID token.
 * @param audience       Audience for which the ID token is intended.
 * @param expirationTime Expiration time of the ID token in seconds since epoch.
 * @param issuedAtTime   Time at which the ID token was issued in seconds since epoch.
 */
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
//todo documentations