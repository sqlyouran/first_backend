package com.mooc.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mooc.app.dto.response.BaseResponse;

import java.util.Map;

public class ErrorResponse extends BaseResponse {

    @JsonProperty("error_code")
    private final String errorCode;

    private final String message;

    private final Map<String, String> details;

    public ErrorResponse(String requestId, String errorCode, String message, Map<String, String> details) {
        super(requestId);
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
    }

    public ErrorResponse(String requestId, String errorCode, String message) {
        this(requestId, errorCode, message, Map.of());
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
