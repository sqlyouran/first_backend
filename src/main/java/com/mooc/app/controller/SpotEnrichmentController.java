package com.mooc.app.controller;

import com.mooc.app.dto.response.EnrichmentReportResponse;
import com.mooc.app.dto.response.StaleSpotListResponse;
import com.mooc.app.dto.response.StaleSpotResponse;
import com.mooc.app.entity.SpotEnrichmentReport;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.exception.SpotException;
import com.mooc.app.repository.SpotEnrichmentReportRepository;
import com.mooc.app.repository.SpotRepository;
import com.mooc.app.service.SpotDataCollectorService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
public class SpotEnrichmentController {

    private final SpotRepository spotRepository;
    private final SpotDataCollectorService collectorService;
    private final SpotEnrichmentReportRepository reportRepository;

    public SpotEnrichmentController(SpotRepository spotRepository,
                                     SpotDataCollectorService collectorService,
                                     SpotEnrichmentReportRepository reportRepository) {
        this.spotRepository = spotRepository;
        this.collectorService = collectorService;
        this.reportRepository = reportRepository;
    }

    @GetMapping("/api/spots/stale")
    public ResponseEntity<StaleSpotListResponse> getStaleSpots(HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        Instant cutoff = Instant.now().minus(Duration.ofDays(7));
        List<SpotEntity> staleSpots = spotRepository.findStaleSpots(cutoff);

        Instant now = Instant.now();
        List<StaleSpotResponse> items = staleSpots.stream()
                .map(spot -> {
                    long daysSinceRefresh = spot.getDataRefreshedAt() != null
                            ? Duration.between(spot.getDataRefreshedAt(), now).toDays()
                            : Long.MAX_VALUE;
                    String priority = daysSinceRefresh > 30 ? "critical" : "normal";
                    return new StaleSpotResponse(
                            spot.getId(), spot.getName(), spot.getNameZh(),
                            spot.getSlug(), spot.getCityName(),
                            spot.getDataRefreshedAt(), daysSinceRefresh, priority);
                })
                .toList();

        return ResponseEntity.ok(new StaleSpotListResponse(requestId, items, items.size()));
    }

    @PostMapping("/api/spots/enrichment/trigger")
    public ResponseEntity<Void> triggerEnrichment() {
        if (collectorService.isRunning()) {
            throw new SpotException(HttpStatus.CONFLICT, "already_running",
                    "Enrichment collection is already in progress");
        }
        collectorService.collectStaleSpotsAsync();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/spots/enrichment/report/latest")
    public ResponseEntity<EnrichmentReportResponse> getLatestReport(HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        SpotEnrichmentReport report = reportRepository.findTopByOrderByStartedAtDesc()
                .orElseThrow(() -> new SpotException(HttpStatus.NOT_FOUND, "not_found",
                        "No enrichment report found"));

        EnrichmentReportResponse response = new EnrichmentReportResponse(
                requestId, report.getRunId(), report.getStartedAt(), report.getCompletedAt(),
                report.getTotalAttempted(), report.getTotalSuccess(), report.getTotalFailed(),
                report.getDetails());
        return ResponseEntity.ok(response);
    }
}
