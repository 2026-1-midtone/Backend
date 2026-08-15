package com.midtone.backend.routine.application;

import org.springframework.http.HttpStatus;

public class RoutineException extends RuntimeException {

    private final ErrorCode errorCode;

    public RoutineException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        INVALID_STATUS("루틴 상태는 PENDING, DONE, SKIPPED 중 하나여야 합니다.", HttpStatus.BAD_REQUEST),
        TASK_NOT_FOUND("해당 루틴 항목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
        ACCESS_DENIED("접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
        INVALID_PERIOD("period는 7d 또는 30d만 지원합니다.", HttpStatus.BAD_REQUEST);

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
