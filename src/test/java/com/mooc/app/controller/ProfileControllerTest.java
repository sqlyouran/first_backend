package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.LoginRequest;
import com.mooc.app.dto.RegisterRequest;
import com.mooc.app.dto.UpdateProfileRequest;
import com.mooc.app.service.VerificationCodeStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VerificationCodeStore codeStore;

    // === GET /api/users/me/profile ===

    @Test
    void getMyProfile_authenticated_returns200WithAllFields() throws Exception {
        registerUser("profile@example.com", "Password1");
        String token = loginAndGetAccessToken("profile@example.com", "Password1");

        mockMvc.perform(get("/api/users/me/profile")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.email").value("profile@example.com"))
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(jsonPath("$.created_at").isNotEmpty())
            .andExpect(jsonPath("$.interest_tags").isArray());
    }

    @Test
    void getMyProfile_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me/profile"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error_code").value("unauthorized"));
    }

    // === PUT /api/users/me/profile ===

    @Test
    void updateProfile_firstSetUsername_success() throws Exception {
        registerUser("update@example.com", "Password1");
        String token = loginAndGetAccessToken("update@example.com", "Password1");

        UpdateProfileRequest request = new UpdateProfileRequest(
                "testuser1", "TestNick", "https://example.com/avatar.png",
                "Hello world", List.of("history", "hiking"));

        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser1"))
            .andExpect(jsonPath("$.nickname").value("TestNick"))
            .andExpect(jsonPath("$.avatar_url").value("https://example.com/avatar.png"))
            .andExpect(jsonPath("$.bio").value("Hello world"))
            .andExpect(jsonPath("$.interest_tags", hasSize(2)))
            .andExpect(jsonPath("$.interest_tags[0]").value("history"))
            .andExpect(jsonPath("$.interest_tags[1]").value("hiking"));
    }

    @Test
    void updateProfile_usernameTaken_returns409() throws Exception {
        // Register first user and set username
        registerUser("user1@example.com", "Password1");
        String token1 = loginAndGetAccessToken("user1@example.com", "Password1");
        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        "taken_name", null, null, null, null))));

        // Register second user and try same username
        registerUser("user2@example.com", "Password1");
        String token2 = loginAndGetAccessToken("user2@example.com", "Password1");
        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        "taken_name", null, null, null, null))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("username_taken"));
    }

    @Test
    void updateProfile_invalidUsernameFormat_returns422() throws Exception {
        registerUser("invalid@example.com", "Password1");
        String token = loginAndGetAccessToken("invalid@example.com", "Password1");

        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        "AB", null, null, null, null))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void updateProfile_nicknameTooShort_returns422() throws Exception {
        registerUser("shortnick@example.com", "Password1");
        String token = loginAndGetAccessToken("shortnick@example.com", "Password1");

        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        null, "A", null, null, null))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void updateProfile_nicknameWithSpecialChars_returns422() throws Exception {
        registerUser("specialchars@example.com", "Password1");
        String token = loginAndGetAccessToken("specialchars@example.com", "Password1");

        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        null, "Bad<Nick>", null, null, null))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void updateProfile_nicknameTooLong_returns422() throws Exception {
        registerUser("longnick@example.com", "Password1");
        String token = loginAndGetAccessToken("longnick@example.com", "Password1");

        String longNick = "a".repeat(31);
        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        null, longNick, null, null, null))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void updateProfile_bioTooLong_returns422() throws Exception {
        registerUser("longbio@example.com", "Password1");
        String token = loginAndGetAccessToken("longbio@example.com", "Password1");

        String longBio = "a".repeat(501);
        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        null, null, null, longBio, null))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void updateProfile_invalidTags_returns422() throws Exception {
        registerUser("badtag@example.com", "Password1");
        String token = loginAndGetAccessToken("badtag@example.com", "Password1");

        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        null, null, null, null, List.of("nonexistent_tag")))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void updateProfile_tooManyTags_returns422() throws Exception {
        registerUser("manytags@example.com", "Password1");
        String token = loginAndGetAccessToken("manytags@example.com", "Password1");

        List<String> elevenTags = List.of(
                "history", "art_museums", "folk_customs", "architecture", "religious_sites",
                "mountains", "lakes_rivers", "deserts", "coastal", "national_parks", "street_food");

        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        null, null, null, null, elevenTags))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void updateProfile_optionalFieldsCanBeCleared() throws Exception {
        registerUser("clear@example.com", "Password1");
        String token = loginAndGetAccessToken("clear@example.com", "Password1");

        // First set some values
        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        "clearuser", "Nick", null, "Bio text", List.of("history")))));

        // Now clear nickname, bio, and interest_tags
        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        null, "", null, "", List.of()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nickname").isEmpty())
            .andExpect(jsonPath("$.bio").isEmpty())
            .andExpect(jsonPath("$.interest_tags").isEmpty());
    }

    @Test
    void updateProfile_modifyExistingUsernameIgnored() throws Exception {
        registerUser("ignore@example.com", "Password1");
        String token = loginAndGetAccessToken("ignore@example.com", "Password1");

        // First set username
        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        "original_name", null, null, null, null))))
            .andExpect(jsonPath("$.username").value("original_name"));

        // Try to change username - should be silently ignored
        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        "new_name_123", null, null, null, null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("original_name"));
    }

    // === GET /api/users/{username} ===

    @Test
    void getPublicProfile_exists_returns200() throws Exception {
        registerUser("public@example.com", "Password1");
        String token = loginAndGetAccessToken("public@example.com", "Password1");

        // Set username and profile
        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        "public_user", "PublicNick", "https://example.com/pic.jpg",
                        "My bio", List.of("photography")))));

        // Access public profile (no auth needed)
        mockMvc.perform(get("/api/users/public_user"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("public_user"))
            .andExpect(jsonPath("$.nickname").value("PublicNick"))
            .andExpect(jsonPath("$.avatar_url").value("https://example.com/pic.jpg"))
            .andExpect(jsonPath("$.bio").value("My bio"))
            .andExpect(jsonPath("$.interest_tags[0]").value("photography"))
            .andExpect(jsonPath("$.email").doesNotExist())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.created_at").isNotEmpty());
    }

    @Test
    void getPublicProfile_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/users/nonexistent_user"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void getPublicProfile_deletedUser_returns404() throws Exception {
        registerUser("deleted@example.com", "Password1");
        String token = loginAndGetAccessToken("deleted@example.com", "Password1");

        // Set username
        mockMvc.perform(put("/api/users/me/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest(
                        "deleted_user", null, null, null, null))));

        // Delete the user
        mockMvc.perform(delete("/api/auth/me")
                .header("Authorization", "Bearer " + token));

        // Try to access public profile
        mockMvc.perform(get("/api/users/deleted_user"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    // === GET /api/users/interest-tags ===

    @Test
    void getInterestTags_returns200With24Tags() throws Exception {
        mockMvc.perform(get("/api/users/interest-tags"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tags", hasSize(24)))
            .andExpect(jsonPath("$.tags[0].value").isNotEmpty())
            .andExpect(jsonPath("$.tags[0].label").isNotEmpty())
            .andExpect(jsonPath("$.tags[0].category").isNotEmpty())
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    // === Helper methods ===

    private void registerUser(String email, String password) throws Exception {
        codeStore.save(email.toLowerCase(), "123456", 600);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest(email, "123456", password))));
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andReturn();
        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("access_token").asText();
    }
}
