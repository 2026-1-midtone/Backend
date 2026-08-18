package com.midtone.backend.global.error;

import com.midtone.backend.auth.AuthException;
import com.midtone.backend.coaching.application.CoachingException;
import com.midtone.backend.nap.application.NapException;
import com.midtone.backend.ocr.application.OcrException;
import com.midtone.backend.routine.application.RoutineException;
import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.transition.application.TransitionException;
import com.midtone.backend.user.application.UserException;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String UNEXPECTED_ERROR_MESSAGE = "서버 오류가 발생했습니다.";
    private static final String RESOURCE_NOT_FOUND_MESSAGE = "요청한 리소스를 찾을 수 없습니다.";
    private static final String INVALID_REQUEST_MESSAGE = "요청 값이 올바르지 않습니다.";
    private static final String INVALID_DATE_TIME_MESSAGE = "날짜 또는 시각 형식이 올바르지 않습니다.";
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(RESOURCE_NOT_FOUND_MESSAGE));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> handleDateTimeParse(DateTimeParseException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(INVALID_DATE_TIME_MESSAGE));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(firstErrorMessage(exception)));
    }

    private String firstErrorMessage(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError != null) {
            return fieldError.getDefaultMessage();
        }
        return exception.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(ObjectError::getDefaultMessage)
                .orElse(INVALID_REQUEST_MESSAGE);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticated(UnauthenticatedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException exception) {
        return ResponseEntity.status(exception.getErrorCode().getStatus())
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(UserException exception) {
        return ResponseEntity.status(exception.getErrorCode().getStatus())
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(ShiftException.class)
    public ResponseEntity<ErrorResponse> handleShiftException(ShiftException exception) {
        return ResponseEntity.status(exception.getErrorCode().getStatus())
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(CoachingException.class)
    public ResponseEntity<ErrorResponse> handleCoachingException(CoachingException exception) {
        return ResponseEntity.status(exception.getErrorCode().getStatus())
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(TransitionException.class)
    public ResponseEntity<ErrorResponse> handleTransitionException(TransitionException exception) {
        return ResponseEntity.status(exception.getErrorCode().getStatus())
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(NapException.class)
    public ResponseEntity<ErrorResponse> handleNapException(NapException exception) {
        return ResponseEntity.status(exception.getErrorCode().getStatus())
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(RoutineException.class)
    public ResponseEntity<ErrorResponse> handleRoutineException(RoutineException exception) {
        return ResponseEntity.status(exception.getErrorCode().getStatus())
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(OcrException.class)
    public ResponseEntity<ErrorResponse> handleOcrException(OcrException exception) {
        return ResponseEntity.status(exception.getErrorCode().getStatus())
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("예상하지 못한 예외가 발생했습니다.", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(UNEXPECTED_ERROR_MESSAGE));
    }
}
