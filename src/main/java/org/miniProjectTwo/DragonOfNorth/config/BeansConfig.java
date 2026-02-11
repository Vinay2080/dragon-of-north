package org.miniProjectTwo.DragonOfNorth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

/**
 * Central configuration class responsible for providing shared application-level beans.
 *
 * <p>In particular, this class registers the {@link AuditorAware} implementation used by
 * Spring Data JPA to automatically populate audit-related fields such as
 * {@code createdBy} and {@code lastModifiedBy}.</p>
 *
 * <p>The provided {@link AuditorAwareImpl} currently resolves the auditor from the
 * security context and falls back to {@code SYSTEM}. This ensures consistent auditing
 * even in background tasks or unauthenticated operations.</p>
 */
@Configuration
public class BeansConfig {

    private static final Logger log = LoggerFactory.getLogger(BeansConfig.class);

    /**
     * Registers the application's {@link AuditorAware} bean.
     *
     * @return implementation responsible for determining the current auditor
     */
    @Bean
    @NullMarked
    public AuditorAware<String> auditorAware() {
        log.info("AuditorAware bean initialized using AuditorAwareImpl");
        return new AuditorAwareImpl();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
