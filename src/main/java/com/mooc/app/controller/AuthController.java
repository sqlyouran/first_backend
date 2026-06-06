package com.mooc.app.controller;

import com.mooc.app.dto.*;
import com.mooc.app.exception.AuthException;
import com.mooc.app.filter.RequestIdFilter;
import com.mooc.app.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-code")
    public ResponseEntity<Map<String, String>> sendCode(
            @Valid @RequestBody SendCodeRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        authService.sendCode(request.email(), ip);
        String requestId = getRequestId(httpRequest);
        return ResponseEntity.ok(Map.of("request_id", requestId));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        authService.register(request.email(), request.code(), request.password(), ip);
        String requestId = getRequestId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("request_id", requestId));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String ip = httpRequest.getRemoteAddr();
        AuthService.LoginResult result = authService.login(request.email(), request.password(), ip);

        // Set refresh_token cookie
        Cookie cookie = new Cookie("refresh_token", result.refreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(604800);
        cookie.setAttribute("SameSite", "Strict");
        httpResponse.addCookie(cookie);

        String requestId = getRequestId(httpRequest);
        return ResponseEntity.ok(Map.of(
            "request_id", requestId,
            "access_token", result.accessToken(),
            "expires_in", result.expiresIn()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token",
                    "Invalid refresh token");
        }
        AuthService.RefreshResult result = authService.refresh(refreshToken);
        String requestId = getRequestId(httpRequest);
        return ResponseEntity.ok(Map.of(
            "request_id", requestId,
            "access_token", result.accessToken(),
            "expires_in", result.expiresIn()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse httpResponse) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token",
                    "Invalid refresh token");
        }
        authService.logout(refreshToken);

        // Clear cookie
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        httpResponse.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest) {
        String token = extractBearerToken(authHeader);
        AuthService.UserInfo info = authService.getMe(token);
        String requestId = getRequestId(httpRequest);
        return ResponseEntity.ok(Map.of(
            "request_id", requestId,
            "id", info.id().toString(),
            "email", info.email(),
            "state", info.state(),
            "created_at", info.createdAt().toString()
        ));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractBearerToken(authHeader);
        authService.deleteMe(token);
        return ResponseEntity.noContent().build();
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials",
                    "Missing or invalid authorization header");
        }
        return authHeader.substring(7);
    }

    private String getRequestId(HttpServletRequest request) {
        Object id = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return id != null ? id.toString() : "unknown";
    }
}
