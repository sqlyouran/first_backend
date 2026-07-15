package com.mooc.app.controller;

import com.mooc.app.config.SecurityConfig;
import com.mooc.app.dto.response.ExchangeRateResponse;
import com.mooc.app.dto.response.WeatherResponse;
import com.mooc.app.exception.ServiceException;
import com.mooc.app.service.ExchangeRateData;
import com.mooc.app.service.ExchangeRateService;
import com.mooc.app.service.JwtService;
import com.mooc.app.service.WeatherData;
import com.mooc.app.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceController.class)
@Import(SecurityConfig.class)
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private WeatherService weatherService;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Test
    void getWeather_validCity_returns200() throws Exception {
        WeatherData data = new WeatherData("Beijing", 25.5, "clear sky", "01d", 60, 3.5, Instant.parse("2026-06-21T10:00:00Z"));
        when(weatherService.getWeather("Beijing")).thenReturn(data);

        mockMvc.perform(get("/api/services/weather").param("city", "Beijing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Beijing"))
                .andExpect(jsonPath("$.temperature").value(25.5))
                .andExpect(jsonPath("$.description").value("clear sky"));
    }

    @Test
    void getWeather_missingCityParam_returns422() throws Exception {
        mockMvc.perform(get("/api/services/weather"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getWeather_cityNotFound_returns404() throws Exception {
        when(weatherService.getWeather("FakeCity"))
                .thenThrow(new ServiceException(HttpStatus.NOT_FOUND, "city_not_found", "City not found"));

        mockMvc.perform(get("/api/services/weather").param("city", "FakeCity"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("city_not_found"));
    }

    @Test
    void getExchangeRate_validBase_returns200() throws Exception {
        ExchangeRateData data = new ExchangeRateData("USD",
                Map.of("CNY", new BigDecimal("7.24"), "EUR", new BigDecimal("0.92")),
                Instant.parse("2026-06-21T10:00:00Z"));
        when(exchangeRateService.getRates("USD")).thenReturn(data);

        mockMvc.perform(get("/api/services/exchange-rate").param("base", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("USD"))
                .andExpect(jsonPath("$.rates.CNY").exists());
    }

    @Test
    void getExchangeRate_defaultBase_usesUSD() throws Exception {
        ExchangeRateData data = new ExchangeRateData("USD",
                Map.of("CNY", new BigDecimal("7.24")),
                Instant.parse("2026-06-21T10:00:00Z"));
        when(exchangeRateService.getRates("USD")).thenReturn(data);

        mockMvc.perform(get("/api/services/exchange-rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("USD"));

        verify(exchangeRateService).getRates("USD");
    }

    @Test
    void getExchangeRate_invalidCurrency_returns422() throws Exception {
        when(exchangeRateService.getRates("INVALID"))
                .thenThrow(new ServiceException(HttpStatus.UNPROCESSABLE_ENTITY, "invalid_currency", "Invalid currency code: INVALID"));

        mockMvc.perform(get("/api/services/exchange-rate").param("base", "INVALID"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error_code").value("invalid_currency"));
    }
}
