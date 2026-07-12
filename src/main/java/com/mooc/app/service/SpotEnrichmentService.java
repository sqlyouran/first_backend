package com.mooc.app.service;

import com.mooc.app.dto.EnrichRequest;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.exception.SpotException;
import com.mooc.app.repository.SpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class SpotEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(SpotEnrichmentService.class);

    private final SpotRepository spotRepository;
    private final KnowledgeBuilderService knowledgeBuilderService;

    public SpotEnrichmentService(SpotRepository spotRepository,
                                  KnowledgeBuilderService knowledgeBuilderService) {
        this.spotRepository = spotRepository;
        this.knowledgeBuilderService = knowledgeBuilderService;
    }

    public void updateSpot(UUID spotId, EnrichRequest request) {
        SpotEntity spot = spotRepository.findByIdAndDeletedFalse(spotId)
                .orElseThrow(() -> new SpotException(HttpStatus.NOT_FOUND, "not_found",
                        "Spot not found: " + spotId));

        if (request.nameZh() != null) spot.setNameZh(request.nameZh());
        if (request.ticketPrice() != null) spot.setTicketPrice(request.ticketPrice());
        if (request.openingHours() != null) spot.setOpeningHours(request.openingHours());
        if (request.address() != null) spot.setAddress(request.address());
        if (request.rating() != null) spot.setRating(request.rating());
        if (request.description() != null) spot.setDescription(request.description());
        if (request.descriptionZh() != null) spot.setDescriptionZh(request.descriptionZh());

        spot.setDataRefreshedAt(Instant.now());
        spotRepository.save(spot);

        log.info("Spot enriched: {} ({})", spot.getName(), spot.getSlug());

        knowledgeBuilderService.refreshSpotDocument(spot);
    }
}
