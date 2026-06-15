package com.mooc.app.service;

import com.mooc.app.dto.response.PostResponse;
import com.mooc.app.dto.response.PostSpotsResponse;
import com.mooc.app.dto.response.SpotPostsResponse;
import com.mooc.app.dto.response.SpotResponse;
import com.mooc.app.entity.*;
import com.mooc.app.exception.PostException;
import com.mooc.app.exception.SpotException;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.SpotPostRepository;
import com.mooc.app.repository.SpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SpotPostService {

    private static final Logger log = LoggerFactory.getLogger(SpotPostService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final SpotPostRepository spotPostRepository;
    private final PostRepository postRepository;
    private final SpotRepository spotRepository;

    public SpotPostService(SpotPostRepository spotPostRepository,
                           PostRepository postRepository,
                           SpotRepository spotRepository) {
        this.spotPostRepository = spotPostRepository;
        this.postRepository = postRepository;
        this.spotRepository = spotRepository;
    }

    public SpotPostsResponse getPostsBySpotId(UUID spotId, int page, int size, String requestId) {
        if (size > MAX_PAGE_SIZE) {
            throw new SpotException(HttpStatus.BAD_REQUEST, "validation_error",
                    "Page size must not exceed " + MAX_PAGE_SIZE);
        }

        spotRepository.findByIdAndDeletedFalse(spotId)
                .orElseThrow(() -> new SpotException(HttpStatus.NOT_FOUND, "not_found", "Spot not found"));

        int zeroBasedPage = Math.max(0, page - 1);
        Page<SpotPostEntity> assocPage = spotPostRepository.findBySpotIdAndDeletedFalse(
                spotId, PageRequest.of(zeroBasedPage, size));

        List<UUID> postIds = assocPage.getContent().stream()
                .map(SpotPostEntity::getPostId)
                .toList();

        List<PostResponse> items = List.of();
        if (!postIds.isEmpty()) {
            List<PostEntity> posts = postRepository.findAllByIdInAndStatusAndDeletedFalse(
                    postIds, PostStatus.PUBLISHED);
            items = posts.stream()
                    .map(post -> toPostResponse(post, requestId))
                    .toList();
        }

        return new SpotPostsResponse(requestId, items, assocPage.getTotalElements(), page, size);
    }

    public PostSpotsResponse getSpotsByPostId(UUID postId, String requestId) {
        postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(HttpStatus.NOT_FOUND, "not_found", "Post not found"));

        List<SpotPostEntity> associations = spotPostRepository.findByPostIdAndDeletedFalse(postId);

        List<SpotResponse> items = List.of();
        if (!associations.isEmpty()) {
            List<UUID> spotIds = associations.stream()
                    .map(SpotPostEntity::getSpotId)
                    .toList();
            List<SpotEntity> spots = spotRepository.findAllByIdInAndStatusAndDeletedFalse(
                    spotIds, SpotStatus.PUBLISHED);
            items = spots.stream()
                    .map(spot -> toSpotResponse(spot, requestId))
                    .toList();
        }

        return new PostSpotsResponse(requestId, items);
    }

    private PostResponse toPostResponse(PostEntity post, String requestId) {
        return new PostResponse(
                requestId,
                post.getId().toString(),
                post.getTitle(),
                post.getSlug(),
                post.getContent(),
                post.getCoverImage(),
                post.getTags(),
                post.getStatus().name().toLowerCase(),
                post.getAuthorId().toString(),
                post.getCreatedAt() != null ? post.getCreatedAt().toString() : null,
                post.getUpdatedAt() != null ? post.getUpdatedAt().toString() : null,
                0, 0, 0
        );
    }

    private SpotResponse toSpotResponse(SpotEntity spot, String requestId) {
        return new SpotResponse(
                requestId,
                spot.getId().toString(),
                spot.getName(),
                spot.getNameZh(),
                spot.getSlug(),
                spot.getDescription(),
                spot.getDescriptionZh(),
                spot.getCoverImage(),
                spot.getGallery(),
                spot.getTags(),
                spot.getCityId() != null ? spot.getCityId().toString() : null,
                spot.getCityName(),
                spot.getStatus() != null ? spot.getStatus().name().toLowerCase() : null,
                spot.getRating() != null ? spot.getRating().toPlainString() : "0.0",
                spot.getViewCount(),
                spot.getBookmarkCount(),
                spot.getCreatedAt() != null ? spot.getCreatedAt().toString() : null,
                spot.getUpdatedAt() != null ? spot.getUpdatedAt().toString() : null
        );
    }
}
