package com.midtone.backend.nutrition.application;

import org.springframework.http.HttpStatus;

public class NutrientException extends RuntimeException {
    private final ErrorCode errorCode;
    public NutrientException(ErrorCode errorCode) { super(errorCode.message); this.errorCode = errorCode; }
    public ErrorCode getErrorCode() { return errorCode; }
    public enum ErrorCode {
        INVALID_NUTRIENT("지원하지 않는 영양소 코드입니다.", HttpStatus.BAD_REQUEST),
        INVALID_SOURCE("출처는 USER_REPORTED 또는 HEALTH_CHECK여야 합니다.", HttpStatus.BAD_REQUEST),
        INVALID_DATE("확인일은 yyyy-MM-dd 형식의 과거 또는 오늘 날짜여야 합니다.", HttpStatus.BAD_REQUEST),
        DUPLICATE_NUTRIENT("중복된 영양소 코드가 있습니다.", HttpStatus.BAD_REQUEST),
        NOT_FOUND("해당 영양소 목표를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        private final String message; private final HttpStatus status;
        ErrorCode(String message, HttpStatus status) { this.message = message; this.status = status; }
        public HttpStatus getStatus() { return status; }
    }
}
