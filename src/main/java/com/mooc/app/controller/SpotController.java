package com.mooc.app.controller;

import com.mooc.app.dto.response.SpotListResponse;
import com.mooc.app.dto.response.SpotPostsResponse;
import com.mooc.app.dto.response.SpotRankingResponse;
import com.mooc.app.dto.response.SpotResponse;
import com.mooc.app.service.RankingCacheService;
import com.mooc.app.service.SpotPostService;
import com.mooc.app.service.SpotService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class SpotController {

    private final SpotService spotService;
    private final SpotPostService spotPostService;
    private final RankingCacheService rankingCacheService;

    public SpotController(SpotService spotService, SpotPostService spotPostService,
                          RankingCacheService rankingCacheService) {
        this.spotService = spotService;
        this.spotPostService = spotPostService;
        this.rankingCacheService = rankingCacheService;
    }

    @GetMapping("/api/spots")
    public ResponseEntity<SpotListResponse> listSpots(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(name = "city_id", required = false) UUID cityId,
            @RequestParam(name = "city", required = false) String citySlug,
            @RequestParam(defaultValue = "latest") String sort,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        SpotListResponse response = spotService.listSpots(page, size, cityId, citySlug, sort, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/spots/{idOrSlug}")
    public ResponseEntity<SpotResponse> getSpot(
            @PathVariable String idOrSlug,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        SpotResponse response = spotService.getSpot(idOrSlug, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/spots/ranking")
    public ResponseEntity<SpotRankingResponse> getRanking(
            @RequestParam String type,
            @RequestParam(defaultValue = "10") int top,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        SpotRankingResponse response = rankingCacheService.getRanking(type, top, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/spots/{id}/posts")
    public ResponseEntity<SpotPostsResponse> getSpotPosts(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        SpotPostsResponse response = spotPostService.getPostsBySpotId(id, page, size, requestId);
        return ResponseEntity.ok(response);
    }
}
