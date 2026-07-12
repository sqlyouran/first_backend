package com.mooc.app.service;

import com.mooc.app.dto.EnrichRequest;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.entity.SpotStatus;
import com.mooc.app.exception.SpotException;
import com.mooc.app.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotEnrichmentServiceTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private KnowledgeBuilderService knowledgeBuilderService;

    @InjectMocks
    private SpotEnrichmentService spotEnrichmentService;

    private SpotEntity existingSpot;
    private UUID spotId;

    @BeforeEach
    void setUp() {
        spotId = UUID.randomUUID();
        existingSpot = new SpotEntity();
        existingSpot.setId(spotId);
        existingSpot.setName("Forbidden City");
        existingSpot.setSlug("forbidden-city");
        existingSpot.setCityId(UUID.randomUUID());
        existingSpot.setStatus(SpotStatus.PUBLISHED);
        existingSpot.setTicketPrice("旺季60元");
        existingSpot.setOpeningHours("08:30-17:00");
        existingSpot.setAddress("景山前街4号");
        existingSpot.setRating(new BigDecimal("4.5"));
    }

    @Test
    void updateSpot_successfullyUpdatesFields() {
        when(spotRepository.findByIdAndDeletedFalse(spotId)).thenReturn(Optional.of(existingSpot));
        when(spotRepository.save(any(SpotEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrichRequest request = new EnrichRequest(
                "故宫", "旺季80元", "08:30-17:30", "北京市东城区景山前街4号",
                new BigDecimal("4.8"), "The Forbidden City", "紫禁城"
        );

        spotEnrichmentService.updateSpot(spotId, request);

        assertEquals("故宫", existingSpot.getNameZh());
        assertEquals("旺季80元", existingSpot.getTicketPrice());
        assertEquals("08:30-17:30", existingSpot.getOpeningHours());
        assertEquals("北京市东城区景山前街4号", existingSpot.getAddress());
        assertEquals(0, existingSpot.getRating().compareTo(new BigDecimal("4.8")));
        assertEquals("The Forbidden City", existingSpot.getDescription());
        assertEquals("紫禁城", existingSpot.getDescriptionZh());
        assertNotNull(existingSpot.getDataRefreshedAt());

        verify(spotRepository).save(existingSpot);
        verify(knowledgeBuilderService).refreshSpotDocument(existingSpot);
    }

    @Test
    void updateSpot_nullFieldsDoNotOverwrite() {
        when(spotRepository.findByIdAndDeletedFalse(spotId)).thenReturn(Optional.of(existingSpot));
        when(spotRepository.save(any(SpotEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Only ticketPrice is non-null, everything else is null
        EnrichRequest request = new EnrichRequest(
                null, "免费", null, null, null, null, null
        );

        spotEnrichmentService.updateSpot(spotId, request);

        // ticketPrice updated
        assertEquals("免费", existingSpot.getTicketPrice());
        // existing values preserved
        assertEquals("08:30-17:00", existingSpot.getOpeningHours());
        assertEquals("景山前街4号", existingSpot.getAddress());
        assertEquals(0, existingSpot.getRating().compareTo(new BigDecimal("4.5")));
        assertNull(existingSpot.getNameZh());
        // dataRefreshedAt still set
        assertNotNull(existingSpot.getDataRefreshedAt());
    }

    @Test
    void updateSpot_spotNotFound_throwsException() {
        when(spotRepository.findByIdAndDeletedFalse(spotId)).thenReturn(Optional.empty());

        EnrichRequest request = new EnrichRequest(null, "免费", null, null, null, null, null);

        SpotException ex = assertThrows(SpotException.class, () ->
                spotEnrichmentService.updateSpot(spotId, request));

        assertEquals("not_found", ex.getErrorCode());
    }

    @Test
    void updateSpot_triggersChromaRefresh() {
        when(spotRepository.findByIdAndDeletedFalse(spotId)).thenReturn(Optional.of(existingSpot));
        when(spotRepository.save(any(SpotEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrichRequest request = new EnrichRequest(null, "免费", null, null, null, null, null);

        spotEnrichmentService.updateSpot(spotId, request);

        verify(knowledgeBuilderService).refreshSpotDocument(existingSpot);
    }
}
