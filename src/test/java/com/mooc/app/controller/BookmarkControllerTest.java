package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.CreatePostRequest;
import com.mooc.app.dto.LoginRequest;
import com.mooc.app.dto.RegisterRequest;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookmarkControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VerificationCodeStore codeStore;

    private String userToken;
    private String postId;

    @BeforeEach
    void setUp() throws Exception {
        userToken = registerAndLogin("bookmark-user@test.com", "Password123!");
        postId = createPost(userToken);
    }

    // === POST /api/posts/{postId}/bookmark ===

    @Test
    void toggleBookmark_add_returnsBookmarkedTrue() throws Exception {
        mockMvc.perform(post("/api/posts/" + postId + "/bookmark")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookmarked").value(true))
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void toggleBookmark_remove_returnsBookmarkedFalse() throws Exception {
        // Add bookmark
        mockMvc.perform(post("/api/posts/" + postId + "/bookmark")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk());

        // Toggle off
        mockMvc.perform(post("/api/posts/" + postId + "/bookmark")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookmarked").value(false));
    }

    @Test
    void toggleBookmark_postNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/posts/00000000-0000-0000-0000-000000000001/bookmark")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void toggleBookmark_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/posts/" + postId + "/bookmark"))
            .andExpect(status().isUnauthorized());
    }

    // === GET /api/bookmarks ===

    @Test
    void listBookmarks_returnsUserBookmarks() throws Exception {
        // Bookmark a post
        mockMvc.perform(post("/api/posts/" + postId + "/bookmark")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/bookmarks")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].post_id").value(postId))
            .andExpect(jsonPath("$.items[0].post_title").value("Bookmark Test Post"))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void listBookmarks_empty_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/bookmarks")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void listBookmarks_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/bookmarks"))
            .andExpect(status().isUnauthorized());
    }

    // === GET /api/posts/{postId}/bookmark-status ===

    @Test
    void bookmarkStatus_loggedIn_bookmarked_returnsTrue() throws Exception {
        // Bookmark the post first
        mockMvc.perform(post("/api/posts/" + postId + "/bookmark")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/" + postId + "/bookmark-status")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookmarked").value(true))
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void bookmarkStatus_loggedIn_notBookmarked_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/posts/" + postId + "/bookmark-status")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookmarked").value(false));
    }

    @Test
    void bookmarkStatus_noToken_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/posts/" + postId + "/bookmark-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookmarked").value(false));
    }

    @Test
    void bookmarkStatus_postNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/posts/00000000-0000-0000-0000-000000000001/bookmark-status")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
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
        CreatePostRequest req = new CreatePostRequest("Bookmark Test Post", "Content", null, List.of(), null);
        MvcResult result = mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
