package com.midtone.backend.shift.application;

import org.springframework.http.HttpStatus;

public class ShiftException extends RuntimeException {

    private final ErrorCode errorCode;

    public ShiftException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        DUPLICATE_SHIFT("해당 날짜에 이미 근무 일정이 있습니다.", HttpStatus.CONFLICT),
        INVALID_DATE_RANGE("종료일은 시작일보다 빠를 수 없습니다.", HttpStatus.BAD_REQUEST),
        SHIFT_NOT_FOUND("해당 근무 일정이 없습니다.", HttpStatus.NOT_FOUND),
        SHIFT_ACCESS_DENIED("접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
        BULK_UPDATE_RANGE_EXCEEDED("변경 기간은 최대 90일까지 지정할 수 있습니다.", HttpStatus.BAD_REQUEST),
        SHIFT_PATTERN_NOT_FOUND("해당 패턴을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
        PATTERN_NAME_REQUIRED("저장할 패턴 이름은 필수입니다.", HttpStatus.BAD_REQUEST);

        private final String message;
        private final HttpStatus status;

        ErrorCode(String message, HttpStatus status) {
            this.message = message;
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}
