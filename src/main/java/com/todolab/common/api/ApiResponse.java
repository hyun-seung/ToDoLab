package com.todolab.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        String status,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        T data,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ErrorBody error,
        LocalDateTime timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>("fail", null, new ErrorBody(errorCode.getCode(), errorCode.getMessage()), LocalDateTime.now());
    }

    public record ErrorBody(int code, String message) {}
}
