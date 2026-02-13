package org.miniProjectTwo.DragonOfNorth.enums;

import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;

/**
 * Standardized API response status for consistent client handling.
 * Determines response processing flow and UI state management. Success indicates
 * request completion, while failed triggers error handling with ErrorResponse details.
 *
 * @see ApiResponse for wrapper implementation
 */
public enum ApiResponseStatus {
    success,
    failed
}
