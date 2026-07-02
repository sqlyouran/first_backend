package com.mooc.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;

    public KnowledgeSearchService(VectorStore vectorStore,
                                   @Value("${app.rag.top-k:5}") int topK,
                                   @Value("${app.rag.similarity-threshold:0.3}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public List<Document> search(String query) {
        return search(query, null);
    }

    public List<Document> search(String query, String cityName) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold);

        if (cityName != null && !cityName.isBlank()) {
            builder.filterExpression("city_name == '" + cityName + "'");
            log.debug("Knowledge search with city filter: {}", cityName);
        }

        List<Document> results = vectorStore.similaritySearch(builder.build());
        log.debug("Knowledge search returned {} results for query: {}", results.size(), query);
        return results;
    }
}
