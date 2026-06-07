package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

/**
 * Input for pre-auth account discovery checks.
 * <p>
 * Used before login/sign-up branches to determine whether an identifier already exists and which
 * flow the client should render. Validation rejects blank identifiers and missing identifier types.
 * <p>
 * Security expectations: this endpoint should be rate limited to prevent abuse for user enumeration attacks, and the response should be generic (e.g. "identifier status checked") without revealing whether the account exists or not. The client should use the response to route the user to either the login or sign-up flow without disclosing the existence of the account.
 *
 * @param identifier The email or phone number to check for an existing account.
 * @param identifierType The type of identifier (EMAIL or PHONE) to route the check and determine the appropriate authentication flow. Required for validation and routing.
 */
@Schema(name = "AppUserStatusFinderRequest", description = "Request payload used to inspect whether an account exists for an email address or phone number.")
public record AppUserStatusFinderRequest(
        @NotBlank(message = "identifier cannot be blank")
        @Schema(description = "Identifier to search. Can be email or phone number.", example = "intern.candidate@example.com")
        String identifier,

        @NotNull(message = "Identifier type is required")
        @Schema(description = "Identifier type for routing the auth flow.", allowableValues = {"EMAIL", "PHONE"}, example = "EMAIL")
        IdentifierType identifierType) {
}
