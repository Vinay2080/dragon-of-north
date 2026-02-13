package org.miniProjectTwo.DragonOfNorth.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.miniProjectTwo.DragonOfNorth.enums.ApiResponseStatus;

import java.time.Instant;

/**
 * A generic API response wrapper that standardizes the structure of all API responses.
 * This class provides a consistent format for both successful and failed API calls,
 * including optional message, status, data payload, and timestamp.
 *
 * @param <T> the type of the data payload contained in the response
 *            JsonInclude(JsonInclude.Include.NON_NULL) ensures that null fields are not included in JSON serialization
 */
@Getter
// todo maybe it should in common module

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Optional message providing additional context about the API response.
     * Can be null for responses where only the data payload is needed.
     */
    private final String message;

    /**
     * The status of the API response indicating success or failure.
     */
    private final ApiResponseStatus apiResponseStatus;

    /**
     * The data payload returned by the API.
     * Can be null for responses that only contain a message or status.
     */
    private final T data;

    /**
     * The timestamp when this response was created.
     * Automatically set to the current time when the response is instantiated.
     */
    private final Instant time;

    /**
     * Private constructor to create an ApiResponse instance.
     * Automatically sets the timestamp to the current time.
     *
     * @param message           optional message providing additional context
     * @param apiResponseStatus the status of the API response
     * @param data              the data payload to be returned
     */
    private ApiResponse(String message, ApiResponseStatus apiResponseStatus, T data) {
        this.message = message;
        this.apiResponseStatus = apiResponseStatus;
        this.data = data;
        this.time = Instant.now();
    }

    /**
     * Creates a successful API response containing data.
     * The message is set to null and the status is set to success.
     *
     * @param <T> the type of the data payload
     * @param data the data payload to be returned in the response
     * @return a successful ApiResponse containing the provided data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(null, ApiResponseStatus.success, data);
    }

    /**
     * Creates a successful API response containing only a message.
     * The data payload is set to null and status is set to success.
     *
     * @param message the success message to be returned in the response
     * @return a successful ApiResponse containing the provided message
     */
    public static ApiResponse<?> successMessage(String message) {
        return new ApiResponse<>(message, ApiResponseStatus.success, null);
    }

    /**
     * Creates a failed API response containing error data.
     * The message is set to null and the status is set to failed.
     *
     * @param <T> the type of the error data payload
     * @param data the error data payload to be returned in the response
     * @return a failed ApiResponse containing the provided error data
     */
    public static <T> ApiResponse<T> failed(T data) {
        return new ApiResponse<>(null, ApiResponseStatus.failed, data);
    }
}
