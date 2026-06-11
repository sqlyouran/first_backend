package com.mooc.app.controller;

import com.mooc.app.dto.response.BookmarkListResponse;
import com.mooc.app.dto.response.BookmarkResponse;
import com.mooc.app.service.BookmarkService;
import com.mooc.app.service.JwtService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
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
        BookmarkResponse response = bookmarkService.getBookmarkStatus(postId, optionalUserId, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/posts/{postId}/bookmark")
    public ResponseEntity<BookmarkResponse> toggleBookmark(
            @PathVariable UUID postId,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        BookmarkResponse response = bookmarkService.toggleBookmark(postId, userId, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/bookmarks")
    public ResponseEntity<BookmarkListResponse> listBookmarks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        BookmarkListResponse response = bookmarkService.listBookmarks(userId, page, size, requestId);
        return ResponseEntity.ok(response);
    }
}
