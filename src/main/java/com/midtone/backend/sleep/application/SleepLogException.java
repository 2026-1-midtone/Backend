package com.midtone.backend.sleep.application;

import org.springframework.http.HttpStatus;

public class SleepLogException extends RuntimeException {

    private final ErrorCode errorCode;

    public SleepLogException(ErrorCode errorCode) {
        super(errorCode.message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        INVALID_INTERVAL("기상 시각은 취침 시각보다 늦어야 합니다.", HttpStatus.BAD_REQUEST),
        FUTURE_RECORD("미래의 수면 기록은 등록할 수 없습니다.", HttpStatus.BAD_REQUEST),
        INVALID_SOURCE("source는 MANUAL 또는 DEVICE여야 합니다.", HttpStatus.BAD_REQUEST),
        INVALID_RANGE("조회 종료일은 시작일보다 빠를 수 없습니다.", HttpStatus.BAD_REQUEST),
        OVERLAPPING_LOG("기존 수면 기록과 시간이 겹칩니다.", HttpStatus.CONFLICT),
        LOG_NOT_FOUND("해당 수면 기록을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
        USER_NOT_FOUND("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
        ACCESS_DENIED("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);

        private final String message;
        private final HttpStatus status;

        ErrorCode(String message, HttpStatus status) {
            this.message = message;
            this.status = status;
        }

        public HttpStatus getStatus() { return status; }
        public String getMessage() { return message; }
    }
}
