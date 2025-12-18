package org.miniProjectTwo.DragonOfNorth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configures JPA auditing for the application.
 *
 * <p>This setup enables automatic population of auditing fields such as
 * {@code createdBy}, {@code createdAt}, {@code updatedBy}, and {@code updatedAt}
 * on entities that extend a base auditable entity class.</p>
 *
 * <p>The {@code auditorAwareRef} points to the {@code AuditorAware} bean responsible
 * for determining the current user performing the action.</p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {

    private static final Logger log = LoggerFactory.getLogger(JpaConfig.class);

    /**
     * Logs JPA auditing activation at application startup.
     */
    public JpaConfig() {
        log.info("JPA Auditing enabled using AuditorAware implementation: 'auditorAware'");
    }
}
