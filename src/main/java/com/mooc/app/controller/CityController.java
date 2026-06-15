package com.mooc.app.controller;

import com.mooc.app.dto.response.CityListResponse;
import com.mooc.app.dto.response.CityResponse;
import com.mooc.app.service.CityService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping("/api/cities")
    public ResponseEntity<CityListResponse> listCities(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        CityListResponse response = cityService.listCities(page, size, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/cities/{id}")
    public ResponseEntity<CityResponse> getCity(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        CityResponse response = cityService.getCity(id, requestId);
        return ResponseEntity.ok(response);
    }
}
