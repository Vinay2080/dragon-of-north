package org.miniProjectTwo.DragonOfNorth.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.miniProjectTwo.DragonOfNorth.enums.ApiResponseStatus;

import java.time.Instant;

@Getter
// todo maybe it should in common module

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final String message;

    private final ApiResponseStatus apiResponseStatus;

    private final T data;

    private final Instant time;

    private ApiResponse(String message, ApiResponseStatus apiResponseStatus, T data) {
        this.message = message;
        this.apiResponseStatus = apiResponseStatus;
        this.data = data;
        this.time = Instant.now();

    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(null, ApiResponseStatus.success, data);
    }

    public static ApiResponse<?> successMessage(String message) {
        return new ApiResponse<>(message, ApiResponseStatus.success, null);
    }

    public static ApiResponse<?> failed(String message) {
        return new ApiResponse<>(message, ApiResponseStatus.failed, null);
    }
}
