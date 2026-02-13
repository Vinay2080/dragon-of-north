package org.miniProjectTwo.DragonOfNorth.dto.api;


import lombok.*;

import java.util.List;

/**
 * Represents a standardized error response for API failures.
 * This record provides a consistent format for error reporting, including
 * error codes, messages, and optional validation errors.
 */
@Builder
public record ErrorResponse(
        String code,
        String defaultMessage,
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
    public static class ValidationError {
        /**
         * The name of the field that failed validation.
         * This identifies which input field has the validation error.
         */
        private String field;

        /**
         * The error code identifying the specific validation rule that failed.
         * This can be used for programmatic handling of specific validation errors.
         */
        private String code;

        /**
         * The human-readable message describing the validation error.
         * This provides a clear explanation of what validation rule was violated.
         */
        private String message;
    }
}
