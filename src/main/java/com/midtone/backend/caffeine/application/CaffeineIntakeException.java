package com.midtone.backend.caffeine.application;

import org.springframework.http.HttpStatus;

public class CaffeineIntakeException extends RuntimeException {

    private final ErrorCode errorCode;

    public CaffeineIntakeException(ErrorCode errorCode) {
        super(errorCode.message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        INVALID_AMOUNT("amountMg는 1 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
        INVALID_SERVINGS("servings는 0보다 크고 소수점 둘째 자리까지 입력해야 합니다.", HttpStatus.BAD_REQUEST),
        FUTURE_RECORD("미래의 카페인 섭취 기록은 등록할 수 없습니다.", HttpStatus.BAD_REQUEST),
        INVALID_RANGE("조회 종료일은 시작일보다 빠를 수 없습니다.", HttpStatus.BAD_REQUEST),
        INTAKE_NOT_FOUND("해당 카페인 섭취 기록을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
        USER_NOT_FOUND("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
        ACCESS_DENIED("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);

        private final String message;
        private final HttpStatus status;

        ErrorCode(String message, HttpStatus status) {
            this.message = message;
            this.status = status;
        }

        public String getMessage() { return message; }
        public HttpStatus getStatus() { return status; }
    }
}
