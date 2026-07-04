package com.mooc.app.exception;

import org.springframework.http.HttpStatus;

public class AiPostAssistException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public AiPostAssistException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
