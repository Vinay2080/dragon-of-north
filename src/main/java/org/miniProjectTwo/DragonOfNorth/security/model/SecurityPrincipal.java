package org.miniProjectTwo.DragonOfNorth.security.model;

import org.springframework.security.core.GrantedAuthority;

import java.security.Principal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

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

