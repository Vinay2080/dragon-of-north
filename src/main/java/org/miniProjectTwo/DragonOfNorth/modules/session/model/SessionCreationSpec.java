package org.miniProjectTwo.DragonOfNorth.modules.session.model;

import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;

import java.time.Instant;
import java.util.Objects;

/**
 * Explicit MFA/session policy applied when a new device session row is created.
 *
 * <p>{@code mfaVerifiedAt} is set only when MFA has been completed (login MFA or step-up).
 * When MFA has not been completed, it remains {@code null}.</p>
 */
public record SessionCreationSpec(
        String primaryAmr,
        boolean mfaRequired,
        Instant mfaVerifiedAt,
        String mfaMethodAmr
) {
    public SessionCreationSpec {
        Objects.requireNonNull(primaryAmr, "primaryAmr must not be null");
        if (primaryAmr.isBlank()) {
            throw new IllegalArgumentException("primaryAmr must not be blank");
        }
        if (mfaRequired && mfaVerifiedAt != null) {
            throw new IllegalArgumentException("mfaVerifiedAt must be null when mfaRequired is true");
        }
        if (mfaMethodAmr != null && mfaMethodAmr.isBlank()) {
            throw new IllegalArgumentException("mfaMethodAmr must not be blank when provided");
        }
    }

    /**
     * Derives session MFA fields from the user's current MFA enrollment state.
     */
    public static SessionCreationSpec fromAppUser(AppUser appUser, String primaryAmr) {
        Objects.requireNonNull(appUser, "appUser must not be null");
        boolean mfaRequired = appUser.isMfaEnabled();
//        Instant mfaVerifiedAt = null;
        return new SessionCreationSpec(primaryAmr, mfaRequired, null, null);
    }
}
