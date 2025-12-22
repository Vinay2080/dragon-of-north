package org.miniProjectTwo.DragonOfNorth.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "auth")
@Getter
@Setter
public class AuthRateLimitProperties {

    Signup signup = new Signup();
    Login login = new Login();

    @Getter
    @Setter
    public static class Signup {
        private int requestWindowSeconds;
        private int maxRequestsPerWindow;
        private int blockDurationMinutes;

    }

    @Getter
    @Setter
    public static class Login {
        private int maxFailedAttempts;
        private int blockDurationMinutes;
    }
}
