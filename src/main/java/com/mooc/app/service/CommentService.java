package com.mooc.app.service;

import com.mooc.app.dto.CreateCommentRequest;
import com.mooc.app.dto.response.CommentListResponse;
import com.mooc.app.dto.response.CommentResponse;
import com.mooc.app.entity.*;
import com.mooc.app.exception.PostException;
import com.mooc.app.repository.CommentRepository;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.SpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final SpotRepository spotRepository;
    private final NotificationService notificationService;
    private final GenericCacheService cacheService;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository,
                           SpotRepository spotRepository, NotificationService notificationService,
                           GenericCacheService cacheService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.spotRepository = spotRepository;
        this.notificationService = notificationService;
        this.cacheService = cacheService;
    }

    @Transactional
    public CommentResponse createComment(UUID entityId, EntityType entityType, UUID userId, CreateCommentRequest request, String requestId) {
        validateEntityExists(entityId, entityType);

        CommentEntity parentComment = null;
        if (request.parentCommentId() != null) {
            parentComment = commentRepository.findByIdAndDeletedFalse(request.parentCommentId())
                    .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Parent comment not found"));
        }

        CommentEntity comment = new CommentEntity();
        comment.setEntityId(entityId);
        comment.setEntityType(entityType);
        comment.setUserId(userId);
        comment.setContent(request.content());
        comment.setParentCommentId(request.parentCommentId());

        CommentEntity saved = commentRepository.save(comment);

        // Maintain denormalized counter for posts
        if (entityType == EntityType.POST) {
            postRepository.incrementCommentCount(entityId, 1);
        }

        if (entityType == EntityType.POST) {
            if (parentComment != null) {
                // Reply: notify parent comment author
                String preview = truncate(request.content(), 50);
                notificationService.createNotification(
                        parentComment.getUserId(), userId,
                        NotificationType.COMMENT_REPLIED, entityId, "post", preview);
            } else {
                // Top-level comment: notify post author
                postRepository.findByIdAndDeletedFalse(entityId).ifPresent(post -> {
                    String preview = truncate(request.content(), 50);
                    notificationService.createNotification(
                            post.getAuthorId(), userId,
                            NotificationType.POST_COMMENTED, entityId, "post", preview);
                });
            }
        }

        if (entityType == EntityType.POST) {
            cacheService.evict("cache:posts:*");
        }

        return toCommentResponse(saved, requestId);
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen) : text;
    }

    public CommentListResponse listTopLevelComments(UUID entityId, EntityType entityType, int page, int size, String requestId) {
        validateEntityExists(entityId, entityType);

        Page<CommentEntity> result = commentRepository.findTopLevelByEntity(entityId, entityType, PageRequest.of(page - 1, size));
        List<CommentResponse> items = result.getContent().stream()
                .map(c -> toCommentResponse(c, requestId))
                .toList();
        return new CommentListResponse(requestId, items, result.getTotalElements(), page, size);
    }

    public CommentListResponse listReplies(UUID commentId, int page, int size, String requestId) {
        Page<CommentEntity> result = commentRepository.findRepliesByParentId(commentId, PageRequest.of(page - 1, size));
        List<CommentResponse> items = result.getContent().stream()
                .map(c -> toCommentResponse(c, requestId))
                .toList();
        return new CommentListResponse(requestId, items, result.getTotalElements(), page, size);
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        CommentEntity comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new PostException(HttpStatus.FORBIDDEN, "access_denied", "You are not the author of this comment");
        }

        comment.markDeleted();
        commentRepository.save(comment);

        // Maintain denormalized counter for posts
        if (comment.getEntityType() == EntityType.POST) {
            postRepository.incrementCommentCount(comment.getEntityId(), -1);
            cacheService.evict("cache:posts:*");
        }
    }

    private void validateEntityExists(UUID entityId, EntityType entityType) {
        switch (entityType) {
            case POST -> postRepository.findByIdAndDeletedFalse(entityId)
                    .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));
            case SPOT -> spotRepository.findByIdAndDeletedFalse(entityId)
                    .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Spot not found"));
        }
    }

    private CommentResponse toCommentResponse(CommentEntity comment, String requestId) {
        String content = comment.isDeleted() ? "[已删除]" : comment.getContent();
        return new CommentResponse(
                requestId,
                comment.getId().toString(),
                comment.getEntityId().toString(),
                comment.getEntityType().name().toLowerCase(),
                comment.getUserId().toString(),
                content,
                comment.getParentCommentId() != null ? comment.getParentCommentId().toString() : null,
                comment.getCreatedAt().toString(),
                comment.isDeleted()
        );
    }
}
