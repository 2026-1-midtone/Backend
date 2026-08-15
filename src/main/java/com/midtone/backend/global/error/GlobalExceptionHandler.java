package com.midtone.backend.global.error;

import com.midtone.backend.auth.AuthException;
import com.midtone.backend.coaching.application.CoachingException;
import com.midtone.backend.nap.application.NapException;
import com.midtone.backend.routine.application.RoutineException;
import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.transition.application.TransitionException;
import com.midtone.backend.user.application.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String UNEXPECTED_ERROR_MESSAGE = "서버 오류가 발생했습니다.";
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("예상하지 못한 예외가 발생했습니다.", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(UNEXPECTED_ERROR_MESSAGE));
    }
}
