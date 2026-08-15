package com.midtone.backend.transition.application;

import org.springframework.http.HttpStatus;

public class TransitionException extends RuntimeException {

    private final ErrorCode errorCode;

    public TransitionException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        NOT_A_TRANSITION_DAY("해당 날짜는 전환일이 아닙니다.", HttpStatus.NOT_FOUND);

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
