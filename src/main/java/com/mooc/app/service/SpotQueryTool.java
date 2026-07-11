package com.mooc.app.service;

import com.mooc.app.dto.response.SpotRankingResponse;
import com.mooc.app.dto.response.SpotResponse;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.repository.SpotRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SpotQueryTool {

    private static final int MAX_RESULTS_PER_QUERY = 10;

    private final SpotRepository spotRepository;
    private final SpotService spotService;

    public SpotQueryTool(SpotRepository spotRepository, SpotService spotService) {
        this.spotRepository = spotRepository;
        this.spotService = spotService;
    }

    @Tool(description = "Search for tourist spots in a specific city. Returns a list of spots with name, rating, and tags.")
    public String searchSpotsByCity(
            @ToolParam(description = "City slug, e.g. 'Hangzhou', 'Beijing', 'Shanghai'") String citySlug) {
        Page<SpotEntity> page = spotRepository.findByCitySlugAndDeletedFalse(
                citySlug,
                PageRequest.of(0, MAX_RESULTS_PER_QUERY, Sort.by(Sort.Direction.DESC, "rating")));

        if (page.isEmpty()) {
            return "No spots found in " + citySlug;
        }

        return page.getContent().stream()
                .map(spot -> String.format("- %s (%s) | Rating: %s | Tags: %s",
                        spot.getName(),
                        spot.getNameZh() != null ? spot.getNameZh() : "",
                        spot.getRating() != null ? spot.getRating().toPlainString() : "N/A",
                        String.join(", ", spot.getTags())))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Get detailed information about a specific tourist spot including description, tags, rating, and gallery.")
    public String getSpotDetails(
            @ToolParam(description = "Spot slug or name identifier, e.g. 'lingyin-temple', 'west-lake'") String nameOrSlug) {
        Optional<SpotEntity> optionalSpot = spotRepository.findBySlugAndDeletedFalse(nameOrSlug);
        if (optionalSpot.isEmpty()) {
            return "Spot not found: " + nameOrSlug;
        }

        SpotEntity spot = optionalSpot.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(spot.getName());
        if (spot.getNameZh() != null) {
            sb.append(" (").append(spot.getNameZh()).append(")");
        }
        sb.append("\nCity: ").append(spot.getCityName() != null ? spot.getCityName() : "Unknown");
        sb.append("\nRating: ").append(spot.getRating() != null ? spot.getRating().toPlainString() : "N/A");
        sb.append("\nTags: ").append(String.join(", ", spot.getTags()));
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
        if (spot.getDescriptionZh() != null) {
            sb.append("\nDescription (Chinese): ").append(spot.getDescriptionZh());
        }
        if (spot.getGallery() != null && !spot.getGallery().isEmpty()) {
            sb.append("\nGallery: ").append(String.join(", ", spot.getGallery()));
        }
        return sb.toString();
    }

    @Tool(description = "Get the top rated tourist spots across all cities. Returns spots sorted by rating.")
    public String getTopRatedSpots(
            @ToolParam(description = "Number of top spots to return, between 1 and 10") int limit) {
        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_RESULTS_PER_QUERY);
        SpotRankingResponse response = spotService.getRanking(
                "rating", effectiveLimit, UUID.randomUUID().toString());

        if (response.getItems().isEmpty()) {
            return "No top rated spots available";
        }

        return response.getItems().stream()
                .map(spot -> String.format("- %s (%s) | Rating: %s | City: %s",
                        spot.getName(),
                        spot.getNameZh() != null ? spot.getNameZh() : "",
                        spot.getRating(),
                        spot.getCityName() != null ? spot.getCityName() : ""))
                .collect(Collectors.joining("\n"));
    }
}
