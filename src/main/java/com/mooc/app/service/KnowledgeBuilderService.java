package com.mooc.app.service;

import com.mooc.app.entity.CityEntity;
import com.mooc.app.entity.PostEntity;
import com.mooc.app.entity.PostStatus;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.entity.SpotStatus;
import com.mooc.app.repository.CityRepository;
import com.mooc.app.repository.PostRepository;
import com.mooc.app.repository.SpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KnowledgeBuilderService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBuilderService.class);
    private static final int POST_LONG_CONTENT_THRESHOLD = 1000;
    private static final int EMBEDDING_BATCH_SIZE = 2;

    private final CityRepository cityRepository;
    private final SpotRepository spotRepository;
    private final PostRepository postRepository;
    private final VectorStore vectorStore;
    private final int maxChunkSize;
    private final int chunkOverlap;

    public KnowledgeBuilderService(CityRepository cityRepository,
                                    SpotRepository spotRepository,
                                    PostRepository postRepository,
                                    VectorStore vectorStore,
                                    @Value("${app.rag.max-chunk-size:800}") int maxChunkSize,
                                    @Value("${app.rag.chunk-overlap:100}") int chunkOverlap) {
        this.cityRepository = cityRepository;
        this.spotRepository = spotRepository;
        this.postRepository = postRepository;
        this.vectorStore = vectorStore;
        this.maxChunkSize = maxChunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    @Async
    public void rebuildAllAsync() {
        rebuildAll();
    }

    public void rebuildAll() {
        Instant start = Instant.now();

        List<Document> allDocuments = new ArrayList<>();
        allDocuments.addAll(buildCityDocuments());
        allDocuments.addAll(buildSpotDocuments());
        allDocuments.addAll(buildPostDocuments());

        if (!allDocuments.isEmpty()) {
            for (int i = 0; i < allDocuments.size(); i += EMBEDDING_BATCH_SIZE) {
                List<Document> batch = allDocuments.subList(i, Math.min(i + EMBEDDING_BATCH_SIZE, allDocuments.size()));
                vectorStore.add(batch);
            }
        }

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("Knowledge base rebuilt: {} documents indexed in {}ms",
                allDocuments.size(), elapsed.toMillis());
    }

    List<Document> buildCityDocuments() {
        return cityRepository.findAll().stream()
                .filter(city -> !city.isDeleted())
                .map(this::toCityDocument)
                .collect(Collectors.toList());
    }

    List<Document> buildSpotDocuments() {
        return spotRepository.findAll().stream()
                .filter(spot -> !spot.isDeleted() && spot.getStatus() == SpotStatus.PUBLISHED)
                .map(this::toSpotDocument)
                .collect(Collectors.toList());
    }

    List<Document> buildPostDocuments() {
        return postRepository.findAll().stream()
                .filter(post -> !post.isDeleted() && post.getStatus() == PostStatus.PUBLISHED)
                .flatMap(post -> toPostDocuments(post).stream())
                .collect(Collectors.toList());
    }

    private Document toCityDocument(CityEntity city) {
        String text = String.format("City: %s (%s)\nDescription: %s\nBest Season: %s",
                city.getName(),
                city.getNameZh() != null ? city.getNameZh() : "",
                city.getDescription() != null ? city.getDescription() : "",
                city.getBestSeason() != null ? city.getBestSeason() : "");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("entity_type", "city");
        metadata.put("slug", city.getSlug());
        metadata.put("name", city.getName());
        if (city.getNameZh() != null) {
            metadata.put("name_zh", city.getNameZh());
        }

        return new Document(text, metadata);
    }

    private Document toSpotDocument(SpotEntity spot) {
        String tags = spot.getTags() != null ? String.join(", ", spot.getTags()) : "";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Spot: %s (%s)\nCity: %s\nTags: %s\nRating: %s",
                spot.getName(),
                spot.getNameZh() != null ? spot.getNameZh() : "",
                spot.getCityName() != null ? spot.getCityName() : "",
                tags,
                spot.getRating()));
        if (spot.getTicketPrice() != null) {
            sb.append("\nTicket Price: ").append(spot.getTicketPrice());
        }
        if (spot.getOpeningHours() != null) {
            sb.append("\nOpening Hours: ").append(spot.getOpeningHours());
        }
        if (spot.getAddress() != null) {
            sb.append("\nAddress: ").append(spot.getAddress());
        }
        if (spot.getDescription() != null) {
            sb.append("\nDescription: ").append(spot.getDescription());
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("entity_type", "spot");
        metadata.put("slug", spot.getSlug());
        metadata.put("name", spot.getName());
        if (spot.getNameZh() != null) {
            metadata.put("name_zh", spot.getNameZh());
        }
        if (spot.getCityName() != null) {
            metadata.put("city_name", spot.getCityName());
        }
        if (!tags.isEmpty()) {
            metadata.put("tags", String.join(",", spot.getTags()));
        }

        return new Document(sb.toString(), metadata);
    }

    private List<Document> toPostDocuments(PostEntity post) {
        String tags = post.getTags() != null ? String.join(", ", post.getTags()) : "";
        String text = String.format("Post: %s\nTags: %s\nContent: %s",
                post.getTitle(),
                tags,
                post.getContent());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("entity_type", "post");
        metadata.put("slug", post.getSlug());
        metadata.put("title", post.getTitle());
        if (!tags.isEmpty()) {
            metadata.put("tags", String.join(",", post.getTags()));
        }

        Document doc = new Document(text, metadata);

        if (post.getContent().length() > POST_LONG_CONTENT_THRESHOLD) {
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(maxChunkSize)
                    .withMinChunkSizeChars(chunkOverlap)
                    .build();
            List<Document> chunks = splitter.apply(List.of(doc));
            chunks.forEach(chunk -> chunk.getMetadata().putAll(metadata));
            return chunks;
        }

        return List.of(doc);
    }
}
