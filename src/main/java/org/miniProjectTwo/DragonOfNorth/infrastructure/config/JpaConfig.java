package org.miniProjectTwo.DragonOfNorth.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing and links it to the {@code auditorAware} bean.
 * <blockquote>
 * <p>JPA auditing allows automatic population of audit fields (e.g., createdBy, createdDate) on entities. By referencing the {@code auditorAware} bean, it will use the logic defined in {@link org.miniProjectTwo.DragonOfNorth.infrastructure.audit.AuditorAwareImpl} to determine the current auditor (e.g., authenticated user) for audit purposes.</p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {
}
