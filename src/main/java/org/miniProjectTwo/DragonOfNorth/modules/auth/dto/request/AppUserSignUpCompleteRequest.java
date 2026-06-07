package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

/**
 * Completes a sign-up after OTP verification has already succeeded.
 * <p>
 * Used by {@code /auth/identifier/sign-up/complete} to transition a CREATED account into an
 * active login-ready state. The identifier and identifier type must match the earlier sign-up
 * request to prevent cross-identifier activation.
 * <p>
 * Security expectations: this endpoint should only be accessible after successful OTP verification, and the identifier should be looked up and validated against the pending OTP context to ensure that only the verified identifier can be activated. Additional rate limiting may be advisable to prevent abuse of this endpoint for account activation.
 * @param identifier The email or phone number that completed OTP verification and is ready to activate the account.
 * @param identifierType The type of identifier (EMAIL or PHONE) used during sign-up, required for routing and validation.
 */
@Schema(name = "AppUserSignUpCompleteRequest", description = "Request payload for completing sign-up after OTP verification succeeds.")
public record AppUserSignUpCompleteRequest(
        @NotBlank
        @Schema(description = "Identifier that has completed OTP verification.", example = "user2@example.com")
        String identifier,
        @NotNull
        @Schema(description = "Type of identifier used during sign-up.", allowableValues = {"EMAIL", "PHONE"}, example = "EMAIL")
        IdentifierType identifierType) {
}
