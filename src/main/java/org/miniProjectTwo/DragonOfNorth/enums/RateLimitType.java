package org.miniProjectTwo.DragonOfNorth.enums;

import org.miniProjectTwo.DragonOfNorth.config.RateLimitConfig;
import org.miniProjectTwo.DragonOfNorth.config.RateLimitProperties;

/**
 * Rate limiting categories for protecting API endpoints and services.
 * Each type has an independent token bucket configuration and Redis storage.
 * SIGNUP prevents account creation abuse, LOGIN blocks brute force attacks,
 * OTP limits SMS/email delivery costs. Critical for system security and cost control.
 *
 * @see RateLimitConfig for bucket configuration
 * @see RateLimitProperties for endpoint mapping
 */
public enum RateLimitType {
    SIGNUP,
    LOGIN,
    OTP
}
