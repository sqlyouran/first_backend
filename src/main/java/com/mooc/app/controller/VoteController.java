package com.mooc.app.controller;

import com.mooc.app.dto.VoteRequest;
import com.mooc.app.dto.response.VoteResponse;
import com.mooc.app.dto.response.VoteStatsResponse;
import com.mooc.app.service.JwtService;
import com.mooc.app.service.VoteService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
public class VoteController {

    private final VoteService voteService;
    private final JwtService jwtService;

    public VoteController(VoteService voteService, JwtService jwtService) {
        this.voteService = voteService;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/posts/{postId}/vote")
    public ResponseEntity<VoteResponse> vote(
            @PathVariable UUID postId,
            @Valid @RequestBody VoteRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        VoteResponse response = voteService.vote(postId, userId, request.voteType(), requestId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/posts/{postId}/vote")
    public ResponseEntity<Void> removeVote(
            @PathVariable UUID postId,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        voteService.removeVote(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/posts/{postId}/vote-stats")
    public ResponseEntity<VoteStatsResponse> getVoteStats(
            @PathVariable UUID postId,
            HttpServletRequest httpRequest) {
        Optional<UUID> optionalUserId = AuthUtil.optionalUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        VoteStatsResponse response = voteService.getVoteStats(postId, optionalUserId, requestId);
        return ResponseEntity.ok(response);
    }
}
