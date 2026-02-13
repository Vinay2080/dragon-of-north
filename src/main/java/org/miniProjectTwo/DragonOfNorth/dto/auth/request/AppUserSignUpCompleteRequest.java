package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

/**
 * Request record for completing the user sign-up process.
 *
 * <p>This DTO is used to finalize user registration after initial sign-up and verification.
 * It contains the user identifier (email or phone) and its type to identify the user account
 * that needs to be upgraded from CREATED to VERIFIED status.</p>
 *
 * <p><strong>Input Components:</strong></p>
 * <ul>
 *   <li>{@code identifier} - The user's email address or phone number used during registration</li>
 *   <li>{@code identifierType} - Enum specifying whether the identifier is EMAIL or PHONE</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <p>Used in {@code AuthenticationController.completeUserSignup()} endpoint at {@code /api/v1/auth/identifier/sign-up/complete}.
 * The service validates the identifier, verifies the user has completed all required verification steps,
 * and updates their status to VERIFIED, enabling full authentication capabilities.</p>
 *
 * <p><strong>Output Flow:</strong></p>
 * <p>When processed successfully, returns {@code AppUserStatusFinderResponse} with updated user status
 * and authentication capabilities through the standardized {@code ApiResponse} wrapper.</p>
 */
public record AppUserSignUpCompleteRequest(
        @NotBlank String identifier,
        @NotNull IdentifierType identifierType) {
}
