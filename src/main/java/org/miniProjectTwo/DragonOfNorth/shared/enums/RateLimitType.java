package org.miniProjectTwo.DragonOfNorth.shared.enums;

import org.miniProjectTwo.DragonOfNorth.infrastructure.config.RateLimitConfig;
import org.miniProjectTwo.DragonOfNorth.infrastructure.config.RateLimitProperties;

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
