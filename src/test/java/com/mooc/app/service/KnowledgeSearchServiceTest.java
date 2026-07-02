package com.mooc.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeSearchServiceTest {

    private static final int EMBEDDING_DIM = 1024;
    private VectorStore vectorStore;
    private KnowledgeSearchService searchService;

    @BeforeEach
    void setUp() {
        EmbeddingModel embeddingModel = createMockEmbeddingModel();
        vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        searchService = new KnowledgeSearchService(vectorStore, 5, 0.0);
    }

    private EmbeddingModel createMockEmbeddingModel() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embed(any(String.class))).thenReturn(createFakeEmbedding());
        when(model.embed(any(Document.class))).thenReturn(createFakeEmbedding());
        when(model.call(any(EmbeddingRequest.class))).thenAnswer(invocation -> {
            EmbeddingRequest request = invocation.getArgument(0);
            List<Embedding> embeddings = request.getInstructions().stream()
                    .map(text -> new Embedding(createFakeEmbedding(), 0))
                    .toList();
            return new EmbeddingResponse(embeddings);
        });
        return model;
    }

    private float[] createFakeEmbedding() {
        float[] embedding = new float[EMBEDDING_DIM];
        for (int i = 0; i < EMBEDDING_DIM; i++) {
            embedding[i] = (float) Math.random();
        }
        return embedding;
    }

    @Test
    void search_returnsDocumentsMatchingQuery() {
        // GIVEN
        Document doc = new Document("Beijing is the capital of China with rich history",
                Map.of("entity_type", "city", "name", "Beijing"));
        vectorStore.add(List.of(doc));

        // WHEN
        List<Document> results = searchService.search("Beijing history");

        // THEN
        assertFalse(results.isEmpty());
    }

    @Test
    void search_withCityFilter_returnsOnlyMatchingDocuments() {
        // GIVEN
        Document beijingDoc = new Document("Forbidden City is a famous spot",
                Map.of("entity_type", "spot", "city_name", "Beijing"));
        Document shanghaiDoc = new Document("The Bund is a famous spot",
                Map.of("entity_type", "spot", "city_name", "Shanghai"));
        vectorStore.add(List.of(beijingDoc, shanghaiDoc));

        // WHEN
        List<Document> results = searchService.search("famous spot", "Beijing");

        // THEN
        assertTrue(results.stream()
                .allMatch(doc -> "Beijing".equals(doc.getMetadata().get("city_name"))));
    }

    @Test
    void search_withNullCity_searchesAllDocuments() {
        // GIVEN
        Document doc = new Document("Travel guide for China",
                Map.of("entity_type", "post"));
        vectorStore.add(List.of(doc));

        // WHEN
        List<Document> results = searchService.search("travel guide", null);

        // THEN
        assertFalse(results.isEmpty());
    }

    @Test
    void search_withBlankCity_treatedAsNoFilter() {
        // GIVEN
        Document doc = new Document("Great Wall of China",
                Map.of("entity_type", "spot"));
        vectorStore.add(List.of(doc));

        // WHEN
        List<Document> results = searchService.search("Great Wall", "  ");

        // THEN
        assertFalse(results.isEmpty());
    }

    @Test
    void search_respectsTopK() {
        // GIVEN
        searchService = new KnowledgeSearchService(vectorStore, 1, 0.0);
        vectorStore.add(List.of(
                new Document("Document one about Beijing", Map.of()),
                new Document("Document two about Beijing", Map.of()),
                new Document("Document three about Beijing", Map.of())
        ));

        // WHEN
        List<Document> results = searchService.search("Beijing");

        // THEN
        assertTrue(results.size() <= 1);
    }
}
