package com.mooc.app.service;

import com.mooc.app.dto.CreatePostRequest;
import com.mooc.app.dto.UpdatePostRequest;
import com.mooc.app.dto.response.PostListResponse;
import com.mooc.app.dto.response.PostResponse;
import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.PostStatus;
import com.mooc.app.exception.PostException;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public PostResponse createPost(CreatePostRequest request, UUID authorId, String requestId) {
        if (!userRepository.existsById(authorId)) {
            throw new PostException(HttpStatus.NOT_FOUND, "not_found", "User not found");
        }

        PostEntity post = new PostEntity();
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setCoverImage(request.coverImage());
        post.setTags(request.tags() != null ? new ArrayList<>(request.tags()) : new ArrayList<>());
        post.setAuthorId(authorId);

        if (request.status() != null && !request.status().isBlank()) {
            try {
                post.setStatus(PostStatus.valueOf(request.status().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new PostException(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                        "Invalid status: " + request.status());
            }
        }

        PostEntity saved = postRepository.save(post);
        return toPostResponse(saved, requestId, true);
    }

    public PostResponse updatePost(UUID postId, UpdatePostRequest request, UUID authorId, String requestId) {
        PostEntity post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));

        if (!post.getAuthorId().equals(authorId)) {
            throw new PostException(HttpStatus.FORBIDDEN, "access_denied", "You are not the author of this post");
        }

        if (request.title() != null) {
            post.setTitle(request.title());
        }
        if (request.content() != null) {
            post.setContent(request.content());
        }
        if (request.coverImage() != null) {
            post.setCoverImage(request.coverImage());
        }
        if (request.tags() != null) {
            post.setTags(new ArrayList<>(request.tags()));
        }
        if (request.status() != null && !request.status().isBlank()) {
            try {
                post.setStatus(PostStatus.valueOf(request.status().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new PostException(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                        "Invalid status: " + request.status());
            }
        }

        PostEntity saved = postRepository.save(post);
        return toPostResponse(saved, requestId, true);
    }

    public void deletePost(UUID postId, UUID authorId) {
        PostEntity post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));

        if (!post.getAuthorId().equals(authorId)) {
            throw new PostException(HttpStatus.FORBIDDEN, "access_denied", "You are not the author of this post");
        }

        post.markDeleted();
        postRepository.save(post);
    }

    public PostResponse getPost(UUID postId, String requestId) {
        PostEntity post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found");
        }

        return toPostResponse(post, requestId, true);
    }

    public PostListResponse listPosts(int page, int size, String requestId) {
        int safePage = Math.max(page - 1, 0);
        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PostEntity> result = postRepository.findByStatusAndDeletedFalse(PostStatus.PUBLISHED, pageable);
        List<PostResponse> items = result.getContent().stream()
                .map(p -> toPostResponse(p, requestId, false))
                .toList();

        return new PostListResponse(requestId, items, result.getTotalElements(), page, safeSize);
    }

    public PostListResponse listUserPosts(UUID authorId, int page, int size, String requestId) {
        int safePage = Math.max(page - 1, 0);
        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PostEntity> result = postRepository.findByAuthorIdAndStatusAndDeletedFalse(
                authorId, PostStatus.PUBLISHED, pageable);
        List<PostResponse> items = result.getContent().stream()
                .map(p -> toPostResponse(p, requestId, false))
                .toList();

        return new PostListResponse(requestId, items, result.getTotalElements(), page, safeSize);
    }

    private PostResponse toPostResponse(PostEntity post, String requestId, boolean includeContent) {
        return new PostResponse(
                requestId,
                post.getId().toString(),
                post.getTitle(),
                includeContent ? post.getContent() : null,
                post.getCoverImage(),
                post.getTags(),
                post.getStatus().name().toLowerCase(),
                post.getAuthorId().toString(),
                post.getCreatedAt().toString(),
                post.getUpdatedAt().toString()
        );
    }
}
