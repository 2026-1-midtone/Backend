package com.midtone.backend.chat.application;

import org.springframework.http.HttpStatus;

public class ChatException extends RuntimeException {
    private final ErrorCode errorCode;
    public ChatException(ErrorCode errorCode) { super(errorCode.message); this.errorCode = errorCode; }
    public ErrorCode getErrorCode() { return errorCode; }
    public enum ErrorCode {
        NOT_FOUND("해당 메시지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
        ASSISTANT_ONLY("답변 메시지에만 피드백할 수 있습니다.", HttpStatus.BAD_REQUEST),
        INVALID_FEEDBACK("피드백은 LIKE 또는 DISLIKE만 가능합니다.", HttpStatus.BAD_REQUEST),
        INVALID_CURSOR("잘못된 대화 커서입니다.", HttpStatus.BAD_REQUEST),
        INVALID_SIZE("조회 개수는 1에서 50 사이여야 합니다.", HttpStatus.BAD_REQUEST),
        GENERATION_UNAVAILABLE("AI 답변을 생성할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE);
        private final String message; private final HttpStatus status;
        ErrorCode(String message, HttpStatus status) { this.message = message; this.status = status; }
        public HttpStatus getStatus() { return status; }
    }
}
