package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.config.SecurityConfig;
import com.mooc.app.dto.AiPostAssistRequest;
import com.mooc.app.dto.response.AiPostAssistResponse;
import com.mooc.app.exception.AiPostAssistException;
import com.mooc.app.service.AiPostAssistService;
import com.mooc.app.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiPostAssistController.class)
@Import(SecurityConfig.class)
class AiPostAssistControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AiPostAssistService aiPostAssistService;
    @MockBean private JwtService jwtService;

    private static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";
    private static final String TEST_TOKEN = "Bearer test-token";

    private void stubAuth() {
        Claims claims = new DefaultClaims(Map.of("sub", TEST_USER_ID));
        when(jwtService.parseToken("test-token")).thenReturn(Optional.of(claims));
    }

    @Test
    void assist_generateTitle_success() throws Exception {
        stubAuth();
        AiPostAssistResponse response = new AiPostAssistResponse("req-1", "Great Title");
        when(aiPostAssistService.assist(any(AiPostAssistRequest.class), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/ai/post-assist")
                .header("Authorization", TEST_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("action", "generate_title", "content", "Some content"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request_id").value("req-1"))
            .andExpect(jsonPath("$.result").value("Great Title"));
    }

    @Test
    void assist_polish_success() throws Exception {
        stubAuth();
        AiPostAssistResponse response = new AiPostAssistResponse("req-2", "Polished content");
        when(aiPostAssistService.assist(any(AiPostAssistRequest.class), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/ai/post-assist")
                .header("Authorization", TEST_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("action", "polish", "content", "Rough content"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("Polished content"));
    }

    @Test
    void assist_blankAction_returns422() throws Exception {
        mockMvc.perform(post("/api/ai/post-assist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("action", "", "content", "content"))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void assist_invalidAction_returns422() throws Exception {
        mockMvc.perform(post("/api/ai/post-assist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("action", "invalid_action", "content", "content"))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void assist_blankContent_returns422() throws Exception {
        mockMvc.perform(post("/api/ai/post-assist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("action", "generate_title", "content", ""))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void assist_serviceException_returns500() throws Exception {
        stubAuth();
        when(aiPostAssistService.assist(any(AiPostAssistRequest.class), anyString()))
                .thenThrow(new AiPostAssistException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "ai_generation_failed", "AI returned empty result"));

        mockMvc.perform(post("/api/ai/post-assist")
                .header("Authorization", TEST_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("action", "generate_title", "content", "content"))))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error_code").value("ai_generation_failed"));
    }
}
