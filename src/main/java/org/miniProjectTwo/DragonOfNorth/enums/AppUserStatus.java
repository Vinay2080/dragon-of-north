package org.miniProjectTwo.DragonOfNorth.enums;

import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.AuthenticationService;

/**
 * User account lifecycle states controlling authentication and authorization flows.
 * Status transitions drive verification requirements and access permissions. CREATED
 * requires OTP verification, VERIFIED enables full authentication, DELETED blocks
 * all access. Critical for security and user journey management.
 *
 * @see AuthenticationService for status-based routing
 * @see AppUserStatusFinderResponse for status reporting
 */
public enum AppUserStatus {

    NOT_EXIST,
    /**
     * The user is created but not verified yet
     */
    CREATED,
    /**
     * The user is verified for signup.
     */
    VERIFIED,
    /**
     * The user has been deleted from the system.
     */
    DELETED
}
