package com.mooc.app.controller;

import com.mooc.app.dto.UpdateProfileRequest;
import com.mooc.app.dto.response.InterestTagListResponse;
import com.mooc.app.dto.response.ProfileResponse;
import com.mooc.app.dto.response.PublicProfileResponse;
import com.mooc.app.exception.ProfileException;
import com.mooc.app.service.JwtService;
import com.mooc.app.service.ProfileService;
import com.mooc.app.util.AuthUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class ProfileController {

    private final ProfileService profileService;
    private final JwtService jwtService;

    public ProfileController(ProfileService profileService, JwtService jwtService) {
        this.profileService = profileService;
        this.jwtService = jwtService;
    }

    @GetMapping("/me/profile")
    public ResponseEntity<ProfileResponse> getMyProfile(HttpServletRequest httpRequest) {
        UUID userId = requireUserId(httpRequest);
        String requestId = AuthUtil.getRequestId(httpRequest);
        ProfileResponse response = profileService.getMyProfile(userId, requestId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/profile")
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = requireUserId(httpRequest);
        String requestId = AuthUtil.getRequestId(httpRequest);
        ProfileResponse response = profileService.updateMyProfile(userId, request, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/interest-tags")
    public ResponseEntity<InterestTagListResponse> getInterestTags(HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        InterestTagListResponse response = profileService.getInterestTags(requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{username}")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(
            @PathVariable String username,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        PublicProfileResponse response = profileService.getPublicProfile(username, requestId);
        return ResponseEntity.ok(response);
    }

    private UUID requireUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ProfileException(HttpStatus.UNAUTHORIZED, "unauthorized",
                    "Missing or invalid authorization header");
        }
        String token = authHeader.substring(7);
        return jwtService.parseToken(token)
                .map(Claims::getSubject)
                .map(UUID::fromString)
                .orElseThrow(() -> new ProfileException(HttpStatus.UNAUTHORIZED, "unauthorized",
                        "Invalid or expired token"));
    }
}
