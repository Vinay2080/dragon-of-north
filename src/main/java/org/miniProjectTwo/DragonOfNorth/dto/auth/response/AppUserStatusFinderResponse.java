package org.miniProjectTwo.DragonOfNorth.dto.auth.response;

import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;

/**
 * Response record for user status information and authentication availability.
 *
 * <p>This DTO returns the current registration status of a user identifier and indicates
 * what authentication methods are available. Essential for frontend logic to determine
 * the appropriate next steps in the user authentication flow.</p>
 *
 * <p><strong>Output Components:</strong></p>
 * <ul>
 *   <li>{@code appUserStatus} - Current registration status (NOT_FOUND, CREATED, VERIFIED)</li>
 * </ul>
 *
 * <p><strong>Usage Context:</strong></p>
 * <p>Returned by {@code AuthenticationController.findUserStatus()}, {@code signupUser()}, and {@code completeUserSignup()}
 * endpoints. The status guides the frontend to display appropriate UI elements:
 * NOT_FOUND → show a sign-up form, CREATED → show verification prompt, VERIFIED → show a login form.</p>
 *
 * <p><strong>Integration Flow:</strong></p>
 * <p>Always wrapped in {@code ApiResponse} for consistent API response format.
 * Works with {@code AuthenticationServiceResolver} to support both email and phone
 * authentication methods with unified status reporting.</p>
 */
public record AppUserStatusFinderResponse(@NotNull AppUserStatus appUserStatus) {
}