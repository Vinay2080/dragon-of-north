package org.miniProjectTwo.DragonOfNorth.shared.enums;

/**
 * Enum representing different types of rate limits for various operations.
 */
public enum RateLimitType {
    SIGNUP,
    LOGIN,
    OTP,
    PASSWORDLESS,
    MFA_VERIFY,
    STEP_UP_VERIFY,
    STEP_UP_REQUEST,
    REFRESH
}
