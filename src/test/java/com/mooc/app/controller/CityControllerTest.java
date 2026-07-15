package com.mooc.app.controller;

import com.mooc.app.config.SecurityConfig;
import com.mooc.app.dto.response.CityListResponse;
import com.mooc.app.dto.response.CityResponse;
import com.mooc.app.exception.CityException;
import com.mooc.app.service.CityService;
import com.mooc.app.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CityController.class)
@Import(SecurityConfig.class)
class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CityService cityService;

    @Test
    void listCities_defaultParams_delegatesToService() throws Exception {
        CityResponse city = new CityResponse("req", "id", "Beijing", "北京", "beijing",
                "https://example.com/img.jpg", "Description", "Autumn", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z");
        CityListResponse listResponse = new CityListResponse("req", List.of(city), 1, 1, 20);
        when(cityService.listCities(eq(1), eq(20), anyString())).thenReturn(listResponse);

        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20));

        verify(cityService).listCities(eq(1), eq(20), anyString());
    }

    @Test
    void listCities_explicitParams_passesThrough() throws Exception {
        CityListResponse listResponse = new CityListResponse("req", List.of(), 0, 2, 3);
        when(cityService.listCities(eq(2), eq(3), anyString())).thenReturn(listResponse);

        mockMvc.perform(get("/api/cities?page=2&size=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(3));

        verify(cityService).listCities(eq(2), eq(3), anyString());
    }

    @Test
    void getCity_exists_returnsCity() throws Exception {
        UUID id = UUID.randomUUID();
        CityResponse city = new CityResponse("req", id.toString(), "Beijing", "北京", "beijing",
                "https://example.com/img.jpg", "Description", "Autumn", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z");
        when(cityService.getCity(eq(id), anyString())).thenReturn(city);

        mockMvc.perform(get("/api/cities/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Beijing"))
                .andExpect(jsonPath("$.slug").value("beijing"));
    }

    @Test
    void getCity_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(cityService.getCity(eq(id), anyString()))
                .thenThrow(new CityException(HttpStatus.NOT_FOUND, "not_found", "City not found"));

        mockMvc.perform(get("/api/cities/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void listCities_noAuthRequired() throws Exception {
        CityListResponse listResponse = new CityListResponse("req", List.of(), 0, 1, 20);
        when(cityService.listCities(eq(1), eq(20), anyString())).thenReturn(listResponse);

        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk());
    }
}
