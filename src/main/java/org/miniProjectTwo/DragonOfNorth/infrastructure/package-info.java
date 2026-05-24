/**
 * Infrastructure module containing cross-cutting runtime wiring used by multiple domain modules.
 * <p>
 * Includes configuration beans, scheduled maintenance, encryption/key providers, audit identity
 * resolution, startup initializers, and third-party client factories (AWS, Cloudinary, Redis).
 * Changes here can have broad side effects across auth, profile, OTP, and session flows.
 */
@ApplicationModule(
        type = ApplicationModule.Type.OPEN
)
package org.miniProjectTwo.DragonOfNorth.infrastructure;

import org.springframework.modulith.ApplicationModule;