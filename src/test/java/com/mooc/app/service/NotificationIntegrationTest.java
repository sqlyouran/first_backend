package com.mooc.app.service;

import com.mooc.app.entity.NotificationType;
import com.mooc.app.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.*;
import com.mooc.app.entity.EntityType;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VerificationCodeStore codeStore;
    @Autowired private NotificationRepository notificationRepository;
    @MockBean private RateLimitService rateLimitService;
    @Autowired private JwtService jwtService;

    private String authorToken;
    private UUID authorId;
    private String otherToken;
    private UUID otherId;
    private String postId;

    @BeforeAll
    void setUp() throws Exception {
        when(rateLimitService.isRegisterRateLimited(anyString())).thenReturn(false);
        when(rateLimitService.isLoginRateLimited(anyString())).thenReturn(false);
        when(rateLimitService.isSendCodeIpRateLimited(anyString())).thenReturn(false);
        when(rateLimitService.isSendCodeEmailRateLimited(anyString())).thenReturn(false);
        authorToken = registerAndLogin("integ-author@test.com", "Password123!");
        authorId = parseUserId(authorToken);
        otherToken = registerAndLogin("integ-other@test.com", "Password123!");
        otherId = parseUserId(otherToken);
        postId = createPost(authorToken, "Integration Test Post");
    }

    // === 4.1: VoteService notification integration ===

    @Test
    void vote_up_createsPostLikedNotification() throws Exception {
        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VoteRequest("up"))))
            .andReturn();

        var notifications = notificationRepository
                .findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                        authorId, otherId, NotificationType.POST_LIKED, UUID.fromString(postId));
        assertTrue(notifications.isPresent());
    }

    @Test
    void vote_toggleOff_deletesPostLikedNotification() throws Exception {
        // Vote up first
        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VoteRequest("up"))));

        // Toggle off
        mockMvc.perform(post("/api/posts/" + postId + "/vote")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VoteRequest("up"))));

        var notifications = notificationRepository
                .findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                        authorId, otherId, NotificationType.POST_LIKED, UUID.fromString(postId));
        assertTrue(notifications.isEmpty());
    }

    @Test
    void vote_selfVote_doesNotCreateNotification() throws Exception {
        String selfPostId = createPost(authorToken, "Self Vote Post");

        mockMvc.perform(post("/api/posts/" + selfPostId + "/vote")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VoteRequest("up"))));

        var notifications = notificationRepository
                .findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                        authorId, authorId, NotificationType.POST_LIKED, UUID.fromString(selfPostId));
        assertTrue(notifications.isEmpty());
    }

    // === 4.3: CommentService notification integration ===

    @Test
    void comment_onPost_createsPostCommentedNotification() throws Exception {
        String commentPostId = createPost(authorToken, "Comment Test Post");

        mockMvc.perform(post("/api/posts/" + commentPostId + "/comments")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCommentRequest("Great post!", null))));

        var notifications = notificationRepository
                .findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                        authorId, otherId, NotificationType.POST_COMMENTED, UUID.fromString(commentPostId));
        assertTrue(notifications.isPresent());
        assertEquals("Great post!", notifications.get().getContentPreview());
    }

    @Test
    void reply_toComment_createsCommentRepliedNotification() throws Exception {
        String replyPostId = createPost(authorToken, "Reply Test Post");

        // Author creates a comment
        MvcResult commentResult = mockMvc.perform(post("/api/posts/" + replyPostId + "/comments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCommentRequest("My comment", null))))
            .andReturn();
        String commentId = objectMapper.readTree(commentResult.getResponse().getContentAsString()).get("id").asText();

        // Other user replies
        mockMvc.perform(post("/api/posts/" + replyPostId + "/comments")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CreateCommentRequest("My reply to you", UUID.fromString(commentId)))));

        var notifications = notificationRepository
                .findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                        authorId, otherId, NotificationType.COMMENT_REPLIED, UUID.fromString(replyPostId));
        assertTrue(notifications.isPresent());
        assertEquals("My reply to you", notifications.get().getContentPreview());
    }

    @Test
    void comment_selfComment_doesNotCreateNotification() throws Exception {
        String selfPostId = createPost(authorToken, "Self Comment Post");

        mockMvc.perform(post("/api/posts/" + selfPostId + "/comments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCommentRequest("Self comment", null))));

        var notifications = notificationRepository
                .findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                        authorId, authorId, NotificationType.POST_COMMENTED, UUID.fromString(selfPostId));
        assertTrue(notifications.isEmpty());
    }

    // === 4.5: BookmarkService notification integration ===

    @Test
    void bookmark_createsPostBookmarkedNotification() throws Exception {
        mockMvc.perform(post("/api/posts/" + postId + "/bookmark")
                .header("Authorization", "Bearer " + otherToken));

        var notifications = notificationRepository
                .findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                        authorId, otherId, NotificationType.POST_BOOKMARKED, UUID.fromString(postId));
        assertTrue(notifications.isPresent());
    }

    @Test
    void bookmark_toggleOff_deletesPostBookmarkedNotification() throws Exception {
        String bmPostId = createPost(authorToken, "Bookmark Toggle Post");

        // Bookmark
        mockMvc.perform(post("/api/posts/" + bmPostId + "/bookmark")
                .header("Authorization", "Bearer " + otherToken));

        // Toggle off
        mockMvc.perform(post("/api/posts/" + bmPostId + "/bookmark")
                .header("Authorization", "Bearer " + otherToken));

        var notifications = notificationRepository
                .findByRecipientIdAndActorIdAndTypeAndEntityIdAndDeletedFalse(
                        authorId, otherId, NotificationType.POST_BOOKMARKED, UUID.fromString(bmPostId));
        assertTrue(notifications.isEmpty());
    }

    // === Helpers ===

    private String registerAndLogin(String email, String password) throws Exception {
        codeStore.save(email.toLowerCase(), "123456", 600);
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .header("X-Forwarded-For", "10.0.0." + (int)(Math.random() * 254 + 1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "123456", password))))
            .andReturn();
        if (registerResult.getResponse().getStatus() >= 400) {
            throw new RuntimeException("Register failed: " + registerResult.getResponse().getContentAsString());
        }

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", "10.0.0." + (int)(Math.random() * 254 + 1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andReturn();
        String body = result.getResponse().getContentAsString();
        var tokenNode = objectMapper.readTree(body).get("access_token");
        if (tokenNode == null) {
            throw new RuntimeException("Login response missing access_token: " + body);
        }
        return tokenNode.asText();
    }

    private UUID parseUserId(String token) {
        return jwtService.parseToken(token)
                .map(c -> UUID.fromString(c.getSubject()))
                .orElseThrow();
    }

    private String createPost(String token, String title) throws Exception {
        CreatePostRequest req = new CreatePostRequest(title, "Content", null, List.of(), null);
        MvcResult result = mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
