package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.LoginRequest;
import com.mooc.app.dto.RegisterRequest;
import com.mooc.app.dto.SendCodeRequest;
import com.mooc.app.service.KnowledgeBuilderService;
import com.mooc.app.service.VerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KnowledgeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VerificationCodeStore codeStore;
    @MockBean private KnowledgeBuilderService knowledgeBuilderService;

    private static final AtomicInteger counter = new AtomicInteger(0);
    private String authToken;

    @BeforeEach
    void setup() throws Exception {
        String email = "admin" + counter.incrementAndGet() + "@example.com";
        authToken = registerAndLogin(email, "Password1");
    }

    @Test
    void rebuild_returns200WithRebuildStartedStatus() throws Exception {
        mockMvc.perform(post("/api/ai/knowledge/rebuild")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rebuild_started"))
                .andExpect(jsonPath("$.request_id").exists());
    }

    @Test
    void rebuild_triggersAsyncRebuild() throws Exception {
        mockMvc.perform(post("/api/ai/knowledge/rebuild")
                        .header("Authorization", "Bearer " + authToken));

        verify(knowledgeBuilderService, atLeast(1)).rebuildAllAsync();
    }

    @Test
    void rebuild_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/ai/knowledge/rebuild"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndLogin(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendCodeRequest(email))))
                .andExpect(status().isOk());

        String code = codeStore.getCode(email).orElseThrow();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, code, password))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("access_token").asText();
    }
}
