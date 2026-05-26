package org.miniProjectTwo.DragonOfNorth.security.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class RecentMfaWebConfig implements WebMvcConfigurer {

    private final RecentMfaEnforcementInterceptor recentMfaEnforcementInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(recentMfaEnforcementInterceptor);
    }
}
