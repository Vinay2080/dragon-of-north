package org.miniProjectTwo.DragonOfNorth.security.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
}
