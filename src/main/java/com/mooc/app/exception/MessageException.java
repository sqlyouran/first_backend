package com.mooc.app.exception;

import org.springframework.http.HttpStatus;

public class MessageException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public MessageException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
