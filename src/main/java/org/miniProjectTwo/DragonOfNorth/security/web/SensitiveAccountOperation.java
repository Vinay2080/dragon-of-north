package org.miniProjectTwo.DragonOfNorth.security.web;

import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaPolicy;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Marker for high-risk account/security operations that must require recent MFA.
 *
 * <p>Meta-annotated with {@link RequireRecentMfa} so new endpoints are secure-by-construction.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequireRecentMfa
public @interface SensitiveAccountOperation {
    /**
     * Policy identifier for the operation-specific MFA freshness window.
     */
    @AliasFor(annotation = RequireRecentMfa.class, attribute = "policy")
    RecentMfaPolicy policy() default RecentMfaPolicy.DEFAULT;

    @AliasFor(annotation = RequireRecentMfa.class, attribute = "onlyWhenMfaEnabled")
    boolean onlyWhenMfaEnabled() default false;
}
