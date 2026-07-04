package com.mooc.app.controller;

import com.mooc.app.config.SecurityConfig;
import com.mooc.app.dto.response.*;
import com.mooc.app.service.HybridSearchService;
import com.mooc.app.service.KeywordResult;
import com.mooc.app.service.KeywordSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@Import(SecurityConfig.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HybridSearchService hybridSearchService;
    @MockBean
    private KeywordSearchService keywordSearchService;

    @Test
    void search_validQuery_returns200AndSearchResponse() throws Exception {
        SearchResultItem item = new SearchResultItem("spot", UUID.randomUUID(), "great-wall", "Great Wall", "长城", null, 0.03);
        SearchResponse response = new SearchResponse("req", List.of(item), 1, 0, 0);
        when(hybridSearchService.search(eq("长城"), isNull(), isNull(), anyString())).thenReturn(response);

        mockMvc.perform(get("/api/search").param("q", "长城"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].slug").value("great-wall"))
                .andExpect(jsonPath("$.spots_count").value(1));
    }

    @Test
    void search_missingQ_returns422() throws Exception {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void search_withTypeFilter_passesFilterToService() throws Exception {
        SearchResponse response = new SearchResponse("req", List.of(), 0, 0, 0);
        when(hybridSearchService.search(eq("x"), eq("spot"), isNull(), anyString())).thenReturn(response);

        mockMvc.perform(get("/api/search").param("q", "x").param("type", "spot"))
                .andExpect(status().isOk());

        verify(hybridSearchService).search(eq("x"), eq("spot"), isNull(), anyString());
    }

    @Test
    void suggest_validQuery_returns200AndSuggestResponse() throws Exception {
        KeywordResult kr = new KeywordResult(UUID.randomUUID(), "长城", "great-wall", "spot", 3.0);
        when(keywordSearchService.suggest("长城")).thenReturn(List.of(kr));

        mockMvc.perform(get("/api/search/suggest").param("q", "长城"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].slug").value("great-wall"));
    }

    @Test
    void suggest_emptyQuery_returnsEmptyList() throws Exception {
        when(keywordSearchService.suggest(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/search/suggest").param("q", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }
}
