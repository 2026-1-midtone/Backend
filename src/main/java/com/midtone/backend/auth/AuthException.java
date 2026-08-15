package com.midtone.backend.auth;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final ErrorCode errorCode;

    public AuthException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AuthException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        INVALID_GOOGLE_TOKEN("유효하지 않은 구글 토큰입니다.", HttpStatus.UNAUTHORIZED),
        GOOGLE_TOKEN_VERIFICATION_FAILED("구글 토큰 검증 중 오류가 발생했습니다.", HttpStatus.UNAUTHORIZED),
        INVALID_REFRESH_TOKEN("만료된 리프레시 토큰입니다. 다시 로그인해 주세요.", HttpStatus.UNAUTHORIZED);

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
