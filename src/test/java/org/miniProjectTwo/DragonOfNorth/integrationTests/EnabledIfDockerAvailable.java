package org.miniProjectTwo.DragonOfNorth.integrationTests;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable Docker availability check for integration tests.
 * <p>
 * Usage:
 * <pre>{@code
 * @SpringBootTest
 * @EnabledIfDockerAvailable
 * class MyIntegrationTest {
 *     // test methods
 * }
 * }</pre>
 * <p>
 * When Docker is not available, all test methods in the class will be skipped.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DockerAvailabilityCondition.class)
public @interface EnabledIfDockerAvailable {
}

