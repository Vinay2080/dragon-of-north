package org.miniProjectTwo.DragonOfNorth.security.web;

import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaPolicy;

import java.lang.annotation.*;

/**
 * Marks endpoints that require a recently verified MFA timestamp.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRecentMfa {
    /**
     * Policy identifier that selects the per-operation freshness window.
     */
    RecentMfaPolicy policy() default RecentMfaPolicy.DEFAULT;

    /**
     * When {@code true}, the recent-MFA requirement is enforced only for users who already have MFA enabled.
     *
     * <p>This is intended for first-time MFA enrollment endpoints where authenticated users without MFA
     * must be able to bootstrap setup, while users with MFA enabled still need fresh step-up verification.
     */
    boolean onlyWhenMfaEnabled() default false;
}
