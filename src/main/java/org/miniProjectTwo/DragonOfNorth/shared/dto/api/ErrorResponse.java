package org.miniProjectTwo.DragonOfNorth.shared.dto.api;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

/**
 * Represents a standardized error response for API failures.
 * This record provides a consistent format for error reporting, including
 * error codes, messages, and optional validation errors.
 */
@Builder
@Schema(name = "ErrorResponse", description = "Standard error payload returned inside the API response envelope when a request fails.")
public record ErrorResponse(
        @Schema(description = "Stable application-specific error code.", example = "VAL_001")
        String code,
        @Schema(description = "Human-readable error summary.", example = "Invalid request body", nullable = true)
        String defaultMessage,
        @Schema(description = "Field-level validation issues when the request payload fails validation.", nullable = true)
        List<ValidationError> validationErrorList
) {
    /**
     * Represents a specific validation error for a particular field.
     * This nested class provides detailed information about validation
     * failures, including the field name, error code, and descriptive message.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "ValidationError", description = "Describes a single validation problem on a request field.")
    public static class ValidationError {
        /**
         * The name of the field that failed validation.
         * This identifies which input field has the validation error.
         */
        @Schema(description = "Field name that failed validation.", example = "identifier_type")
        private String field;

        /**
         * The error code identifying the specific validation rule that failed.
         * This can be used for programmatic handling of specific validation errors.
         */
        @Schema(description = "Short code or validation message for the failure.", example = "Identifier type is required")
        private String code;

        /**
         * The human-readable message describing the validation error.
         * This provides a clear explanation of what validation rule was violated.
         */
        @Schema(description = "Human-readable explanation of the validation failure.", example = "Identifier type is required")
        private String message;
    }
}
