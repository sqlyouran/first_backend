package com.mooc.app.service;

import com.mooc.app.dto.response.BookmarkListResponse;
import com.mooc.app.dto.response.BookmarkListResponse.BookmarkItemResponse;
import com.mooc.app.dto.response.BookmarkResponse;
import com.mooc.app.entity.BookmarkEntity;
import com.mooc.app.entity.PostEntity;
import com.mooc.app.exception.PostException;
import com.mooc.app.repository.BookmarkRepository;
import com.mooc.app.repository.PostRepository;
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

    public BookmarkService(BookmarkRepository bookmarkRepository, PostRepository postRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.postRepository = postRepository;
    }

    public BookmarkResponse getBookmarkStatus(UUID postId, Optional<UUID> optionalUserId, String requestId) {
        postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));

        if (optionalUserId.isEmpty()) {
            return new BookmarkResponse(requestId, false);
        }

        boolean bookmarked = bookmarkRepository.findByPostIdAndUserId(postId, optionalUserId.get()).isPresent();
        return new BookmarkResponse(requestId, bookmarked);
    }

    public BookmarkResponse toggleBookmark(UUID postId, UUID userId, String requestId) {
        postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));

        Optional<BookmarkEntity> existing = bookmarkRepository.findByPostIdAndUserId(postId, userId);

        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            return new BookmarkResponse(requestId, false);
        } else {
            BookmarkEntity bookmark = new BookmarkEntity();
            bookmark.setPostId(postId);
            bookmark.setUserId(userId);
            bookmarkRepository.save(bookmark);
            return new BookmarkResponse(requestId, true);
        }
    }

    public BookmarkListResponse listBookmarks(UUID userId, int page, int size, String requestId) {
        Page<BookmarkEntity> result = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page - 1, size));

        List<BookmarkItemResponse> items = result.getContent().stream()
                .map(b -> {
                    String postTitle = postRepository.findByIdAndDeletedFalse(b.getPostId())
                            .map(PostEntity::getTitle)
                            .orElse("[已删除的帖子]");
                    return new BookmarkItemResponse(
                            b.getId().toString(),
                            b.getPostId().toString(),
                            postTitle,
                            b.getCreatedAt().toString()
                    );
                })
                .toList();

        return new BookmarkListResponse(requestId, items, result.getTotalElements(), page, size);
    }
}
