package com.mooc.app.controller;

import com.mooc.app.dto.*;
import com.mooc.app.dto.response.*;
import com.mooc.app.exception.AuthException;
import com.mooc.app.filter.RequestIdFilter;
import com.mooc.app.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-code")
    public ResponseEntity<SendCodeResponse> sendCode(
            @Valid @RequestBody SendCodeRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        authService.sendCode(request.email(), ip);
        String requestId = getRequestId(httpRequest);
        return ResponseEntity.ok(new SendCodeResponse(requestId));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        authService.register(request.email(), request.code(), request.password(), ip);
        String requestId = getRequestId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(requestId));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String ip = httpRequest.getRemoteAddr();
        AuthService.LoginResult result = authService.login(request.email(), request.password(), ip);

        Cookie cookie = new Cookie("refresh_token", result.refreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(604800);
        cookie.setAttribute("SameSite", "Strict");
        httpResponse.addCookie(cookie);

        String requestId = getRequestId(httpRequest);
        return ResponseEntity.ok(new LoginResponse(requestId, result.accessToken(), result.expiresIn()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (refreshToken == null || refreshToken.isBlank()) {
            clearRefreshCookie(httpResponse);
            throw new AuthException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token",
                    "Invalid refresh token");
        }
        try {
            AuthService.RefreshResult result = authService.refresh(refreshToken);
            String requestId = getRequestId(httpRequest);
            return ResponseEntity.ok(new RefreshResponse(requestId, result.accessToken(), result.expiresIn()));
        } catch (AuthException ex) {
            clearRefreshCookie(httpResponse);
            throw ex;
        }
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

        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        httpResponse.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserInfoResponse> getMe(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest) {
        String token = extractBearerToken(authHeader);
        AuthService.UserInfo info = authService.getMe(token);
        String requestId = getRequestId(httpRequest);
        return ResponseEntity.ok(new UserInfoResponse(
            requestId,
            info.id().toString(),
            info.email(),
            info.state(),
            info.createdAt().toString(),
            info.username(),
            info.nickname(),
            info.avatarUrl()
        ));
    }

    @DeleteMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
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

    private void clearRefreshCookie(HttpServletResponse httpResponse) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        httpResponse.addCookie(cookie);
    }

    private String getRequestId(HttpServletRequest request) {
        Object id = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return id != null ? id.toString() : "unknown";
    }
}
