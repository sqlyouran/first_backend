package com.mooc.app.dto;

import java.util.Map;

public record ErrorResponse(
    String request_id,
    String error_code,
    String message,
    Map<String, String> details
) {
    public ErrorResponse(String requestId, String errorCode, String message) {
        this(requestId, errorCode, message, Map.of());
    }
}
