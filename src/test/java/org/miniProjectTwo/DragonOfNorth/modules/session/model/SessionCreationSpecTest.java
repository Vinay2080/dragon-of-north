package org.miniProjectTwo.DragonOfNorth.modules.session.model;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SessionCreationSpecTest {

    @Test
    void fromAppUser_shouldSetVerifiedTimestamp_whenMfaNotRequired() {
        AppUser user = new AppUser();
        user.setMfaEnabled(false);

        SessionCreationSpec spec = SessionCreationSpec.fromAppUser(user, "pwd");

        assertFalse(spec.mfaRequired());
        assertNotNull(spec.mfaVerifiedAt());
    }

    @Test
    void fromAppUser_shouldLeaveMfaUnverified_whenMfaRequired() {
        AppUser user = new AppUser();
        user.setMfaEnabled(true);

        SessionCreationSpec spec = SessionCreationSpec.fromAppUser(user, "oauth");

        assertTrue(spec.mfaRequired());
        assertNull(spec.mfaVerifiedAt());
        assertEquals("oauth", spec.primaryAmr());
    }

    @Test
    void constructor_shouldRejectVerifiedTimestamp_whenMfaRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> new SessionCreationSpec("pwd", true, Instant.now()));
    }

    @Test
    void constructor_shouldRejectMissingVerifiedTimestamp_whenMfaNotRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> new SessionCreationSpec("pwd", false, null));
    }
}
