package org.miniProjectTwo.DragonOfNorth.config;

import org.jspecify.annotations.NullMarked;
import org.miniProjectTwo.DragonOfNorth.impl.AuditorAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration class for JPA and auditing settings.
 * This class enables JPA auditing and provides the necessary beans for tracking
 * the principal that created or modified an entity.
 *
 * <p>Uses {@code @EnableJpaAuditing} to enable the JPA auditing functionality
 * and configures the {@link AuditorAware} bean to provide the current auditor.</p>
 *
 * @see org.springframework.data.jpa.repository.config.EnableJpaAuditing
 * @see org.springframework.data.domain.AuditorAware
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * Creates and configures an {@link AuditorAware} bean to provide the current auditor.
     * This bean is used by Spring Data JPA to automatically populate the
     * {@code createdBy} and {@code lastModifiedBy} fields in audited entities.
     *
     * <p>The current implementation returns a static value ("SYSTEM"). In a real application,
     * this would typically be replaced with a mechanism to get the current user from the
     * security context.</p>
     *
     * @return An instance of {@link AuditorAware} that provides the current auditor
     * @see AuditorAwareImpl
     */
    @Bean
    @NullMarked
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl();
    }
}
