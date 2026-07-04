package com.mooc.app.service;

import com.mooc.app.dto.response.SearchResponse;
import com.mooc.app.dto.response.SearchResultItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @Mock
    private KnowledgeSearchService knowledgeSearchService;
    @Mock
    private KeywordSearchService keywordSearchService;

    private HybridSearchService hybridSearchService;

    @BeforeEach
    void setUp() {
        hybridSearchService = new HybridSearchService(knowledgeSearchService, keywordSearchService);
        ReflectionTestUtils.setField(hybridSearchService, "rrfK", 60);
        ReflectionTestUtils.setField(hybridSearchService, "vectorTopK", 10);
    }

    @Test
    void search_callsBothServicesAndMergesByRRF() {
        UUID spotId = UUID.randomUUID();
        // Vector returns spot A at rank 1
        Document vecDoc = createDoc("spot", "great-wall", "Great Wall", null);
        when(knowledgeSearchService.search("great wall", null)).thenReturn(List.of(vecDoc));

        // Keyword returns spot A at rank 1
        KeywordResult kwResult = new KeywordResult(spotId, "Great Wall", "great-wall", "spot", 3.0);
        when(keywordSearchService.search("great wall")).thenReturn(List.of(kwResult));

        // The vector doc needs entity_id in metadata for matching
        // Actually, dedup by slug+type
        SearchResponse response = hybridSearchService.search("great wall", null, null, "req-1");

        assertNotNull(response);
        assertFalse(response.getItems().isEmpty());
        assertEquals("req-1", response.getRequestId());
    }

    @Test
    void search_rrfFormula_k60() {
        UUID spotId = UUID.randomUUID();
        // Vector: A at rank 1 (position 0 in list → rank 1)
        Document vecDoc = createDoc("spot", "great-wall", "Great Wall", null);
        when(knowledgeSearchService.search("test", null)).thenReturn(List.of(vecDoc));

        // Keyword: A at rank 2 (position 1 in list → rank 2)
        KeywordResult kwA = new KeywordResult(spotId, "Great Wall", "great-wall", "spot", 3.0);
        KeywordResult kwB = new KeywordResult(UUID.randomUUID(), "Other", "other", "spot", 2.0);
        when(keywordSearchService.search("test")).thenReturn(List.of(kwB, kwA));

        SearchResponse response = hybridSearchService.search("test", null, null, "req-1");

        // RRF for great-wall: 1/(60+1) [vector rank 1] + 1/(60+2) [keyword rank 2]
        // = 0.01639 + 0.01613 = 0.03252
        // RRF for other: 1/(60+1) [keyword rank 1] = 0.01639
        SearchResultItem greatWall = response.getItems().stream()
                .filter(i -> i.slug().equals("great-wall")).findFirst().orElse(null);
        SearchResultItem other = response.getItems().stream()
                .filter(i -> i.slug().equals("other")).findFirst().orElse(null);

        assertNotNull(greatWall);
        assertNotNull(other);
        assertTrue(greatWall.score() > other.score(),
                "Great Wall (in both lists) should score higher than Other (keyword only)");

        // Verify exact RRF score for great-wall
        double expectedScore = 1.0 / (60 + 1) + 1.0 / (60 + 2);
        assertEquals(expectedScore, greatWall.score(), 0.0001);
    }

    @Test
    void search_deduplicatesBySlugAndType() {
        UUID spotId = UUID.randomUUID();
        Document vecDoc = createDoc("spot", "great-wall", "Great Wall", null);
        when(knowledgeSearchService.search("wall", null)).thenReturn(List.of(vecDoc));

        KeywordResult kwResult = new KeywordResult(spotId, "Great Wall", "great-wall", "spot", 3.0);
        when(keywordSearchService.search("wall")).thenReturn(List.of(kwResult));

        SearchResponse response = hybridSearchService.search("wall", null, null, "req-1");

        // Same spot should appear only once
        long count = response.getItems().stream().filter(i -> i.slug().equals("great-wall")).count();
        assertEquals(1, count);
    }

    @Test
    void search_groupCountsByType() {
        Document spotDoc = createDoc("spot", "spot-a", "Spot A", null);
        Document cityDoc = createDoc("city", "city-a", "City A", null);
        when(knowledgeSearchService.search("test", null)).thenReturn(List.of(spotDoc, cityDoc));
        when(keywordSearchService.search("test")).thenReturn(List.of());

        SearchResponse response = hybridSearchService.search("test", null, null, "req-1");

        assertEquals(1, response.getSpotsCount());
        assertEquals(0, response.getPostsCount());
        assertEquals(1, response.getCitiesCount());
    }

    @Test
    void search_chromaUnavailable_fallsBackToKeywordOnly() {
        when(knowledgeSearchService.search("beijing", null)).thenThrow(new RuntimeException("Chroma unavailable"));

        KeywordResult kw = new KeywordResult(UUID.randomUUID(), "Beijing", "beijing", "city", 3.0);
        when(keywordSearchService.search("beijing")).thenReturn(List.of(kw));

        SearchResponse response = hybridSearchService.search("beijing", null, null, "req-1");

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals("beijing", response.getItems().get(0).slug());
    }

    @Test
    void search_typeFilter_onlyReturnsSpecifiedType() {
        Document spotDoc = createDoc("spot", "spot-a", "Spot A", null);
        Document cityDoc = createDoc("city", "city-a", "City A", null);
        when(knowledgeSearchService.search("test", null)).thenReturn(List.of(spotDoc, cityDoc));

        KeywordResult kwSpot = new KeywordResult(UUID.randomUUID(), "Spot B", "spot-b", "spot", 3.0);
        KeywordResult kwCity = new KeywordResult(UUID.randomUUID(), "City B", "city-b", "city", 3.0);
        when(keywordSearchService.search("test")).thenReturn(List.of(kwSpot, kwCity));

        SearchResponse response = hybridSearchService.search("test", "spot", null, "req-1");

        assertTrue(response.getItems().stream().allMatch(i -> i.type().equals("spot")));
        assertEquals(0, response.getCitiesCount());
    }

    private Document createDoc(String entityType, String slug, String name, String nameZh) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("entity_type", entityType);
        metadata.put("slug", slug);
        metadata.put("name", name);
        if (nameZh != null) metadata.put("name_zh", nameZh);
        return new Document("text content for " + name, metadata);
    }
}
