package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.CreateCommentRequest;
import com.mooc.app.dto.LoginRequest;
import com.mooc.app.dto.RegisterRequest;
import com.mooc.app.dto.CreatePostRequest;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VerificationCodeStore codeStore;

    private String userToken;
    private String otherToken;
    private String postId;

    @BeforeEach
    void setUp() throws Exception {
        userToken = registerAndLogin("comment-user@test.com", "Password123!");
        otherToken = registerAndLogin("comment-other@test.com", "Password123!");
        postId = createPost(userToken);
    }

    // === POST /api/posts/{postId}/comments ===

    @Test
    void createComment_topLevel_returns201() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("Great post!", null);

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.post_id").value(postId))
            .andExpect(jsonPath("$.content").value("Great post!"))
            .andExpect(jsonPath("$.parent_comment_id").isEmpty())
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void createComment_reply_returns201() throws Exception {
        String commentId = createComment(userToken, postId, "Parent comment", null);

        CreateCommentRequest req = new CreateCommentRequest("Reply!", UUID.fromString(commentId));

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.parent_comment_id").value(commentId));
    }

    @Test
    void createComment_postNotFound_returns404() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("Hello", null);

        mockMvc.perform(post("/api/posts/00000000-0000-0000-0000-000000000001/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void createComment_parentNotFound_returns404() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("Reply", UUID.fromString("00000000-0000-0000-0000-000000000001"));

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void createComment_noToken_returns401() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("Hello", null);

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    // === GET /api/posts/{postId}/comments ===

    @Test
    void listComments_returnsTopLevel() throws Exception {
        createComment(userToken, postId, "Comment 1", null);
        createComment(userToken, postId, "Comment 2", null);

        mockMvc.perform(get("/api/posts/" + postId + "/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void listComments_deletedShowsPlaceholder() throws Exception {
        String commentId = createComment(userToken, postId, "To be deleted", null);

        mockMvc.perform(delete("/api/posts/" + postId + "/comments/" + commentId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/" + postId + "/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].content").value("[已删除]"))
            .andExpect(jsonPath("$.items[0].deleted").value(true));
    }

    @Test
    void listComments_postNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/posts/00000000-0000-0000-0000-000000000001/comments"))
            .andExpect(status().isNotFound());
    }

    // === GET /api/comments/{commentId}/replies ===

    @Test
    void listReplies_returnsReplies() throws Exception {
        String parentId = createComment(userToken, postId, "Parent", null);
        createComment(userToken, postId, "Reply 1", parentId);
        createComment(userToken, postId, "Reply 2", parentId);

        mockMvc.perform(get("/api/comments/" + parentId + "/replies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.total").value(2));
    }

    // === DELETE /api/posts/{postId}/comments/{commentId} ===

    @Test
    void deleteComment_author_returns204() throws Exception {
        String commentId = createComment(userToken, postId, "Delete me", null);

        mockMvc.perform(delete("/api/posts/" + postId + "/comments/" + commentId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteComment_notAuthor_returns403() throws Exception {
        String commentId = createComment(userToken, postId, "Not yours", null);

        mockMvc.perform(delete("/api/posts/" + postId + "/comments/" + commentId)
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error_code").value("access_denied"));
    }

    @Test
    void deleteComment_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/posts/" + postId + "/comments/00000000-0000-0000-0000-000000000001")
                .header("Authorization", "Bearer " + userToken))
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
        CreatePostRequest req = new CreatePostRequest("Test Post", "Content body", null, List.of(), null);
        MvcResult result = mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createComment(String token, String postId, String content, String parentId) throws Exception {
        CreateCommentRequest req = new CreateCommentRequest(content, parentId != null ? UUID.fromString(parentId) : null);
        MvcResult result = mockMvc.perform(post("/api/posts/" + postId + "/comments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
