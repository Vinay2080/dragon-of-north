package org.miniProjectTwo.DragonOfNorth.security.service;

import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Mints access tokens from the persisted session context plus role/authentication facts.
 */
@Component
public class SessionAccessTokenIssuer {

    private final JwtServices jwtServices;

    public SessionAccessTokenIssuer(JwtServices jwtServices) {
        this.jwtServices = jwtServices;
    }

    /**
     * Mints an access JWT from the authoritative session row and current role set.
     */
    public String mintAccessToken(Session session, Set<Role> roles) {
        return jwtServices.generateAccessToken(toAuthnFacts(session, roles));
    }

    /**
     * Builds {@link AuthnFacts} from session state (source of truth) and role names.
     */
    public static AuthnFacts toAuthnFacts(Session session, Set<Role> roles) {
        Objects.requireNonNull(session, "session must not be null");
        UUID userId = session.getAppUser() == null ? null : session.getAppUser().getId();
        if (userId == null) {
            throw new IllegalArgumentException("session.appUser.id must not be null");
        }
        UUID sessionId = session.getId();
        if (sessionId == null) {
            throw new IllegalArgumentException("session.id must not be null");
        }

        List<String> roleNames = roles == null
                ? List.of()
                : roles.stream()
                  .map(role -> role.getRoleName().name())
                  .toList();

        Instant mfaVerifiedAt = session.getMfaVerifiedAt();
        boolean mfaVerified = mfaVerifiedAt != null;

        return new AuthnFacts(
                userId,
                roleNames,
                mfaVerified,
                mfaVerifiedAt,
                buildAmr(session),
                sessionId
        );
    }

    /**
     * Builds the AMR list for the session, including primary authentication and MFA method if applicable.
     *
     * @param session the session from which to derive AMR values (must have non-blank primaryAmr)
     * @return the AMR list for the session
     */
    private static List<String> buildAmr(Session session) {
        String primary = session.getPrimaryAmr();
        if (primary == null || primary.isBlank()) {
            throw new IllegalArgumentException("session.primaryAmr must not be blank");
        }
        String mfaMethod = session.getMfaMethodAmr();
        if (mfaMethod == null || mfaMethod.isBlank()) {
            return List.of(primary);
        }
        if (primary.equals(mfaMethod)) {
            return List.of(primary);
        }
        return List.of(primary, mfaMethod);
    }
}
