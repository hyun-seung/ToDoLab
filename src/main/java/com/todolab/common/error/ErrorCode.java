package com.todolab.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 검증 에러
    INVALID_INPUT(10001, "필수값이 없습니다."),
    REQUIRED_VALUE_MISSING(10002, "값이 올바르지 않습니다."),

    // 서버 내부 오류
    INTERNAL_ERROR(99999, "서버 오류가 발생했습니다.");

    private final int code;
    private final String message;


}
