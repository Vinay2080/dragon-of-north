package org.miniProjectTwo.DragonOfNorth.config;

import org.jspecify.annotations.NullMarked;
import org.miniProjectTwo.DragonOfNorth.impl.AuditorAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {

    @Bean
    @NullMarked
    public AuditorAware<String> auditorAware(){
        return new AuditorAwareImpl();
    }
}
