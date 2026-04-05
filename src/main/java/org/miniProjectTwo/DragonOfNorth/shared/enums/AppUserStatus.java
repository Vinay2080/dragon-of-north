package org.miniProjectTwo.DragonOfNorth.shared.enums;

import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthenticationService;

/**
 * User account lifecycle states controlling authentication and authorization flows.
 * Status transitions drive verification requirements and access permissions.
 * PENDING_VERIFICATION requires OTP verification before login is allowed,
 * ACTIVE enables full authentication, LOCKED blocks access, DELETED blocks
 * all access. Critical for security and user journey management.
 *
 * @see AuthenticationService for status-based routing
 * @see AppUserStatusFinderResponse for status reporting
 */
public enum AppUserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    LOCKED,
    DELETED
}
