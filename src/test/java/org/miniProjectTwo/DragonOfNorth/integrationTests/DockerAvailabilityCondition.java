package org.miniProjectTwo.DragonOfNorth.integrationTests;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * JUnit 5 extension that checks if Docker is available before running integration tests.
 * If Docker is not available, tests will be skipped with a reason message.
 * <p>
 * Usage: Add {@link EnabledIfDockerAvailable} annotation to your test class.
 *
 * @see EnabledIfDockerAvailable
 */
@SuppressWarnings({"java:S1068"})
public class DockerAvailabilityCondition implements ExecutionCondition {

    private static final String DOCKER_CHECK_DISABLED = "docker.availability.check.disabled";
    private static volatile Boolean dockerAvailable;


    @Override
    public @NonNull ConditionEvaluationResult evaluateExecutionCondition(@NonNull ExtensionContext context) {
        // Check if docker availability check is disabled
        if (isDockerCheckDisabled()) {
            return ConditionEvaluationResult.enabled("Docker availability check is disabled");
        }

        if (isDockerAvailable()) {
            return ConditionEvaluationResult.enabled("Docker is available");
        } else {
            return ConditionEvaluationResult.disabled(
                    "Docker is not available. Please start Docker Desktop or install Docker. " +
                            "See DOCKER_TESTCONTAINERS_GUIDE.md for setup instructions."
            );
        }
    }

    private static boolean isDockerCheckDisabled() {
        return "true".equalsIgnoreCase(System.getProperty(DOCKER_CHECK_DISABLED));
    }

    public static boolean isDockerAvailable() {
        if (dockerAvailable != null) {
            return dockerAvailable;
        }

        try {
            synchronized (DockerAvailabilityCondition.class) {
                if (dockerAvailable != null) {
                    return dockerAvailable;
                }

                // Try to run 'docker ps' command
                Process process = new ProcessBuilder("docker", "ps")
                        .redirectErrorStream(true)
                        .start();

                boolean completed = process.waitFor(5, TimeUnit.SECONDS);
                dockerAvailable = completed && process.exitValue() == 0;

                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        } catch (IOException | InterruptedException e) {
            dockerAvailable = false;
        }

        return dockerAvailable;
    }
}

