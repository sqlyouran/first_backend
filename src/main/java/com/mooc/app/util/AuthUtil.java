package com.mooc.app.util;

import com.mooc.app.exception.AuthException;
import com.mooc.app.filter.RequestIdFilter;
import com.mooc.app.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

public final class AuthUtil {

    private AuthUtil() {}

    public static UUID requireUserId(HttpServletRequest request, JwtService jwtService) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "unauthorized",
                    "Missing or invalid authorization header");
        }
        String token = authHeader.substring(7);
        return jwtService.parseToken(token)
                .map(Claims::getSubject)
                .map(UUID::fromString)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "unauthorized",
                        "Invalid or expired token"));
    }

    public static Optional<UUID> optionalUserId(HttpServletRequest request, JwtService jwtService) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authHeader.substring(7);
        return jwtService.parseToken(token)
                .map(Claims::getSubject)
                .map(UUID::fromString);
    }

    public static String getRequestId(HttpServletRequest request) {
        Object id = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return id != null ? id.toString() : "unknown";
    }
}
