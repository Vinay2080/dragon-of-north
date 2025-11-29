package org.miniProjectTwo.DragonOfNorth.impl;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@NullMarked
public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    @NullMarked
    public Optional<String> getCurrentAuditor() {
        return Optional.of("SYSTEM");
    }
}
