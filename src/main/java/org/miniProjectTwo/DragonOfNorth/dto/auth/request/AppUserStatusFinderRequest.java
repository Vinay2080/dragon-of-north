package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

/**
 * Request record for checking user registration status and existence.
 *
 * <p>This DTO is used to query the current status of a user identifier to determine
 * if the user exists, their registration stage, and what authentication steps are required.
 * Essential for frontend logic to guide users through appropriate authentication flows.</p>
 *
 * <p><strong>Input Components:</strong></p>
 * <ul>
 *   <li>{@code identifier} - The user's email address or phone number to check</li>
 *   <li>{@code identifierType} - Enum specifying whether the identifier is EMAIL or PHONE</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <p>Used in {@code AuthenticationController.findUserStatus()} endpoint at {@code /api/v1/auth/identifier/status}.
 * The service checks if the identifier exists in the system and returns the current registration status
 * (NOT_FOUND, CREATED, VERIFIED) to determine next authentication steps.</p>
 *
 * <p><strong>Output Flow:</strong></p>
 * <p>Returns {@code AppUserStatusFinderResponse} containing user status, authentication method availability,
 * and recommended next actions through the standardized {@code ApiResponse} wrapper.
 * This enables the frontend to display the appropriate UI (login, sign-up, or verification prompts).</p>
 */
public record AppUserStatusFinderRequest(
        @NotBlank(message = "identifier cannot be blank")
        String identifier,

        @NotNull(message = "Identifier type is required")
        IdentifierType identifierType) {
}