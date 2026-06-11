package com.mooc.app.service;

import com.mooc.app.dto.CreateCommentRequest;
import com.mooc.app.dto.response.CommentListResponse;
import com.mooc.app.dto.response.CommentResponse;
import com.mooc.app.entity.CommentEntity;
import com.mooc.app.exception.PostException;
import com.mooc.app.repository.CommentRepository;
import com.mooc.app.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    public CommentResponse createComment(UUID postId, UUID userId, CreateCommentRequest request, String requestId) {
        postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));

        if (request.parentCommentId() != null) {
            commentRepository.findByIdAndDeletedFalse(request.parentCommentId())
                    .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Parent comment not found"));
        }

        CommentEntity comment = new CommentEntity();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(request.content());
        comment.setParentCommentId(request.parentCommentId());

        CommentEntity saved = commentRepository.save(comment);
        return toCommentResponse(saved, requestId);
    }

    public CommentListResponse listTopLevelComments(UUID postId, int page, int size, String requestId) {
        postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));

        Page<CommentEntity> result = commentRepository.findTopLevelByPostId(postId, PageRequest.of(page - 1, size));
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

    public void deleteComment(UUID commentId, UUID userId) {
        CommentEntity comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new PostException(HttpStatus.FORBIDDEN, "access_denied", "You are not the author of this comment");
        }

        comment.markDeleted();
        commentRepository.save(comment);
    }

    private CommentResponse toCommentResponse(CommentEntity comment, String requestId) {
        String content = comment.isDeleted() ? "[已删除]" : comment.getContent();
        return new CommentResponse(
                requestId,
                comment.getId().toString(),
                comment.getPostId().toString(),
                comment.getUserId().toString(),
                content,
                comment.getParentCommentId() != null ? comment.getParentCommentId().toString() : null,
                comment.getCreatedAt().toString(),
                comment.isDeleted()
        );
    }
}
