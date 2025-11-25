package com.todolab.common.api;

import com.todolab.task.exception.TaskValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    // Bean Validation (@NotBlank 등) 에러 처리
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<?>> handleBindException(WebExchangeBindException e) {
        FieldError error = e.getFieldErrors().getFirst();

        StringBuilder sb = new StringBuilder();
        sb.append(error.getField()).append(": ").append(error.getDefaultMessage());
        String detail = sb.toString();

        log.error("Validation Failed : {}", detail);
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(TaskValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleTaskValidationException(TaskValidationException e) {
        log.error("Task Validation Failed : {}", e.getDetail());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT));
    }



    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unhandled Exception", e);
        return ResponseEntity.internalServerError().body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR));
    }
}
