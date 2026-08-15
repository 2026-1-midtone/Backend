package com.midtone.backend.coaching.application;

import org.springframework.http.HttpStatus;

public class CoachingException extends RuntimeException {

    private final ErrorCode errorCode;

    public CoachingException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        SHIFT_NOT_REGISTERED("근무 일정을 먼저 등록해 주세요.", HttpStatus.CONFLICT),
        CARD_NOT_FOUND("해당 코칭 카드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
        REGENERATE_RANGE_EXCEEDED("재생성 기간은 최대 90일까지 지정할 수 있습니다.", HttpStatus.BAD_REQUEST);

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
