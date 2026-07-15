package com.mooc.app.controller;

import com.mooc.app.config.SecurityConfig;
import com.mooc.app.entity.SpotEnrichmentReport;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.entity.SpotStatus;
import com.mooc.app.repository.SpotEnrichmentReportRepository;
import com.mooc.app.repository.SpotRepository;
import com.mooc.app.service.JwtService;
import com.mooc.app.service.SpotDataCollectorService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpotEnrichmentController.class)
@Import(SecurityConfig.class)
class SpotEnrichmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SpotRepository spotRepository;
    @MockBean private SpotDataCollectorService collectorService;
    @MockBean private SpotEnrichmentReportRepository reportRepository;
    @MockBean private JwtService jwtService;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private String adminToken;

    @BeforeEach
    void setup() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        String userId = UUID.randomUUID().toString();
        adminToken = Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(1800)))
                .signWith(key)
                .compact();

        Claims claims = new DefaultClaims(Map.of("sub", userId));
        when(jwtService.parseToken(adminToken)).thenReturn(Optional.of(claims));
    }

    @Test
    void getStaleSpots_returnsList() throws Exception {
        SpotEntity spot = new SpotEntity();
        spot.setId(UUID.randomUUID());
        spot.setName("Forbidden City");
        spot.setNameZh("故宫");
        spot.setSlug("forbidden-city");
        spot.setCityName("Beijing");
        spot.setStatus(SpotStatus.PUBLISHED);
        spot.setDataRefreshedAt(Instant.now().minus(35, ChronoUnit.DAYS));

        when(spotRepository.findStaleSpots(any(Instant.class))).thenReturn(List.of(spot));

        mockMvc.perform(get("/api/spots/stale")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].slug").value("forbidden-city"))
                .andExpect(jsonPath("$.items[0].priority").value("critical"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void getStaleSpots_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/spots/stale"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void triggerEnrichment_startsCollection() throws Exception {
        when(collectorService.isRunning()).thenReturn(false);

        mockMvc.perform(post("/api/spots/enrichment/trigger")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        verify(collectorService).collectStaleSpotsAsync();
    }

    @Test
    void triggerEnrichment_alreadyRunning_returns409() throws Exception {
        when(collectorService.isRunning()).thenReturn(true);

        mockMvc.perform(post("/api/spots/enrichment/trigger")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("already_running"));
    }

    @Test
    void getLatestReport_returnsReport() throws Exception {
        SpotEnrichmentReport report = new SpotEnrichmentReport();
        report.setRunId("run-123");
        report.setStartedAt(Instant.parse("2026-07-12T02:00:00Z"));
        report.setCompletedAt(Instant.parse("2026-07-12T02:05:00Z"));
        report.setTotalAttempted(5);
        report.setTotalSuccess(4);
        report.setTotalFailed(1);
        report.setDetails("[]");

        when(reportRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.of(report));

        mockMvc.perform(get("/api/spots/enrichment/report/latest")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run_id").value("run-123"))
                .andExpect(jsonPath("$.total_attempted").value(5))
                .andExpect(jsonPath("$.total_success").value(4));
    }

    @Test
    void getLatestReport_noReport_returns404() throws Exception {
        when(reportRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/spots/enrichment/report/latest")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("not_found"));
    }
}
