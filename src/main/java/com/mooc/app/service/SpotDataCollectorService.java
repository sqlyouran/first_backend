package com.mooc.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.EnrichRequest;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.entity.SpotEnrichmentReport;
import com.mooc.app.repository.SpotEnrichmentReportRepository;
import com.mooc.app.repository.SpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SpotDataCollectorService {

    private static final Logger log = LoggerFactory.getLogger(SpotDataCollectorService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final SpotRepository spotRepository;
    private final SpotEnrichmentService spotEnrichmentService;
    private final SpotEnrichmentReportRepository reportRepository;
    private final ChatModel chatModel;
    private final int staleDays;
    private final int criticalDays;

    public SpotDataCollectorService(SpotRepository spotRepository,
                                     SpotEnrichmentService spotEnrichmentService,
                                     SpotEnrichmentReportRepository reportRepository,
                                     ChatModel chatModel,
                                     @Value("${app.enrichment.stale-days:7}") int staleDays,
                                     @Value("${app.enrichment.critical-days:30}") int criticalDays) {
        this.spotRepository = spotRepository;
        this.spotEnrichmentService = spotEnrichmentService;
        this.reportRepository = reportRepository;
        this.chatModel = chatModel;
        this.staleDays = staleDays;
        this.criticalDays = criticalDays;
    }

    public boolean isRunning() {
        return running.get();
    }

    @Scheduled(cron = "${app.enrichment.cron:0 0 2 * * *}")
    @Async
    public void scheduledCollect() {
        collectStaleSpots();
    }

    @Async
    public void collectStaleSpotsAsync() {
        collectStaleSpots();
    }

    public void collectStaleSpots() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Collection already in progress, skipping");
            return;
        }
        try {
            doCollect();
        } finally {
            running.set(false);
        }
    }

    private void doCollect() {
        Instant startedAt = Instant.now();
        String runId = UUID.randomUUID().toString();
        log.info("Starting spot data collection run: {}", runId);

        Instant cutoff = Instant.now().minus(staleDays, ChronoUnit.DAYS);
        List<SpotEntity> staleSpots = spotRepository.findStaleSpots(cutoff);

        int totalAttempted = 0;
        int totalSuccess = 0;
        int totalFailed = 0;
        List<String> details = new ArrayList<>();

        for (SpotEntity spot : staleSpots) {
            totalAttempted++;
            try {
                enrichSpot(spot);
                totalSuccess++;
                details.add(String.format("{\"slug\":\"%s\",\"status\":\"success\"}", spot.getSlug()));
                log.info("Successfully enriched: {} ({})", spot.getName(), spot.getSlug());
            } catch (Exception e) {
                totalFailed++;
                details.add(String.format("{\"slug\":\"%s\",\"status\":\"failed\",\"error\":\"%s\"}",
                        spot.getSlug(), escapeJson(e.getMessage())));
                log.error("Failed to enrich {}: {}", spot.getSlug(), e.getMessage());
            }
        }

        Instant completedAt = Instant.now();
        Duration elapsed = Duration.between(startedAt, completedAt);

        SpotEnrichmentReport report = new SpotEnrichmentReport();
        report.setRunId(runId);
        report.setStartedAt(startedAt);
        report.setCompletedAt(completedAt);
        report.setTotalAttempted(totalAttempted);
        report.setTotalSuccess(totalSuccess);
        report.setTotalFailed(totalFailed);
        report.setDetails("[" + String.join(",", details) + "]");
        reportRepository.save(report);

        log.info("Collection run {} completed: {} attempted, {} success, {} failed in {}ms",
                runId, totalAttempted, totalSuccess, totalFailed, elapsed.toMillis());
    }

    private void enrichSpot(SpotEntity spot) {
        // Build extraction prompt for LLM
        String extractionPrompt = buildExtractionPrompt(spot);
        var response = chatModel.call(new Prompt(extractionPrompt));

        String llmContent = response.getResult().getOutput().getText();
        EnrichRequest enrichRequest = parseLlmResponse(llmContent);

        spotEnrichmentService.updateSpot(spot.getId(), enrichRequest);
    }

    private String buildExtractionPrompt(SpotEntity spot) {
        return """
                Extract structured information about the spot "%s" from the context below.
                Return ONLY a JSON object with these fields (use null for unknown fields):
                {
                  "name_zh": "Chinese name",
                  "ticket_price": "ticket price info",
                  "opening_hours": "opening hours",
                  "address": "full address",
                  "rating": 4.5,
                  "description": "English description",
                  "description_zh": "Chinese description"
                }
                
                Spot: %s (%s), City: %s
                """.formatted(spot.getName(), spot.getName(), spot.getNameZh(), spot.getCityName());
    }

    EnrichRequest parseLlmResponse(String content) {
        try {
            // Extract JSON from response (might be wrapped in markdown code blocks)
            String json = extractJson(content);
            JsonNode node = objectMapper.readTree(json);

            return new EnrichRequest(
                    textOrNull(node, "name_zh"),
                    textOrNull(node, "ticket_price"),
                    textOrNull(node, "opening_hours"),
                    textOrNull(node, "address"),
                    node.has("rating") && !node.get("rating").isNull()
                            ? new BigDecimal(node.get("rating").asText()) : null,
                    textOrNull(node, "description"),
                    textOrNull(node, "description_zh")
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse LLM response as JSON: " + content, e);
        }
    }

    private String extractJson(String content) {
        if (content == null) throw new RuntimeException("LLM returned null content");
        // Strip markdown code blocks if present
        String trimmed = content.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private String textOrNull(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) return null;
        return node.get(field).asText();
    }

    private String escapeJson(String text) {
        if (text == null) return "null";
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }
}
