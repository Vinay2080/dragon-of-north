package org.miniProjectTwo.DragonOfNorth.security.model;

import org.springframework.security.core.GrantedAuthority;

import java.security.Principal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Immutable security principal representing the authenticated user's identity and authorities.
 *
 * <p>Fields:</p>
 * <ul>
 *     <li>{@code userId}: The unique identifier of the authenticated user.</li>
 *     <li>{@code authorities}: The collection of granted authorities (roles/permissions) for the user.</li>
 *     <li>{@code mfaVerified}: Indicates whether MFA has been verified for the current session.</li>
 *     <li>{@code mfaVerifiedAt}: The timestamp when MFA was verified, or null if not verified.</li>
 *     <li>{@code sessionId}: The unique identifier of the current session, if applicable.</li>
 *     <li>{@code amr}: The list of authentication methods used during authentication (e.g., "pwd", "otp").</li>
 * </ul>
 */
public record SecurityPrincipal(
        UUID userId,
        Collection<? extends GrantedAuthority> authorities,
        boolean mfaVerified,
        Instant mfaVerifiedAt,
        UUID sessionId,
        List<String> amr
) implements Principal {
    @Override
    public String getName() {
        return userId == null ? "" : userId.toString();
    }
}

