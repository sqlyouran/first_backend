package com.mooc.app.service;

import com.mooc.app.dto.response.PostSpotsResponse;
import com.mooc.app.dto.response.SpotPostsResponse;
import com.mooc.app.entity.*;
import com.mooc.app.exception.PostException;
import com.mooc.app.exception.SpotException;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.SpotPostRepository;
import com.mooc.app.repository.SpotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotPostServiceTest {

    @Mock
    private SpotPostRepository spotPostRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private SpotRepository spotRepository;

    @InjectMocks
    private SpotPostService spotPostService;

    private static final UUID SPOT_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();

    // --- getPostsBySpotId ---

    @Test
    void getPostsBySpotId_returnsPaginatedAssociatedPosts() {
        SpotEntity spot = createSpot(SPOT_ID, "Spot");
        when(spotRepository.findByIdAndDeletedFalse(SPOT_ID)).thenReturn(Optional.of(spot));

        PostEntity post = createPost(POST_ID, "Post Title");
        SpotPostEntity assoc = createAssociation(SPOT_ID, POST_ID);
        when(spotPostRepository.findBySpotIdAndDeletedFalse(eq(SPOT_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(assoc)));
        when(postRepository.findAllByIdInAndStatusAndDeletedFalse(List.of(POST_ID), PostStatus.PUBLISHED))
                .thenReturn(List.of(post));

        SpotPostsResponse response = spotPostService.getPostsBySpotId(SPOT_ID, 1, 20, "req-id");

        assertEquals(1, response.getTotal());
        assertEquals(1, response.getItems().size());
        assertEquals("Post Title", response.getItems().get(0).getTitle());
        assertEquals(1, response.getPage());
        assertEquals(20, response.getSize());
    }

    @Test
    void getPostsBySpotId_filtersOutDraftPosts() {
        SpotEntity spot = createSpot(SPOT_ID, "Spot");
        when(spotRepository.findByIdAndDeletedFalse(SPOT_ID)).thenReturn(Optional.of(spot));

        UUID draftPostId = UUID.randomUUID();
        SpotPostEntity assoc1 = createAssociation(SPOT_ID, POST_ID);
        SpotPostEntity assoc2 = createAssociation(SPOT_ID, draftPostId);

        when(spotPostRepository.findBySpotIdAndDeletedFalse(eq(SPOT_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(assoc1, assoc2)));

        PostEntity publishedPost = createPost(POST_ID, "Published");
        when(postRepository.findAllByIdInAndStatusAndDeletedFalse(eq(List.of(POST_ID, draftPostId)), eq(PostStatus.PUBLISHED)))
                .thenReturn(List.of(publishedPost));

        SpotPostsResponse response = spotPostService.getPostsBySpotId(SPOT_ID, 1, 20, "req-id");

        assertEquals(1, response.getItems().size());
    }

    @Test
    void getPostsBySpotId_spotNotFound_throwsSpotException() {
        UUID unknownSpot = UUID.randomUUID();
        when(spotRepository.findByIdAndDeletedFalse(unknownSpot)).thenReturn(Optional.empty());

        SpotException ex = assertThrows(SpotException.class,
                () -> spotPostService.getPostsBySpotId(unknownSpot, 1, 20, "req-id"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("not_found", ex.getErrorCode());
    }

    @Test
    void getPostsBySpotId_emptyAssociations_returnsEmptyList() {
        SpotEntity spot = createSpot(SPOT_ID, "Spot");
        when(spotRepository.findByIdAndDeletedFalse(SPOT_ID)).thenReturn(Optional.of(spot));
        when(spotPostRepository.findBySpotIdAndDeletedFalse(eq(SPOT_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        SpotPostsResponse response = spotPostService.getPostsBySpotId(SPOT_ID, 1, 20, "req-id");

        assertEquals(0, response.getTotal());
        assertTrue(response.getItems().isEmpty());
    }

    // --- getSpotsByPostId ---

    @Test
    void getSpotsByPostId_returnsAssociatedSpots() {
        PostEntity post = createPost(POST_ID, "Post");
        when(postRepository.findByIdAndDeletedFalse(POST_ID)).thenReturn(Optional.of(post));

        SpotEntity spot = createSpot(SPOT_ID, "Spot Name");
        SpotPostEntity assoc = createAssociation(SPOT_ID, POST_ID);
        when(spotPostRepository.findByPostIdAndDeletedFalse(POST_ID)).thenReturn(List.of(assoc));
        when(spotRepository.findAllByIdInAndStatusAndDeletedFalse(List.of(SPOT_ID), SpotStatus.PUBLISHED))
                .thenReturn(List.of(spot));

        PostSpotsResponse response = spotPostService.getSpotsByPostId(POST_ID, "req-id");

        assertEquals(1, response.getItems().size());
        assertEquals("Spot Name", response.getItems().get(0).getName());
    }

    @Test
    void getSpotsByPostId_postNotFound_throwsPostException() {
        UUID unknownPost = UUID.randomUUID();
        when(postRepository.findByIdAndDeletedFalse(unknownPost)).thenReturn(Optional.empty());

        PostException ex = assertThrows(PostException.class,
                () -> spotPostService.getSpotsByPostId(unknownPost, "req-id"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("not_found", ex.getErrorCode());
    }

    @Test
    void getSpotsByPostId_emptyAssociations_returnsEmptyList() {
        PostEntity post = createPost(POST_ID, "Post");
        when(postRepository.findByIdAndDeletedFalse(POST_ID)).thenReturn(Optional.of(post));
        when(spotPostRepository.findByPostIdAndDeletedFalse(POST_ID)).thenReturn(List.of());

        PostSpotsResponse response = spotPostService.getSpotsByPostId(POST_ID, "req-id");

        assertTrue(response.getItems().isEmpty());
    }

    // --- helpers ---

    private SpotEntity createSpot(UUID id, String name) {
        SpotEntity spot = new SpotEntity();
        spot.setId(id);
        spot.setName(name);
        spot.setSlug("spot-" + id);
        spot.setCityId(UUID.randomUUID());
        spot.setCityName("City");
        spot.setStatus(SpotStatus.PUBLISHED);
        spot.setRating(new BigDecimal("4.0"));
        spot.setViewCount(100);
        spot.setBookmarkCount(50);
        return spot;
    }

    private PostEntity createPost(UUID id, String title) {
        PostEntity post = new PostEntity();
        post.setId(id);
        post.setTitle(title);
        post.setSlug(title.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + id.toString().substring(0, 8));
        post.setContent("Content");
        post.setAuthorId(UUID.randomUUID());
        post.setStatus(PostStatus.PUBLISHED);
        return post;
    }

    private SpotPostEntity createAssociation(UUID spotId, UUID postId) {
        SpotPostEntity sp = new SpotPostEntity();
        sp.setSpotId(spotId);
        sp.setPostId(postId);
        return sp;
    }
}
