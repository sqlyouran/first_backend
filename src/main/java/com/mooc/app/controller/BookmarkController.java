package com.mooc.app.controller;

import com.mooc.app.dto.response.BookmarkListResponse;
import com.mooc.app.dto.response.BookmarkResponse;
import com.mooc.app.entity.EntityType;
import com.mooc.app.exception.PostException;
import com.mooc.app.service.BookmarkService;
import com.mooc.app.service.JwtService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final JwtService jwtService;

    public BookmarkController(BookmarkService bookmarkService, JwtService jwtService) {
        this.bookmarkService = bookmarkService;
        this.jwtService = jwtService;
    }

    @GetMapping("/api/posts/{postId}/bookmark-status")
    public ResponseEntity<BookmarkResponse> getBookmarkStatus(
            @PathVariable UUID postId,
            HttpServletRequest httpRequest) {
        Optional<UUID> optionalUserId = AuthUtil.optionalUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        BookmarkResponse response = bookmarkService.getBookmarkStatus(postId, EntityType.POST, optionalUserId, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/posts/{postId}/bookmark")
    public ResponseEntity<BookmarkResponse> toggleBookmark(
            @PathVariable UUID postId,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        BookmarkResponse response = bookmarkService.toggleBookmark(postId, EntityType.POST, userId, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/bookmarks")
    public ResponseEntity<BookmarkListResponse> listBookmarks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(name = "entity_type", required = false) String entityType,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);

        EntityType parsedEntityType = null;
        if (entityType != null && !entityType.isBlank()) {
            try {
                parsedEntityType = EntityType.valueOf(entityType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new PostException(HttpStatus.BAD_REQUEST, "validation_error",
                        "Invalid entity_type: " + entityType + ". Valid values: POST, SPOT");
            }
        }

        BookmarkListResponse response = bookmarkService.listBookmarks(userId, page, size, parsedEntityType, requestId);
        return ResponseEntity.ok(response);
    }
}
