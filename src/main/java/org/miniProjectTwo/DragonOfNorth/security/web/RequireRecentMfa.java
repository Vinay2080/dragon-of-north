package org.miniProjectTwo.DragonOfNorth.security.web;

import java.lang.annotation.*;

/**
 * Marks endpoints that require a recently verified MFA timestamp.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRecentMfa {
}
