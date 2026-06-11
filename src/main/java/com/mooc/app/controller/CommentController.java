package com.mooc.app.controller;

import com.mooc.app.dto.CreateCommentRequest;
import com.mooc.app.dto.response.CommentListResponse;
import com.mooc.app.dto.response.CommentResponse;
import com.mooc.app.service.CommentService;
import com.mooc.app.service.JwtService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class CommentController {

    private final CommentService commentService;
    private final JwtService jwtService;

    public CommentController(CommentService commentService, JwtService jwtService) {
        this.commentService = commentService;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        CommentResponse response = commentService.createComment(postId, userId, request, requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/posts/{postId}/comments")
    public ResponseEntity<CommentListResponse> listComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        CommentListResponse response = commentService.listTopLevelComments(postId, page, size, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/comments/{commentId}/replies")
    public ResponseEntity<CommentListResponse> listReplies(
            @PathVariable UUID commentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        CommentListResponse response = commentService.listReplies(commentId, page, size, requestId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/posts/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
