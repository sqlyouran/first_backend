package com.mooc.app.exception;

import com.mooc.app.dto.ErrorResponse;
import com.mooc.app.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ErrorResponse body = new ErrorResponse(requestId, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(PostException.class)
    public ResponseEntity<ErrorResponse> handlePostException(PostException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ErrorResponse body = new ErrorResponse(requestId, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(CityException.class)
    public ResponseEntity<ErrorResponse> handleCityException(CityException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ErrorResponse body = new ErrorResponse(requestId, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(SpotException.class)
    public ResponseEntity<ErrorResponse> handleSpotException(SpotException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ErrorResponse body = new ErrorResponse(requestId, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(ProfileException.class)
    public ResponseEntity<ErrorResponse> handleProfileException(ProfileException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ErrorResponse body = new ErrorResponse(requestId, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ErrorResponse> handleNotificationException(NotificationException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ErrorResponse body = new ErrorResponse(requestId, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MessageException.class)
    public ResponseEntity<ErrorResponse> handleMessageException(MessageException ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        ErrorResponse body = new ErrorResponse(requestId, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        String requestId = getRequestId(request);
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            details.put(error.getField(), error.getDefaultMessage())
        );
        ErrorResponse body = new ErrorResponse(requestId, "validation_error",
                "Request validation failed", details);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String requestId = getRequestId(request);
        log.error("Unexpected error [requestId={}]", requestId, ex);
        ErrorResponse body = new ErrorResponse(requestId, "internal_error",
                "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String getRequestId(HttpServletRequest request) {
        Object id = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return id != null ? id.toString() : "unknown";
    }
}
