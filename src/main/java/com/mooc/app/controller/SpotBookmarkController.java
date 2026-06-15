package com.mooc.app.controller;

import com.mooc.app.dto.response.BookmarkResponse;
import com.mooc.app.entity.EntityType;
import com.mooc.app.service.BookmarkService;
import com.mooc.app.service.JwtService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
public class SpotBookmarkController {

    private final BookmarkService bookmarkService;
    private final JwtService jwtService;

    public SpotBookmarkController(BookmarkService bookmarkService, JwtService jwtService) {
        this.bookmarkService = bookmarkService;
        this.jwtService = jwtService;
    }

    @GetMapping("/api/spots/{spotId}/bookmark-status")
    public ResponseEntity<BookmarkResponse> getBookmarkStatus(
            @PathVariable UUID spotId,
            HttpServletRequest httpRequest) {
        Optional<UUID> optionalUserId = AuthUtil.optionalUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        BookmarkResponse response = bookmarkService.getBookmarkStatus(spotId, EntityType.SPOT, optionalUserId, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/spots/{spotId}/bookmark")
    public ResponseEntity<BookmarkResponse> toggleBookmark(
            @PathVariable UUID spotId,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        BookmarkResponse response = bookmarkService.toggleBookmark(spotId, EntityType.SPOT, userId, requestId);
        return ResponseEntity.ok(response);
    }
}
