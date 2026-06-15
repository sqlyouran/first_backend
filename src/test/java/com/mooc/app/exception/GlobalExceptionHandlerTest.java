package com.mooc.app.exception;

import com.mooc.app.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getAttribute("requestId")).thenReturn("test-request-id");
    }

    @Test
    void handleCityException_mapsToErrorResponse() {
        CityException ex = new CityException(HttpStatus.NOT_FOUND, "not_found", "City not found");

        ResponseEntity<ErrorResponse> response = handler.handleCityException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test-request-id", response.getBody().getRequestId());
        assertEquals("not_found", response.getBody().getErrorCode());
        assertEquals("City not found", response.getBody().getMessage());
    }

    @Test
    void handleSpotException_mapsToErrorResponse() {
        SpotException ex = new SpotException(HttpStatus.NOT_FOUND, "not_found", "Spot not found");

        ResponseEntity<ErrorResponse> response = handler.handleSpotException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test-request-id", response.getBody().getRequestId());
        assertEquals("not_found", response.getBody().getErrorCode());
        assertEquals("Spot not found", response.getBody().getMessage());
    }
}
