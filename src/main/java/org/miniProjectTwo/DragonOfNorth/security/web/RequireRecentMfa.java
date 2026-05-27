package org.miniProjectTwo.DragonOfNorth.security.web;

import java.lang.annotation.*;

/**
 * Marks endpoints that require a recently verified MFA timestamp.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRecentMfa {
    /**
     * When {@code true}, the recent-MFA requirement is enforced only for users who already have MFA enabled.
     *
     * <p>This is intended for first-time MFA enrollment endpoints where authenticated users without MFA
     * must be able to bootstrap setup, while users with MFA enabled still need fresh step-up verification.
     */
    boolean onlyWhenMfaEnabled() default false;
}
