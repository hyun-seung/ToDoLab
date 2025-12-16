package com.todolab.common.api;

import com.todolab.task.exception.TaskValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebInputException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    // Bean Validation (@NotBlank 등) 에러 처리
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<?>> handleBindException(WebExchangeBindException e) {
        FieldError error = e.getFieldErrors().getFirst();

        String detail = error.getField() + ": " + error.getDefaultMessage();

        log.error("Validation Failed : {}", detail);
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(TaskValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleTaskValidationException(TaskValidationException e) {
        log.error("Task Validation Failed : {}", e.getDetail());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(MissingRequestValueException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingRequestValueException(MissingRequestValueException e) {
        log.error("Missing Validation Failed : {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiResponse<?>> handleServerWebInputException(ServerWebInputException e) {
        // PathVariable/RequestParam 타입 미스매치, 바인딩 실패 등
        log.error("Wrong Request : {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unhandled Exception", e);
        return ResponseEntity.internalServerError().body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR));
    }
}
