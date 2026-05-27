package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.time.Instant;
import java.util.List;

/**
 * Error payload returned when a protected endpoint requires step-up MFA.
 */
@Schema(name = "StepUpRequiredResponse", description = "Step-up MFA challenge details returned with a 403 response.")
public record StepUpRequiredResponse(
        @Schema(description = "Stable application-specific error code.", example = "AUTH_028")
        String code,
        @Schema(description = "Human-readable error summary.")
        String defaultMessage,
        @Schema(description = "Opaque step-up challenge token.")
        String challengeId,
        @Schema(description = "Allowed MFA providers for this challenge.")
        List<ProviderType> availableMethods,
        @Schema(description = "Challenge expiration timestamp.")
        Instant expiresAt
) {
    public StepUpRequiredResponse {
        availableMethods = availableMethods == null ? List.of() : List.copyOf(availableMethods);
    }

    public static StepUpRequiredResponse from(ErrorCode errorCode, MfaChallenge challenge) {
        if (errorCode == null) {
            throw new IllegalArgumentException("errorCode must not be null");
        }
        if (challenge == null) {
            return new StepUpRequiredResponse(errorCode.getCode(), errorCode.getDefaultMessage(), null, List.of(), null);
        }
        return new StepUpRequiredResponse(
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                challenge.mfaToken(),
                challenge.availableMethods(),
                challenge.expiresAt()
        );
    }
}
