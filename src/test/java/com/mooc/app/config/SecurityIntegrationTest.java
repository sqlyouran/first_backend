package com.mooc.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.CreatePostRequest;
import com.mooc.app.dto.LoginRequest;
import com.mooc.app.dto.RegisterRequest;
import com.mooc.app.dto.SendCodeRequest;
import com.mooc.app.RateLimitTestHelper;
import com.mooc.app.service.RateLimitService;
import com.mooc.app.service.VerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VerificationCodeStore codeStore;
    @Autowired private RateLimitService rateLimitService;

    @BeforeEach
    void resetRateLimits() {
        RateLimitTestHelper.reset(rateLimitService);
    }

    // === 2.5: Public endpoints accessible without token ===

    @Test
    void publicEndpoint_getPosts_noToken_returns200() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk());
    }

    @Test
    void publicEndpoint_getSpots_noToken_returns200() throws Exception {
        mockMvc.perform(get("/api/spots"))
                .andExpect(status().isOk());
    }

    @Test
    void publicEndpoint_getSearch_noToken_returns200() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "test"))
                .andExpect(status().isOk());
    }

    // === 2.6: Authenticated endpoints return 401 without token ===

    @Test
    void authRequired_createPost_noToken_returns401() throws Exception {
        CreatePostRequest req = new CreatePostRequest(
                "Title", "# Hello", null, List.of(), null);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("unauthorized"));
    }

    @Test
    void authRequired_deletePost_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/posts/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("unauthorized"));
    }

    @Test
    void authRequired_getStaleSpots_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/spots/stale"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("unauthorized"));
    }

    @Test
    void authRequired_triggerEnrichment_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/spots/enrichment/trigger"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("unauthorized"));
    }

    @Test
    void authRequired_knowledgeRebuild_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/ai/knowledge/rebuild"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("unauthorized"));
    }

    @Test
    void authRequired_getNotifications_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    // === 2.7: Valid token can access authenticated endpoints ===

    @Test
    void validToken_createPost_returns201() throws Exception {
        String token = registerAndLogin("user@example.com", "Password1");

        CreatePostRequest req = new CreatePostRequest(
                "Title", "# Hello\nWorld", null, List.of("travel"), null);

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        CreatePostRequest req = new CreatePostRequest(
                "Title", "# Hello", null, List.of(), null);

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // === Helper methods ===

    private String registerAndLogin(String email, String password) throws Exception {
        // Send verification code
        mockMvc.perform(post("/api/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendCodeRequest(email))))
                .andExpect(status().isOk());

        String code = codeStore.getCode(email).orElseThrow();

        // Register
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, code, password))))
                .andExpect(status().isCreated());

        // Login
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
