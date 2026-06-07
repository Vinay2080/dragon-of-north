package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

/**
 * Request record for initiating the user registration / sign-up process.
 *
 * <p>This DTO is used to create new user accounts with initially CREATED status.
 * It contains the user identifier, authentication method preference, and password
 * that will be validated and stored securely. Password must meet security requirements.</p>

 *
 * <p><strong>Usage:</strong></p>
 * <p>Used in {@code AuthenticationController.signupUser()} endpoint at {@code /api/v1/auth/identifier/sign-up}.
 * The service validates the identifier uniqueness, encrypts the password, creates the user account
 * with CREATED status, and initiates a verification process (OTP/email verification).</p>
 *
 * <p><strong>Lifecycle Note:</strong></p>
 * This request enters at the sign-up start endpoint and transitions the account state into a
 * verification-required phase. Completion requires OTP verification plus the sign-up-complete call.
 * Password content is security-sensitive and must remain redacted in logs/telemetry.
 * Security expectations: this endpoint should only be accessible after successful OTP verification, and the identifier should be looked up and validated against the pending OTP context to ensure that only the verified identifier can be activated. Additional rate limiting may be advisable to prevent abuse of this endpoint for account activation.
 * @param identifier The email address or phone number used as the username for registration. Must be unique and non-blank.
 * @param identifierType The type of identifier (EMAIL or PHONE) to determine the authentication method and verification flow. Required for routing and validation.
 * @param password The user's chosen password for authentication. Must be 8-50 characters long and include uppercase, lowercase, numeric, and special characters to meet security requirements. Must be non-blank and is sensitive information that should not be logged.
 */
@Schema(name = "AppUserSignUpRequest", description = "Request payload for starting a local identifier-based sign-up flow.")
public record AppUserSignUpRequest(

        @NotBlank
        @Schema(description = "Email address or phone number used as username.", example = "intern.candidate@example.com")
        String identifier,

        @NotNull(message = "identifier type cannot be null")
        @Schema(description = "Type of identifier. Use EMAIL or PHONE.", allowableValues = {"EMAIL", "PHONE"}, example = "EMAIL")
        IdentifierType identifierType,

        @Size(message = "password length must be between 8 and 50", min = 8, max = 50)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = """
                At least one lowercase letter
                At least one uppercase letter
                At least one number
                At least one special character""")
        @NotBlank(message = "password cannot be blank")
        @Schema(description = "Strong password: 8-50 chars with upper, lower, number, and special character.", example = "Intern@123")
        String password) {
}
