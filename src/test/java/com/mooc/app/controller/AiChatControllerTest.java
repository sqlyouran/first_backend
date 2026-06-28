package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.config.SecurityConfig;
import com.mooc.app.dto.AiChatRequest;
import com.mooc.app.dto.response.AiConversationResponse;
import com.mooc.app.exception.AiChatException;
import com.mooc.app.service.AiChatService;
import com.mooc.app.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiChatController.class)
@Import(SecurityConfig.class)
class AiChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AiChatService aiChatService;
    @MockBean private JwtService jwtService;

    @Test
    void createConversation_returns201() throws Exception {
        UUID convId = UUID.randomUUID();
        AiConversationResponse response = new AiConversationResponse("req-1", convId.toString(), "2026-01-01T00:00:00Z");
        when(aiChatService.createConversation(isNull(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/ai/conversations")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(jsonPath("$.id").value(convId.toString()))
            .andExpect(jsonPath("$.created_at").isNotEmpty());
    }

    @Test
    void chat_returns200Sse() throws Exception {
        UUID convId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();
        emitter.complete();
        when(aiChatService.sendMessage(eq(convId), eq("hello"))).thenReturn(emitter);

        MvcResult result = mockMvc.perform(post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new AiChatRequest(convId, "hello"))))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(result))
            .andExpect(status().isOk());
    }

    @Test
    void chat_conversationNotFound_returns404() throws Exception {
        UUID convId = UUID.randomUUID();
        when(aiChatService.sendMessage(eq(convId), anyString()))
                .thenThrow(new AiChatException(HttpStatus.NOT_FOUND, "not_found", "Conversation not found"));

        mockMvc.perform(post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new AiChatRequest(convId, "hello"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void chat_blankMessage_returns422() throws Exception {
        UUID convId = UUID.randomUUID();
        when(aiChatService.sendMessage(any(), any()))
                .thenReturn(new SseEmitter());

        mockMvc.perform(post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("conversation_id", convId.toString(), "message", ""))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void chat_nullConversationId_returns422() throws Exception {
        when(aiChatService.sendMessage(any(), any()))
                .thenReturn(new SseEmitter());

        mockMvc.perform(post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("message", "hello"))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }
}
