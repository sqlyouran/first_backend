package com.mooc.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RagConfig {

    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);
    private static final String DEFAULT_TENANT = "default_tenant";
    private static final String DEFAULT_DATABASE = "default_database";

    @Bean
    @ConditionalOnProperty(name = "app.rag.enabled", havingValue = "true", matchIfMissing = true)
    public VectorStore chromaVectorStore(EmbeddingModel embeddingModel,
                                         @Value("${app.chroma.host:http://localhost}") String host,
                                         @Value("${app.chroma.port:8000}") int port,
                                         @Value("${app.chroma.collection-name:wanderchina-knowledge}") String collectionName) {
        
        // 配置自定义 HttpClient 增加超时
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(requestFactory);
        
        ChromaApi chromaApi = ChromaApi.builder()
                .baseUrl(host + ":" + port)
                .restClientBuilder(restClientBuilder)
                .build();

        ensureCollectionExists(chromaApi, collectionName);

        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .tenantName(DEFAULT_TENANT)
                .databaseName(DEFAULT_DATABASE)
                .collectionName(collectionName)
                .initializeSchema(true)
                .build();
    }

    private void ensureCollectionExists(ChromaApi chromaApi, String collectionName) {
        try {
            ChromaApi.Collection existing = chromaApi.getCollection(DEFAULT_TENANT, DEFAULT_DATABASE, collectionName);
            if (existing != null) {
                log.info("Chroma collection '{}' already exists", collectionName);
                return;
            }
        } catch (Exception e) {
            log.debug("Collection '{}' not found, will create: {}", collectionName, e.getMessage());
        }

        try {
            ensureTenantExists(chromaApi, DEFAULT_TENANT);
            ensureDatabaseExists(chromaApi, DEFAULT_TENANT, DEFAULT_DATABASE);
            chromaApi.createCollection(DEFAULT_TENANT, DEFAULT_DATABASE,
                    new ChromaApi.CreateCollectionRequest(collectionName));
            log.info("Created Chroma collection '{}'", collectionName);
        } catch (Exception e) {
            log.warn("Failed to create Chroma collection '{}': {}", collectionName, e.getMessage());
        }
    }

    private void ensureTenantExists(ChromaApi chromaApi, String tenantName) {
        try {
            chromaApi.getTenant(tenantName);
        } catch (Exception e) {
            chromaApi.createTenant(tenantName);
        }
    }

    private void ensureDatabaseExists(ChromaApi chromaApi, String tenantName, String databaseName) {
        try {
            chromaApi.getDatabase(tenantName, databaseName);
        } catch (Exception e) {
            chromaApi.createDatabase(databaseName, tenantName);
        }
    }

    @Bean
    @ConditionalOnProperty(name = "app.rag.enabled", havingValue = "false")
    public VectorStore inMemoryVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
