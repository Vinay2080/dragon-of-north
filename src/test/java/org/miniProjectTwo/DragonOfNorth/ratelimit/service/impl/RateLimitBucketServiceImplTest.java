package org.miniProjectTwo.DragonOfNorth.ratelimit.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.infrastructure.config.RateLimitProperties;
import org.miniProjectTwo.DragonOfNorth.ratelimit.service.RateLimitBucketService;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RateLimitType;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitBucketServiceImplTest {

    private Map<String, RateLimitProperties.LimitRule> fullRules() {
        RateLimitProperties.LimitRule rule = new RateLimitProperties.LimitRule();
        rule.setCapacity(10);
        rule.setRefillTokens(5);
        rule.setRefillMinutes(1);
        return Map.of(
                "otp", rule,
                "signup", rule,
                "login", rule,
                "passwordless", rule,
                "mfa_verify", rule,
                "step_up_request", rule,
                "step_up_verify", rule,
                "refresh", rule
        );
    }



    @Mock
    private RateLimitProperties properties;

    @InjectMocks
    private RateLimitBucketServiceImpl rateLimitBucketService;

    @Test
    void initializeConfigurations_shouldSucceed_whenOtpRuleExists() {
        // arrange
        RateLimitProperties.LimitRule otpRule = new RateLimitProperties.LimitRule();
        otpRule.setCapacity(10);
        otpRule.setRefillTokens(5);
        otpRule.setRefillMinutes(1);

        RateLimitProperties.LimitRule signupRule = new RateLimitProperties.LimitRule();
        signupRule.setCapacity(3);
        signupRule.setRefillTokens(3);
        signupRule.setRefillMinutes(60);

        RateLimitProperties.LimitRule loginRule = new RateLimitProperties.LimitRule();
        loginRule.setCapacity(10);
        loginRule.setRefillTokens(10);
        loginRule.setRefillMinutes(15);

        RateLimitProperties.LimitRule passwordlessRule = new RateLimitProperties.LimitRule();
        passwordlessRule.setCapacity(5);
        passwordlessRule.setRefillTokens(1);
        passwordlessRule.setRefillMinutes(10);

        when(properties.getRules()).thenReturn(fullRules());

        // act & assert
        assertDoesNotThrow(() -> rateLimitBucketService.initializeConfigurations());

        verify(properties, atLeastOnce()).getRules();
    }

    @Test
    void tryConsume_shouldReturnConsumptionResult() {
        // arrange
        RateLimitProperties.LimitRule otpRule = new RateLimitProperties.LimitRule();
        otpRule.setCapacity(10);
        otpRule.setRefillTokens(5);
        otpRule.setRefillMinutes(1);

        RateLimitProperties.LimitRule signupRule = new RateLimitProperties.LimitRule();
        signupRule.setCapacity(3);
        signupRule.setRefillTokens(3);
        signupRule.setRefillMinutes(60);

        RateLimitProperties.LimitRule loginRule = new RateLimitProperties.LimitRule();
        loginRule.setCapacity(10);
        loginRule.setRefillTokens(10);
        loginRule.setRefillMinutes(15);

        RateLimitProperties.LimitRule passwordlessRule = new RateLimitProperties.LimitRule();
        passwordlessRule.setCapacity(5);
        passwordlessRule.setRefillTokens(1);
        passwordlessRule.setRefillMinutes(10);

        when(properties.getRules()).thenReturn(fullRules());
        rateLimitBucketService.initializeConfigurations();

        String key = "test@example.com";

        // act
        RateLimitBucketService.ConsumptionResult result = rateLimitBucketService.tryConsume(key, RateLimitType.OTP);

        // assert
        assertNotNull(result);
    }

    @Test
    void tryConsume_shouldHandleNullKey() {
        // arrange
        RateLimitProperties.LimitRule otpRule = new RateLimitProperties.LimitRule();
        otpRule.setCapacity(10);
        otpRule.setRefillTokens(5);
        otpRule.setRefillMinutes(1);

        RateLimitProperties.LimitRule signupRule = new RateLimitProperties.LimitRule();
        signupRule.setCapacity(3);
        signupRule.setRefillTokens(3);
        signupRule.setRefillMinutes(60);

        RateLimitProperties.LimitRule loginRule = new RateLimitProperties.LimitRule();
        loginRule.setCapacity(10);
        loginRule.setRefillTokens(10);
        loginRule.setRefillMinutes(15);

        RateLimitProperties.LimitRule passwordlessRule = new RateLimitProperties.LimitRule();
        passwordlessRule.setCapacity(5);
        passwordlessRule.setRefillTokens(1);
        passwordlessRule.setRefillMinutes(10);

        when(properties.getRules()).thenReturn(fullRules());
        rateLimitBucketService.initializeConfigurations();

        // act
        RateLimitBucketService.ConsumptionResult result = rateLimitBucketService.tryConsume(null, RateLimitType.OTP);

        // assert
        assertNotNull(result);
    }

    @Test
    void createBucketConfiguration_shouldReturnConfiguration() {
        // arrange
        RateLimitProperties.LimitRule otpRule = new RateLimitProperties.LimitRule();
        otpRule.setCapacity(10);
        otpRule.setRefillTokens(5);
        otpRule.setRefillMinutes(1);

        RateLimitProperties.LimitRule signupRule = new RateLimitProperties.LimitRule();
        signupRule.setCapacity(3);
        signupRule.setRefillTokens(3);
        signupRule.setRefillMinutes(60);

        RateLimitProperties.LimitRule loginRule = new RateLimitProperties.LimitRule();
        loginRule.setCapacity(10);
        loginRule.setRefillTokens(10);
        loginRule.setRefillMinutes(15);

        RateLimitProperties.LimitRule passwordlessRule = new RateLimitProperties.LimitRule();
        passwordlessRule.setCapacity(5);
        passwordlessRule.setRefillTokens(1);
        passwordlessRule.setRefillMinutes(10);

        when(properties.getRules()).thenReturn(fullRules());
        rateLimitBucketService.initializeConfigurations();

        // act
        var result = rateLimitBucketService.createBucketConfiguration(RateLimitType.OTP);

        // assert
        assertNotNull(result);
    }
}
//todo warnings