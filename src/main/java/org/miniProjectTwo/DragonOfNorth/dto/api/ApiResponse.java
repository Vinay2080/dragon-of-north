package org.miniProjectTwo.DragonOfNorth.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.miniProjectTwo.DragonOfNorth.enums.Status;

import java.time.Instant;

@Getter
// todo maybe it should in common module

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final String message;

    private final Status status;

    private final T data;

    private final Instant time;

    private ApiResponse(String message, Status status, T data) {
        this.message = message;
        this.status = status;
        this.data = data;
        this.time = Instant.now();

    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(null, Status.success, data);
    }

    public static ApiResponse<?> successMessage(String message) {
        return new ApiResponse<>(message, Status.success, null);
    }

    public static ApiResponse<?> failed(String message) {
        return new ApiResponse<>(message, Status.failed, null);
    }
}
