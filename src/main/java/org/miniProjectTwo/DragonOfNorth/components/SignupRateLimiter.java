package org.miniProjectTwo.DragonOfNorth.components;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.exception.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.properties.AuthRateLimitProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SignupRateLimiter {

    private final AuthRateLimitProperties properties;
    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public void check(String ip, String identifier) {
        String key = ip + ":" + identifier;
        AttemptWindow window = attempts.computeIfAbsent(
                key,
                k -> new AttemptWindow()
        );
    }

    static class AttemptWindow {
        int count = 0;
        long windowStart = System.currentTimeMillis();
        long blockedUntil = 0;

        synchronized void validate(AuthRateLimitProperties.Signup conf) {
            long now = System.currentTimeMillis();

            if (now < windowStart) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
            }
            if (now - windowStart > conf.getRequestWindowSeconds() * 1000L) {
                windowStart = now;
                count = 0;
            }
            count++;

            if (count > conf.getMaxRequestsPerWindow()){
                blockedUntil = now + conf.getBlockDurationMinutes() * 60_000L;
                throw  new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
            }
        }

    }
}

