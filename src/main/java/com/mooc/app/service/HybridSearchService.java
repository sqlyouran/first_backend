package com.mooc.app.service;

import com.mooc.app.dto.response.SearchResponse;
import com.mooc.app.dto.response.SearchResultItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);

    private final KnowledgeSearchService knowledgeSearchService;
    private final KeywordSearchService keywordSearchService;

    @Value("${app.search.rrf-k:60}")
    private int rrfK;

    @Value("${app.search.vector-top-k:10}")
    private int vectorTopK;

    public HybridSearchService(KnowledgeSearchService knowledgeSearchService,
                               KeywordSearchService keywordSearchService) {
        this.knowledgeSearchService = knowledgeSearchService;
        this.keywordSearchService = keywordSearchService;
    }

    public SearchResponse search(String query, String type, String city, String requestId) {
        // Keyword results (always available)
        List<KeywordResult> keywordResults = keywordSearchService.search(query);

        // Vector results (may fail if Chroma is unavailable)
        List<Document> vectorResults;
        try {
            vectorResults = knowledgeSearchService.search(query, city);
        } catch (Exception e) {
            log.warn("Vector search unavailable, falling back to keyword-only: {}", e.getMessage());
            vectorResults = List.of();
        }

        // RRF merge
        Map<String, RrfEntry> merged = rrfMerge(vectorResults, keywordResults);

        // Convert to SearchResultItem list
        List<SearchResultItem> items = merged.values().stream()
                .sorted(Comparator.comparingDouble(e -> -e.score))
                .map(e -> toSearchResultItem(e))
                .toList();

        // Apply type filter
        if (type != null && !type.isBlank()) {
            items = items.stream().filter(i -> i.type().equals(type)).toList();
        }

        // Count by type
        int spotsCount = (int) items.stream().filter(i -> "spot".equals(i.type())).count();
        int postsCount = (int) items.stream().filter(i -> "post".equals(i.type())).count();
        int citiesCount = (int) items.stream().filter(i -> "city".equals(i.type())).count();

        return new SearchResponse(requestId, items, spotsCount, postsCount, citiesCount);
    }

    private Map<String, RrfEntry> rrfMerge(List<Document> vectorResults, List<KeywordResult> keywordResults) {
        Map<String, RrfEntry> merged = new LinkedHashMap<>();

        // Vector results: rank = position + 1
        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            String entityType = (String) doc.getMetadata().getOrDefault("entity_type", "unknown");
            String slug = (String) doc.getMetadata().getOrDefault("slug", "");
            String key = entityType + ":" + slug;
            String name = (String) doc.getMetadata().getOrDefault("name",
                    doc.getMetadata().getOrDefault("title", ""));
            String nameZh = (String) doc.getMetadata().get("name_zh");

            double rrfScore = 1.0 / (rrfK + i + 1);
            merged.merge(key, new RrfEntry(key, entityType, slug, name, nameZh, rrfScore),
                    (existing, entry) -> new RrfEntry(existing.key, existing.type, existing.slug,
                            existing.name, existing.nameZh, existing.score + entry.score));
        }

        // Keyword results: rank = position + 1
        for (int i = 0; i < keywordResults.size(); i++) {
            KeywordResult kr = keywordResults.get(i);
            String key = kr.type() + ":" + kr.slug();
            double rrfScore = 1.0 / (rrfK + i + 1);

            merged.merge(key, new RrfEntry(key, kr.type(), kr.slug(), kr.name(), null, rrfScore),
                    (existing, entry) -> new RrfEntry(existing.key, existing.type, existing.slug,
                            existing.name, existing.nameZh, existing.score + entry.score));
        }

        return merged;
    }

    private SearchResultItem toSearchResultItem(RrfEntry entry) {
        String summary = null; // summary can be populated later
        return new SearchResultItem(entry.type, null, entry.slug, entry.name, entry.nameZh, summary, entry.score);
    }

    private record RrfEntry(String key, String type, String slug, String name, String nameZh, double score) {}
}
