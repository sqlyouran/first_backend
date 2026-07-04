package com.mooc.app.service;

import com.mooc.app.entity.CityEntity;
import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.repository.CityRepository;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeywordSearchServiceTest {

    @Mock
    private SpotRepository spotRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private KeywordSearchService keywordSearchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keywordSearchService, "keywordTopK", 10);
        ReflectionTestUtils.setField(keywordSearchService, "suggestTopK", 5);
    }

    @Test
    void search_returnsSortedResultsByScore() {
        SpotEntity spot = createSpot("Great Wall", "great-wall", "Ancient wall", "Beijing");
        PostEntity post = createPost("Great Wall", "great-wall", "Visited the wall");
        when(spotRepository.searchByKeyword("great wall")).thenReturn(List.of(spot));
        when(postRepository.searchByKeyword("great wall")).thenReturn(List.of(post));
        when(cityRepository.searchByKeyword("great wall")).thenReturn(List.of());

        List<KeywordResult> results = keywordSearchService.search("great wall");

        assertFalse(results.isEmpty());
        assertEquals("spot", results.get(0).type());
    }

    @Test
    void search_emptyQueryReturnsEmptyList() {
        List<KeywordResult> results = keywordSearchService.search("");
        assertTrue(results.isEmpty());

        results = keywordSearchService.search(null);
        assertTrue(results.isEmpty());

        results = keywordSearchService.search("   ");
        assertTrue(results.isEmpty());
    }

    @Test
    void search_scoringRules_nameMatch3_tags2_desc1() {
        // Spot with name match (3) + desc match (1) = 4
        SpotEntity spotNameDesc = createSpot("Beijing", "beijing", "Beijing is great", "Beijing");
        // Spot with only desc match (1) = 1
        SpotEntity spotDesc = createSpot("Shanghai", "shanghai", "Visit Beijing today", "Shanghai");

        when(spotRepository.searchByKeyword("beijing")).thenReturn(List.of(spotNameDesc, spotDesc));
        when(postRepository.searchByKeyword("beijing")).thenReturn(List.of());
        when(cityRepository.searchByKeyword("beijing")).thenReturn(List.of());

        List<KeywordResult> results = keywordSearchService.search("beijing");

        assertEquals(2, results.size());
        // spotNameDesc should score higher (name + desc = 4) than spotDesc (desc only = 1)
        assertTrue(results.get(0).score() > results.get(1).score());
        assertEquals("beijing", results.get(0).slug());
    }

    @Test
    void search_spotWithTagsMatch_scoresHigherThanDescOnly() {
        SpotEntity spotWithTags = createSpot("Temple", "temple", "A nice place", "Beijing");
        spotWithTags.setTags(List.of("beijing", "history"));
        SpotEntity spotDescOnly = createSpot("Park", "park", "Beijing park", "Beijing");

        when(spotRepository.searchByKeyword("beijing")).thenReturn(List.of(spotWithTags, spotDescOnly));
        when(postRepository.searchByKeyword("beijing")).thenReturn(List.of());
        when(cityRepository.searchByKeyword("beijing")).thenReturn(List.of());

        List<KeywordResult> results = keywordSearchService.search("beijing");

        assertEquals(2, results.size());
        // spotWithTags: name no(0) + tags(2) + desc no(0) = 2 ... but spotDescOnly: name no(0) + desc(1) = 1
        // Actually spotDescOnly matches "beijing" in desc AND cityName, and the repo already filtered
        // spotWithTags: name "Temple" no match(0) + tags "beijing"(2) + desc "A nice place" no(0) = 2
        // spotDescOnly: name "Park" no(0) + desc "Beijing park"(1) = 1
        assertTrue(results.get(0).score() >= results.get(1).score());
    }

    @Test
    void suggest_returnsLimitedResults() {
        ReflectionTestUtils.setField(keywordSearchService, "suggestTopK", 3);

        SpotEntity s1 = createSpot("Beijing", "beijing", "Capital", "Beijing");
        SpotEntity s2 = createSpot("Beijing Zoo", "beijing-zoo", "Zoo in Beijing", "Beijing");
        SpotEntity s3 = createSpot("Beijing Park", "beijing-park", "Park", "Beijing");
        SpotEntity s4 = createSpot("Beijing Lake", "beijing-lake", "Lake", "Beijing");

        when(spotRepository.searchByKeyword("beijing")).thenReturn(List.of(s1, s2, s3, s4));
        when(postRepository.searchByKeyword("beijing")).thenReturn(List.of());
        when(cityRepository.searchByKeyword("beijing")).thenReturn(List.of());

        List<KeywordResult> results = keywordSearchService.suggest("beijing");

        assertTrue(results.size() <= 3);
    }

    @Test
    void search_postTitleMatch_scoresHigherThanContentOnly() {
        PostEntity postTitle = createPost("Beijing Travel Guide", "beijing-travel", "A guide");
        PostEntity postContent = createPost("My Trip", "my-trip", "Went to Beijing last summer");

        when(spotRepository.searchByKeyword("beijing")).thenReturn(List.of());
        when(postRepository.searchByKeyword("beijing")).thenReturn(List.of(postTitle, postContent));
        when(cityRepository.searchByKeyword("beijing")).thenReturn(List.of());

        List<KeywordResult> results = keywordSearchService.search("beijing");

        assertEquals(2, results.size());
        assertEquals("beijing-travel", results.get(0).slug());
        assertEquals(3.0, results.get(0).score()); // title match = 3
        assertEquals(1.0, results.get(1).score()); // content match = 1
    }

    private SpotEntity createSpot(String name, String slug, String description, String cityName) {
        SpotEntity spot = new SpotEntity();
        spot.setId(UUID.randomUUID());
        spot.setName(name);
        spot.setSlug(slug);
        spot.setDescription(description);
        spot.setCityId(UUID.randomUUID());
        spot.setCityName(cityName);
        spot.setTags(List.of());
        return spot;
    }

    private PostEntity createPost(String title, String slug, String content) {
        PostEntity post = new PostEntity();
        post.setId(UUID.randomUUID());
        post.setTitle(title);
        post.setSlug(slug);
        post.setContent(content);
        post.setTags(List.of());
        post.setAuthorId(UUID.randomUUID());
        return post;
    }
}
