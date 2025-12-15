package com.todolab.task.exception;

import com.todolab.common.api.ErrorCode;
import lombok.Getter;

@Getter
public class TaskNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    public TaskNotFoundException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
