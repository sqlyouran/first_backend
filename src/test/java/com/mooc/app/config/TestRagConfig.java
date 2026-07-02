package com.mooc.app.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class TestRagConfig {

    private static final int EMBEDDING_DIM = 1024;

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "none")
    public EmbeddingModel mockEmbeddingModel() {
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);

        when(embeddingModel.embed(any(String.class))).thenReturn(createFakeEmbedding());
        when(embeddingModel.embed(any(Document.class))).thenReturn(createFakeEmbedding());

        when(embeddingModel.call(any(EmbeddingRequest.class))).thenAnswer(invocation -> {
            EmbeddingRequest request = invocation.getArgument(0);
            List<Embedding> embeddings = request.getInstructions().stream()
                    .map(text -> new Embedding(createFakeEmbedding(), 0))
                    .toList();
            return new EmbeddingResponse(embeddings);
        });

        return embeddingModel;
    }

    private float[] createFakeEmbedding() {
        float[] embedding = new float[EMBEDDING_DIM];
        for (int i = 0; i < EMBEDDING_DIM; i++) {
            embedding[i] = (float) Math.random();
        }
        return embedding;
    }
}
