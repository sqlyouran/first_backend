package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.CreatePostRequest;
import com.mooc.app.dto.LoginRequest;
import com.mooc.app.dto.RegisterRequest;
import com.mooc.app.dto.VoteRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VoteControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VerificationCodeStore codeStore;
    @Autowired private RateLimitService rateLimitService;

    private String userToken;
    private String postId;

    @BeforeEach
    void setUp() throws Exception {
        RateLimitTestHelper.reset(rateLimitService);
        userToken = registerAndLogin("vote-user@test.com", "Password123!");
        postId = createPost(userToken);
    }

    // === POST /api/posts/{postId}/vote ===

    @Test
    void vote_upvote_returnsOk() throws Exception {
        VoteRequest req = new VoteRequest("up");

        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vote_type").value("up"))
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void vote_toggle_removesVote() throws Exception {
        // First vote up
        VoteRequest req = new VoteRequest("up");
        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());

        // Vote up again → toggle off
        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vote_type").doesNotExist());
    }

    @Test
    void vote_switchType_updatesVote() throws Exception {
        // Vote up
        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VoteRequest("up"))))
            .andExpect(status().isOk());

        // Switch to down
        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VoteRequest("down"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vote_type").value("down"));
    }

    @Test
    void vote_invalidType_returns422() throws Exception {
        VoteRequest req = new VoteRequest("invalid");

        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void vote_noToken_returns401() throws Exception {
        VoteRequest req = new VoteRequest("up");

        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void vote_postNotFound_returns404() throws Exception {
        VoteRequest req = new VoteRequest("up");

        mockMvc.perform(post("/api/posts/00000000-0000-0000-0000-000000000001/vote")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNotFound());
    }

    // === DELETE /api/posts/{postId}/vote ===

    @Test
    void removeVote_existingVote_returns204() throws Exception {
        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VoteRequest("up"))))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void removeVote_noExistingVote_returns204() throws Exception {
        mockMvc.perform(delete("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isNoContent());
    }

    // === GET /api/posts/{postId}/vote-stats ===

    @Test
    void voteStats_withAuth_returnsStatsAndUserVote() throws Exception {
        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VoteRequest("up"))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/" + postId + "/vote-stats")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.up_count").value(1))
            .andExpect(jsonPath("$.down_count").value(0))
            .andExpect(jsonPath("$.user_vote").value("up"));
    }

    @Test
    void voteStats_noAuth_returnsStatsWithoutUserVote() throws Exception {
        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VoteRequest("up"))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/" + postId + "/vote-stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.up_count").value(1))
            .andExpect(jsonPath("$.down_count").value(0))
            .andExpect(jsonPath("$.user_vote").doesNotExist());
    }

    @Test
    void voteStats_postNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/posts/00000000-0000-0000-0000-000000000001/vote-stats"))
            .andExpect(status().isNotFound());
    }

    // === Helpers ===

    private String registerAndLogin(String email, String password) throws Exception {
        codeStore.save(email.toLowerCase(), "123456", 600);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "123456", password))));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }

    private String createPost(String token) throws Exception {
        CreatePostRequest req = new CreatePostRequest("Vote Test Post", "Content", null, List.of(), null);
        MvcResult result = mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
