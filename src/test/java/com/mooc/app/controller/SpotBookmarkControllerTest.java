package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class SpotBookmarkControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VerificationCodeStore codeStore;
    @Autowired private SpotRepository spotRepository;

    private String userToken;
    private String spotId;

    @BeforeEach
    void setUp() throws Exception {
        userToken = registerAndLogin("spot-bookmark-user@test.com", "Password123!");
        spotId = createSpot();
    }

    @Test
    void toggleSpotBookmark_add_returnsBookmarkedTrue() throws Exception {
        mockMvc.perform(post("/api/spots/" + spotId + "/bookmark")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookmarked").value(true))
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void toggleSpotBookmark_remove_returnsBookmarkedFalse() throws Exception {
        mockMvc.perform(post("/api/spots/" + spotId + "/bookmark")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/spots/" + spotId + "/bookmark")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookmarked").value(false));
    }

    @Test
    void toggleSpotBookmark_spotNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/spots/00000000-0000-0000-0000-000000000001/bookmark")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void toggleSpotBookmark_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/spots/" + spotId + "/bookmark"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void spotBookmarkStatus_bookmarked_returnsTrue() throws Exception {
        mockMvc.perform(post("/api/spots/" + spotId + "/bookmark")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/spots/" + spotId + "/bookmark-status")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookmarked").value(true));
    }

    @Test
    void spotBookmarkStatus_notBookmarked_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/spots/" + spotId + "/bookmark-status")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookmarked").value(false));
    }

    @Test
    void spotBookmarkStatus_noToken_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/spots/" + spotId + "/bookmark-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookmarked").value(false));
    }

    @Test
    void spotBookmarkStatus_spotNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/spots/00000000-0000-0000-0000-000000000001/bookmark-status")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    private String createSpot() {
        SpotEntity spot = new SpotEntity();
        spot.setName("Test Spot");
        spot.setSlug("test-spot-bookmark");
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
