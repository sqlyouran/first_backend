package com.mooc.app.controller;

import com.mooc.app.dto.response.ExchangeRateResponse;
import com.mooc.app.dto.response.WeatherResponse;
import com.mooc.app.service.ExchangeRateData;
import com.mooc.app.service.ExchangeRateService;
import com.mooc.app.service.WeatherData;
import com.mooc.app.service.WeatherService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ServiceController {

    private final WeatherService weatherService;
    private final ExchangeRateService exchangeRateService;

    public ServiceController(WeatherService weatherService, ExchangeRateService exchangeRateService) {
        this.weatherService = weatherService;
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/api/services/weather")
    public ResponseEntity<WeatherResponse> getWeather(
            @RequestParam String city,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        WeatherData data = weatherService.getWeather(city);
        WeatherResponse response = new WeatherResponse(
                requestId, data.city(), data.temperature(), data.description(),
                data.icon(), data.humidity(), data.windSpeed(),
                data.updatedAt().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/services/exchange-rate")
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(
            @RequestParam(defaultValue = "USD") String base,
            HttpServletRequest httpRequest) {
        String requestId = AuthUtil.getRequestId(httpRequest);
        ExchangeRateData data = exchangeRateService.getRates(base);
        Map<String, String> rates = data.rates().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toPlainString()));
        ExchangeRateResponse response = new ExchangeRateResponse(
                requestId, data.base(), rates, data.updatedAt().toString());
        return ResponseEntity.ok(response);
    }
}
