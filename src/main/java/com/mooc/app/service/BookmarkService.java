package com.mooc.app.service;

import com.mooc.app.dto.response.BookmarkListResponse;
import com.mooc.app.dto.response.BookmarkListResponse.BookmarkItemResponse;
import com.mooc.app.dto.response.BookmarkResponse;
import com.mooc.app.entity.*;
import com.mooc.app.exception.PostException;
import com.mooc.app.repository.BookmarkRepository;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.SpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookmarkService {

    private static final Logger log = LoggerFactory.getLogger(BookmarkService.class);

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final SpotRepository spotRepository;
    private final NotificationService notificationService;

    public BookmarkService(BookmarkRepository bookmarkRepository, PostRepository postRepository,
                            SpotRepository spotRepository, NotificationService notificationService) {
        this.bookmarkRepository = bookmarkRepository;
        this.postRepository = postRepository;
        this.spotRepository = spotRepository;
        this.notificationService = notificationService;
    }

    public BookmarkResponse getBookmarkStatus(UUID entityId, EntityType entityType, Optional<UUID> optionalUserId, String requestId) {
        validateEntityExists(entityId, entityType);

        if (optionalUserId.isEmpty()) {
            return new BookmarkResponse(requestId, false);
        }

        boolean bookmarked = bookmarkRepository.findByEntityIdAndEntityTypeAndUserId(entityId, entityType, optionalUserId.get()).isPresent();
        return new BookmarkResponse(requestId, bookmarked);
    }

    public BookmarkResponse toggleBookmark(UUID entityId, EntityType entityType, UUID userId, String requestId) {
        validateEntityExists(entityId, entityType);

        Optional<BookmarkEntity> existing = bookmarkRepository.findByEntityIdAndEntityTypeAndUserId(entityId, entityType, userId);

        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            if (entityType == EntityType.POST) {
                postRepository.findByIdAndDeletedFalse(entityId).ifPresent(post ->
                        notificationService.deleteNotification(post.getAuthorId(), userId,
                                NotificationType.POST_BOOKMARKED, entityId));
            }
            return new BookmarkResponse(requestId, false);
        } else {
            BookmarkEntity bookmark = new BookmarkEntity();
            bookmark.setEntityId(entityId);
            bookmark.setEntityType(entityType);
            bookmark.setUserId(userId);
            bookmarkRepository.save(bookmark);
            if (entityType == EntityType.POST) {
                postRepository.findByIdAndDeletedFalse(entityId).ifPresent(post ->
                        notificationService.createNotification(post.getAuthorId(), userId,
                                NotificationType.POST_BOOKMARKED, entityId, "post", null));
            }
            return new BookmarkResponse(requestId, true);
        }
    }

    public BookmarkListResponse listBookmarks(UUID userId, int page, int size, EntityType entityType, String requestId) {
        Page<BookmarkEntity> result;
        if (entityType != null) {
            result = bookmarkRepository.findByUserIdAndEntityTypeOrderByCreatedAtDesc(userId, entityType, PageRequest.of(page - 1, size));
        } else {
            result = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page - 1, size));
        }

        List<BookmarkItemResponse> items = result.getContent().stream()
                .map(b -> {
                    String title = resolveEntityTitle(b.getEntityId(), b.getEntityType());
                    return new BookmarkItemResponse(
                            b.getId().toString(),
                            b.getEntityId().toString(),
                            b.getEntityType().name().toLowerCase(),
                            title,
                            b.getCreatedAt().toString()
                    );
                })
                .toList();

        return new BookmarkListResponse(requestId, items, result.getTotalElements(), page, size);
    }

    private void validateEntityExists(UUID entityId, EntityType entityType) {
        switch (entityType) {
            case POST -> postRepository.findByIdAndDeletedFalse(entityId)
                    .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));
            case SPOT -> spotRepository.findByIdAndDeletedFalse(entityId)
                    .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Spot not found"));
        }
    }

    private String resolveEntityTitle(UUID entityId, EntityType entityType) {
        return switch (entityType) {
            case POST -> postRepository.findByIdAndDeletedFalse(entityId)
                    .map(PostEntity::getTitle)
                    .orElse("[已删除的帖子]");
            case SPOT -> spotRepository.findByIdAndDeletedFalse(entityId)
                    .map(SpotEntity::getName)
                    .orElse("[已删除的景点]");
        };
    }
}
