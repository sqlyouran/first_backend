package com.mooc.app.controller;

import com.mooc.app.dto.CreateCommentRequest;
import com.mooc.app.dto.response.CommentListResponse;
import com.mooc.app.dto.response.CommentResponse;
import com.mooc.app.entity.EntityType;
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
public class SpotCommentController {

    private final CommentService commentService;
    private final JwtService jwtService;

    public SpotCommentController(CommentService commentService, JwtService jwtService) {
        this.commentService = commentService;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/spots/{spotId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID spotId,
            @Valid @RequestBody CreateCommentRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        CommentResponse response = commentService.createComment(spotId, EntityType.SPOT, userId, request, requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/spots/{spotId}/comments")
    public ResponseEntity<CommentListResponse> listComments(
            @PathVariable UUID spotId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        CommentListResponse response = commentService.listTopLevelComments(spotId, EntityType.SPOT, page, size, requestId);
        return ResponseEntity.ok(response);
    }
}
