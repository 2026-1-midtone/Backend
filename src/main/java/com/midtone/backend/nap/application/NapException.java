package com.midtone.backend.nap.application;

import org.springframework.http.HttpStatus;

public class NapException extends RuntimeException {

    private final ErrorCode errorCode;

    public NapException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        ALREADY_RUNNING("이미 진행 중인 낮잠 타이머가 있습니다.", HttpStatus.CONFLICT),
        USER_SETTINGS_NOT_FOUND("사용자 설정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
        DAILY_LIMIT_EXCEEDED("오늘 설정한 최대 낮잠 횟수를 모두 사용했어요.", HttpStatus.CONFLICT),
        INVALID_DURATION("낮잠 시간은 1분 이상 180분 이하여야 합니다.", HttpStatus.BAD_REQUEST),
        INVALID_STATUS("낮잠 상태는 COMPLETED 또는 CANCELED여야 합니다.", HttpStatus.BAD_REQUEST),
        NAP_NOT_FOUND("해당 낮잠 세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
        ACCESS_DENIED("접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
        ALREADY_FINISHED("이미 종료된 낮잠 세션입니다.", HttpStatus.CONFLICT);

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
