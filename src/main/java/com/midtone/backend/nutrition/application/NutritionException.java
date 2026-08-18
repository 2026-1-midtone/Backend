package com.midtone.backend.nutrition.application;

import org.springframework.http.HttpStatus;

public class NutritionException extends RuntimeException {
    private final ErrorCode errorCode;
    public NutritionException(ErrorCode errorCode) { super(errorCode.message); this.errorCode = errorCode; }
    public ErrorCode getErrorCode() { return errorCode; }
    public enum ErrorCode {
        INVALID_TIMING_TAG("지원하지 않는 timingTag입니다.", HttpStatus.BAD_REQUEST),
        INVALID_CONTENT_TYPE("지원하지 않는 contentType입니다.", HttpStatus.BAD_REQUEST),
        INVALID_PAGE("페이지는 0 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
        CONTENT_NOT_FOUND("해당 콘텐츠를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
        ALREADY_FAVORITED("이미 즐겨찾기한 콘텐츠입니다.", HttpStatus.CONFLICT),
        FAVORITE_NOT_FOUND("즐겨찾기 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        private final String message; private final HttpStatus status;
        ErrorCode(String message, HttpStatus status) { this.message = message; this.status = status; }
        public HttpStatus getStatus() { return status; }
    }
}
