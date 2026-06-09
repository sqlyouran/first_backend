package com.mooc.app.controller;

import com.mooc.app.dto.CreatePostRequest;
import com.mooc.app.dto.UpdatePostRequest;
import com.mooc.app.dto.response.PostListResponse;
import com.mooc.app.dto.response.PostResponse;
import com.mooc.app.exception.PostException;
import com.mooc.app.filter.RequestIdFilter;
import com.mooc.app.service.JwtService;
import com.mooc.app.service.PostService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class PostController {

    private final PostService postService;
    private final JwtService jwtService;

    public PostController(PostService postService, JwtService jwtService) {
        this.postService = postService;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/posts")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            HttpServletRequest httpRequest) {
        UUID authorId = requireUserId(httpRequest);
        String requestId = getRequestId(httpRequest);
        PostResponse response = postService.createPost(request, authorId, requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/posts/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request,
            HttpServletRequest httpRequest) {
        UUID authorId = requireUserId(httpRequest);
        String requestId = getRequestId(httpRequest);
        PostResponse response = postService.updatePost(id, request, authorId, requestId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/posts/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID authorId = requireUserId(httpRequest);
        postService.deletePost(id, authorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/posts/{id}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        String requestId = getRequestId(httpRequest);
        PostResponse response = postService.getPost(id, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/posts")
    public ResponseEntity<PostListResponse> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String requestId = getRequestId(httpRequest);
        PostListResponse response = postService.listPosts(page, size, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/users/{userId}/posts")
    public ResponseEntity<PostListResponse> listUserPosts(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String requestId = getRequestId(httpRequest);
        PostListResponse response = postService.listUserPosts(userId, page, size, requestId);
        return ResponseEntity.ok(response);
    }

    private UUID requireUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new PostException(HttpStatus.UNAUTHORIZED, "unauthorized",
                    "Missing or invalid authorization header");
        }
        String token = authHeader.substring(7);
        return jwtService.parseToken(token)
                .map(Claims::getSubject)
                .map(UUID::fromString)
                .orElseThrow(() -> new PostException(HttpStatus.UNAUTHORIZED, "unauthorized",
                        "Invalid or expired token"));
    }

    private String getRequestId(HttpServletRequest request) {
        Object id = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        return id != null ? id.toString() : "unknown";
    }
}
