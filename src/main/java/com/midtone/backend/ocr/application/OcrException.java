package com.midtone.backend.ocr.application;

import org.springframework.http.HttpStatus;

public class OcrException extends RuntimeException {

    private final ErrorCode errorCode;

    public OcrException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        IMAGE_REQUIRED("근무표 이미지는 필수입니다.", HttpStatus.BAD_REQUEST),
        UNSUPPORTED_IMAGE_TYPE("JPEG 또는 PNG 이미지만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST),
        IMAGE_TOO_LARGE("이미지는 최대 10MB까지 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST),
        INVALID_MONTH("month는 yyyy-MM 형식이어야 합니다.", HttpStatus.BAD_REQUEST),
        JOB_NOT_FOUND("해당 OCR 작업이 없습니다.", HttpStatus.NOT_FOUND),
        DRAFT_NOT_FOUND("해당 초안 항목이 없습니다.", HttpStatus.NOT_FOUND),
        JOB_ACCESS_DENIED("접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
        JOB_NOT_COMPLETED("분석이 완료된 작업만 검수·확정할 수 있습니다.", HttpStatus.CONFLICT),
        DRAFT_DATE_OUT_OF_MONTH("초안 날짜는 근무표의 대상 월 안에 있어야 합니다.", HttpStatus.BAD_REQUEST),
        JOB_NOT_RETRYABLE("실패했거나 완료된 작업만 재시도할 수 있습니다.", HttpStatus.CONFLICT);

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
