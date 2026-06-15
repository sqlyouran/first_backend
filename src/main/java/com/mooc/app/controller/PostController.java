package com.mooc.app.controller;

import com.mooc.app.dto.CreatePostRequest;
import com.mooc.app.dto.UpdatePostRequest;
import com.mooc.app.dto.response.PostListResponse;
import com.mooc.app.dto.response.PostResponse;
import com.mooc.app.dto.response.PostSpotsResponse;
import com.mooc.app.entity.PostSortBy;
import com.mooc.app.exception.PostException;
import com.mooc.app.service.JwtService;
import com.mooc.app.service.PostService;
import com.mooc.app.service.SpotPostService;
import com.mooc.app.util.AuthUtil;
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
    private final SpotPostService spotPostService;

    public PostController(PostService postService, JwtService jwtService, SpotPostService spotPostService) {
        this.postService = postService;
        this.jwtService = jwtService;
        this.spotPostService = spotPostService;
    }

    @PostMapping("/api/posts")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            HttpServletRequest httpRequest) {
        UUID authorId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        PostResponse response = postService.createPost(request, authorId, requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/posts/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request,
            HttpServletRequest httpRequest) {
        UUID authorId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        PostResponse response = postService.updatePost(id, request, authorId, requestId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/posts/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID authorId = AuthUtil.requireUserId(httpRequest, jwtService);
        postService.deletePost(id, authorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/posts/{idOrSlug}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable String idOrSlug,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        PostResponse response = postService.getPost(idOrSlug, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/posts")
    public ResponseEntity<PostListResponse> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "latest") String sort,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        PostSortBy sortBy = parseSortBy(sort);
        PostListResponse response = postService.listPosts(page, size, cursor, sortBy, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/users/{userId}/posts")
    public ResponseEntity<PostListResponse> listUserPosts(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "latest") String sort,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        PostSortBy sortBy = parseSortBy(sort);
        PostListResponse response = postService.listUserPosts(userId, page, size, cursor, sortBy, requestId);
        return ResponseEntity.ok(response);
    }

    private PostSortBy parseSortBy(String sort) {
        try {
            return PostSortBy.fromString(sort);
        } catch (IllegalArgumentException e) {
            throw new PostException(HttpStatus.BAD_REQUEST, "validation_error", "Invalid sort value: " + sort);
        }
    }

    @GetMapping("/api/posts/{id}/spots")
    public ResponseEntity<PostSpotsResponse> getPostSpots(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        PostSpotsResponse response = spotPostService.getSpotsByPostId(id, requestId);
        return ResponseEntity.ok(response);
    }


}
