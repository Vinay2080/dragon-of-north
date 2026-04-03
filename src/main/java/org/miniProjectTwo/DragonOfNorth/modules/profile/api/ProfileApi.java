package org.miniProjectTwo.DragonOfNorth.modules.profile.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.miniProjectTwo.DragonOfNorth.modules.profile.dto.UpdateProfileRequest;
import org.miniProjectTwo.DragonOfNorth.modules.profile.dto.response.GetProfileResponse;

@Tag(name = "Profile", description = "Read and update the authenticated user's profile.")
@SecurityRequirement(name = "accessTokenCookie")
public interface ProfileApi {

    @Operation(
            summary = "Update my profile",
            description = "Updates the authenticated user's profile fields. Any omitted field is left unchanged."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile updated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "profileUpdated",
                                    value = """
                                            {
                                              "api_response_status": "success",
                                              "data": {
                                                "username": "arya_north",
                                                "display_name": "Arya Stark",
                                                "bio": "Explorer, archer, and dragon rider in training.",
                                                "avatar_url": "https://cdn.dragonofnorth.dev/avatars/arya.png",
                                                "auth_provider": "LOCAL"
                                              },
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "409", description = "Username is already taken")
    })
    org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<GetProfileResponse> updateProfile(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Subset of profile fields to update.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "updateProfileRequest",
                                    value = """
                                            {
                                              "display_name": "Arya Stark",
                                              "avatar_url": "https://cdn.dragonofnorth.dev/avatars/arya.png",
                                              "bio": "Explorer, archer, and dragon rider in training.",
                                              "username": "arya_north"
                                            }
                                            """
                            )
                    )
            )
            UpdateProfileRequest request
    );

    @Operation(
            summary = "Get my profile",
            description = "Returns the authenticated user's profile together with the detected authentication provider."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile returned",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "profileFound",
                                    value = """
                                            {
                                              "api_response_status": "success",
                                              "data": {
                                                "username": "arya_north",
                                                "display_name": "Arya Stark",
                                                "bio": "Explorer, archer, and dragon rider in training.",
                                                "avatar_url": "https://cdn.dragonofnorth.dev/avatars/arya.png",
                                                "auth_provider": "LOCAL"
                                              },
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Profile or user record was not found")
    })
    org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<GetProfileResponse> getProfile();
}
