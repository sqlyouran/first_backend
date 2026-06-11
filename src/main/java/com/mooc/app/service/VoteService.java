package com.mooc.app.service;

import com.mooc.app.dto.response.VoteResponse;
import com.mooc.app.dto.response.VoteStatsResponse;
import com.mooc.app.entity.VoteEntity;
import com.mooc.app.entity.VoteType;
import com.mooc.app.exception.PostException;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.VoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class VoteService {

    private static final Logger log = LoggerFactory.getLogger(VoteService.class);

    private final VoteRepository voteRepository;
    private final PostRepository postRepository;

    public VoteService(VoteRepository voteRepository, PostRepository postRepository) {
        this.voteRepository = voteRepository;
        this.postRepository = postRepository;
    }

    public VoteResponse vote(UUID postId, UUID userId, String voteTypeStr, String requestId) {
        postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));

        VoteType voteType;
        try {
            voteType = VoteType.valueOf(voteTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PostException(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                    "Invalid vote type: " + voteTypeStr);
        }

        Optional<VoteEntity> existing = voteRepository.findByPostIdAndUserId(postId, userId);

        if (existing.isEmpty()) {
            VoteEntity vote = new VoteEntity();
            vote.setPostId(postId);
            vote.setUserId(userId);
            vote.setVoteType(voteType);
            voteRepository.save(vote);
            return new VoteResponse(requestId, voteType.name().toLowerCase());
        }

        VoteEntity existingVote = existing.get();
        if (existingVote.getVoteType() == voteType) {
            voteRepository.delete(existingVote);
            return new VoteResponse(requestId, null);
        } else {
            existingVote.setVoteType(voteType);
            voteRepository.save(existingVote);
            return new VoteResponse(requestId, voteType.name().toLowerCase());
        }
    }

    public void removeVote(UUID postId, UUID userId) {
        voteRepository.findByPostIdAndUserId(postId, userId)
                .ifPresent(voteRepository::delete);
    }

    public VoteStatsResponse getVoteStats(UUID postId, Optional<UUID> optionalUserId, String requestId) {
        postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));

        long upCount = voteRepository.countByPostIdAndVoteType(postId, VoteType.UP);
        long downCount = voteRepository.countByPostIdAndVoteType(postId, VoteType.DOWN);

        String userVote = null;
        if (optionalUserId.isPresent()) {
            userVote = voteRepository.findByPostIdAndUserId(postId, optionalUserId.get())
                    .map(v -> v.getVoteType().name().toLowerCase())
                    .orElse(null);
        }

        return new VoteStatsResponse(requestId, upCount, downCount, userVote);
    }
}
