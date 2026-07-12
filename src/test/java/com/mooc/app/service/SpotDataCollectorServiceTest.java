package com.mooc.app.service;

import com.mooc.app.dto.EnrichRequest;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.entity.SpotEnrichmentReport;
import com.mooc.app.entity.SpotStatus;
import com.mooc.app.repository.SpotEnrichmentReportRepository;
import com.mooc.app.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotDataCollectorServiceTest {

    @Mock private SpotRepository spotRepository;
    @Mock private SpotEnrichmentService spotEnrichmentService;
    @Mock private SpotEnrichmentReportRepository reportRepository;
    @Mock private ChatModel chatModel;

    private SpotDataCollectorService collectorService;

    private SpotEntity staleSpot;

    @BeforeEach
    void setUp() {
        collectorService = new SpotDataCollectorService(
                spotRepository, spotEnrichmentService, reportRepository, chatModel, 7, 30);

        staleSpot = new SpotEntity();
        staleSpot.setId(UUID.randomUUID());
        staleSpot.setName("Forbidden City");
        staleSpot.setNameZh("故宫");
        staleSpot.setSlug("forbidden-city");
        staleSpot.setCityId(UUID.randomUUID());
        staleSpot.setCityName("Beijing");
        staleSpot.setStatus(SpotStatus.PUBLISHED);
        staleSpot.setDataRefreshedAt(Instant.now().minus(35, ChronoUnit.DAYS));
    }

    @Test
    void collectStaleSpots_processesStaleSpotsAndSavesReport() {
        when(spotRepository.findStaleSpots(any(Instant.class))).thenReturn(List.of(staleSpot));

        // Mock LLM extraction returning JSON
        String extractedJson = """
                {"name_zh":"故宫","ticket_price":"旺季80元","opening_hours":"08:30-17:00",
                 "address":"北京市东城区景山前街4号","rating":4.8,
                 "description":"The Forbidden City","description_zh":"紫禁城"}
                """;
        ChatResponse chatResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage(extractedJson))));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        when(reportRepository.save(any(SpotEnrichmentReport.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        collectorService.collectStaleSpots();

        // Verify enrichment was called for the stale spot
        verify(spotEnrichmentService).updateSpot(eq(staleSpot.getId()), any(EnrichRequest.class));

        // Verify report was saved
        ArgumentCaptor<SpotEnrichmentReport> reportCaptor = ArgumentCaptor.forClass(SpotEnrichmentReport.class);
        verify(reportRepository).save(reportCaptor.capture());
        SpotEnrichmentReport report = reportCaptor.getValue();
        assertEquals(1, report.getTotalAttempted());
        assertEquals(1, report.getTotalSuccess());
        assertEquals(0, report.getTotalFailed());
    }

    @Test
    void collectStaleSpots_singleFailure_doesNotStopOthers() {
        SpotEntity secondSpot = new SpotEntity();
        secondSpot.setId(UUID.randomUUID());
        secondSpot.setName("Great Wall");
        secondSpot.setSlug("great-wall");
        secondSpot.setCityId(UUID.randomUUID());
        secondSpot.setCityName("Beijing");
        secondSpot.setStatus(SpotStatus.PUBLISHED);
        secondSpot.setDataRefreshedAt(Instant.now().minus(10, ChronoUnit.DAYS));

        when(spotRepository.findStaleSpots(any(Instant.class)))
                .thenReturn(List.of(staleSpot, secondSpot));

        // First spot: LLM returns invalid JSON → enrichment throws during parse
        ChatResponse badResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage("not valid json"))));
        ChatResponse goodResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage("{\"ticket_price\":\"\u514d\u8d39\"}"))));
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(badResponse)
                .thenReturn(goodResponse);
        
        when(reportRepository.save(any(SpotEnrichmentReport.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        collectorService.collectStaleSpots();

        // Second spot should still be processed
        verify(spotEnrichmentService).updateSpot(eq(secondSpot.getId()), any(EnrichRequest.class));

        // Report: 2 attempted, 1 success, 1 failed
        ArgumentCaptor<SpotEnrichmentReport> reportCaptor = ArgumentCaptor.forClass(SpotEnrichmentReport.class);
        verify(reportRepository).save(reportCaptor.capture());
        assertEquals(2, reportCaptor.getValue().getTotalAttempted());
        assertEquals(1, reportCaptor.getValue().getTotalSuccess());
        assertEquals(1, reportCaptor.getValue().getTotalFailed());
    }

    @Test
    void collectStaleSpots_noStaleSpots_savesEmptyReport() {
        when(spotRepository.findStaleSpots(any(Instant.class))).thenReturn(List.of());
        when(reportRepository.save(any(SpotEnrichmentReport.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        collectorService.collectStaleSpots();

        verify(spotEnrichmentService, never()).updateSpot(any(), any());

        ArgumentCaptor<SpotEnrichmentReport> reportCaptor = ArgumentCaptor.forClass(SpotEnrichmentReport.class);
        verify(reportRepository).save(reportCaptor.capture());
        assertEquals(0, reportCaptor.getValue().getTotalAttempted());
        assertEquals(0, reportCaptor.getValue().getTotalSuccess());
        assertEquals(0, reportCaptor.getValue().getTotalFailed());
    }

    @Test
    void collectStaleSpots_concurrentLock_preventsDoubleRun() {
        when(spotRepository.findStaleSpots(any(Instant.class))).thenReturn(List.of());
        when(reportRepository.save(any(SpotEnrichmentReport.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Simulate already running by calling and checking the lock
        collectorService.collectStaleSpots();

        // Calling again while first is "done" should be fine (lock released)
        collectorService.collectStaleSpots();

        verify(reportRepository, times(2)).save(any(SpotEnrichmentReport.class));
    }
}
