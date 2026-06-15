package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.CreateCommentRequest;
import com.mooc.app.dto.LoginRequest;
import com.mooc.app.dto.RegisterRequest;
import com.mooc.app.entity.SpotEntity;
import com.mooc.app.entity.SpotStatus;
import com.mooc.app.repository.SpotRepository;
import com.mooc.app.service.VerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SpotCommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VerificationCodeStore codeStore;
    @Autowired private SpotRepository spotRepository;

    private String userToken;
    private String otherToken;
    private String spotId;

    @BeforeEach
    void setUp() throws Exception {
        userToken = registerAndLogin("spot-comment-user@test.com", "Password123!");
        otherToken = registerAndLogin("spot-comment-other@test.com", "Password123!");
        spotId = createSpot();
    }

    @Test
    void createSpotComment_topLevel_returns201() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("Great spot!", null);

        mockMvc.perform(post("/api/spots/" + spotId + "/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.entity_id").value(spotId))
            .andExpect(jsonPath("$.entity_type").value("spot"))
            .andExpect(jsonPath("$.content").value("Great spot!"))
            .andExpect(jsonPath("$.parent_comment_id").isEmpty())
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void createSpotComment_reply_returns201() throws Exception {
        String commentId = createComment(userToken, "Parent comment", null);

        CreateCommentRequest req = new CreateCommentRequest("Reply!", UUID.fromString(commentId));

        mockMvc.perform(post("/api/spots/" + spotId + "/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.parent_comment_id").value(commentId));
    }

    @Test
    void createSpotComment_spotNotFound_returns404() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("Hello", null);

        mockMvc.perform(post("/api/spots/00000000-0000-0000-0000-000000000001/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void createSpotComment_noToken_returns401() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("Hello", null);

        mockMvc.perform(post("/api/spots/" + spotId + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listSpotComments_returnsTopLevel() throws Exception {
        createComment(userToken, "Comment 1", null);
        createComment(userToken, "Comment 2", null);

        mockMvc.perform(get("/api/spots/" + spotId + "/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void listSpotComments_spotNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/spots/00000000-0000-0000-0000-000000000001/comments"))
            .andExpect(status().isNotFound());
    }

    private String createComment(String token, String content, String parentId) throws Exception {
        CreateCommentRequest req = new CreateCommentRequest(content, parentId != null ? UUID.fromString(parentId) : null);
        MvcResult result = mockMvc.perform(post("/api/spots/" + spotId + "/comments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createSpot() {
        SpotEntity spot = new SpotEntity();
        spot.setName("Test Spot");
        spot.setSlug("test-spot-comment");
        spot.setDescription("Test description");
        spot.setCityId(UUID.fromString("a1111111-1111-1111-1111-111111111111"));
        spot.setCityName("Test City");
        spot.setStatus(SpotStatus.PUBLISHED);
        return spotRepository.save(spot).getId().toString();
    }

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
}
