package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

/**
 * Request record for initiating the user registration / sign-up process.
 *
 * <p>This DTO is used to create new user accounts with initially CREATED status.
 * It contains the user identifier, authentication method preference, and password
 * that will be validated and stored securely. Password must meet security requirements.</p>
 *
 * <p><strong>Input Components:</strong></p>
 * <ul>
 *   <li>{@code identifier} - The user's email address or phone number for registration</li>
 *   <li>{@code identifierType} - Enum specifying EMAIL or PHONE authentication method</li>
 *   <li>{@code password} - User password meeting security criteria (8-50 chars, mixed case, numbers, special chars)</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <p>Used in {@code AuthenticationController.signupUser()} endpoint at {@code /api/v1/auth/identifier/sign-up}.
 * The service validates the identifier uniqueness, encrypts the password, creates the user account
 * with CREATED status, and initiates a verification process (OTP/email verification).</p>
 *
 * <p><strong>Output Flow:</strong></p>
 * <p>When registration succeeds, returns {@code AppUserStatusFinderResponse} with CREATED status
 * through {@code ApiResponse} wrapper. User must complete verification via {@code AppUserSignUpCompleteRequest}
 * before full authentication capabilities are enabled.</p>
 */
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
