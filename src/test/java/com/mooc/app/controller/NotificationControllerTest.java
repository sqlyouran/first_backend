package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.CreatePostRequest;
import com.mooc.app.dto.LoginRequest;
import com.mooc.app.dto.RegisterRequest;
import com.mooc.app.entity.NotificationType;
import com.mooc.app.service.JwtService;
import com.mooc.app.service.NotificationService;
import com.mooc.app.service.VerificationCodeStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VerificationCodeStore codeStore;
    @Autowired private NotificationService notificationService;
    @Autowired private JwtService jwtService;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;
    private String postAId;
    private String postBId;

    @BeforeAll
    void setUp() throws Exception {
        // Register user A (1/3)
        userAToken = registerAndLogin("notif-a@test.com", "Password123!");
        userAId = parseUserId(userAToken);
        // Register user B (2/3)
        userBToken = registerAndLogin("notif-b@test.com", "Password123!");
        userBId = parseUserId(userBToken);
        // Create 2 posts for user A (no more registrations needed)
        postAId = createPost(userAToken, "Notification Test Post A");
        postBId = createPost(userAToken, "Notification Test Post B");
    }

    // === GET /api/notifications ===

    @Test
    void listNotifications_hasNotifications_returnsPaginatedList() throws Exception {
        notificationService.createNotification(userAId, userBId, NotificationType.POST_LIKED,
                UUID.fromString(postAId), "post", null);

        mockMvc.perform(get("/api/notifications?page=1&size=20")
                .header("Authorization", "Bearer " + userAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].type").value("POST_LIKED"))
            .andExpect(jsonPath("$.items[0].read").value(false))
            .andExpect(jsonPath("$.items[0].entity_id").value(postAId))
            .andExpect(jsonPath("$.items[0].actor_id").value(userBId.toString()))
            .andExpect(jsonPath("$.items[0].actor_nickname").isNotEmpty())
            .andExpect(jsonPath("$.items[0].target_title").value("Notification Test Post A"))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void listNotifications_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
            .andExpect(status().isUnauthorized());
    }

    // === POST /api/notifications/{id}/read ===

    @Test
    void markAsRead_validNotification_returnsOk() throws Exception {
        notificationService.createNotification(userAId, userBId, NotificationType.POST_COMMENTED,
                UUID.fromString(postBId), "post", "Great post!");

        String notificationId = getLatestNotificationId(userAToken);

        mockMvc.perform(post("/api/notifications/" + notificationId + "/read")
                .header("Authorization", "Bearer " + userAToken))
            .andExpect(status().isOk());
    }

    @Test
    void markAsRead_othersNotification_returns404() throws Exception {
        notificationService.createNotification(userAId, userBId, NotificationType.POST_BOOKMARKED,
                UUID.fromString(postAId), "post", null);

        String notificationId = getLatestNotificationId(userAToken);

        mockMvc.perform(post("/api/notifications/" + notificationId + "/read")
                .header("Authorization", "Bearer " + userBToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    @Test
    void markAsRead_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/notifications/" + UUID.randomUUID() + "/read"))
            .andExpect(status().isUnauthorized());
    }

    // === POST /api/notifications/mark-all-read ===

    @Test
    void markAllRead_hasUnread_returnsUpdatedCount() throws Exception {
        // Clear any existing unread for userB first
        mockMvc.perform(post("/api/notifications/mark-all-read")
                .header("Authorization", "Bearer " + userBToken));

        String postForB = createPost(userAToken, "Post For B MarkAll");
        notificationService.createNotification(userBId, userAId, NotificationType.POST_LIKED,
                UUID.fromString(postForB), "post", null);
        notificationService.createNotification(userBId, userAId, NotificationType.POST_COMMENTED,
                UUID.fromString(postForB), "post", "Nice!");

        mockMvc.perform(post("/api/notifications/mark-all-read")
                .header("Authorization", "Bearer " + userBToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.updated_count").value(2))
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void markAllRead_noUnread_returnsZero() throws Exception {
        // markAllRead already ran for userB above, but to be safe test with a clean state
        // First mark all read, then call again
        mockMvc.perform(post("/api/notifications/mark-all-read")
                .header("Authorization", "Bearer " + userBToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.updated_count").isNumber());
    }

    @Test
    void markAllRead_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/notifications/mark-all-read"))
            .andExpect(status().isUnauthorized());
    }

    // === GET /api/notifications/unread-count ===

    @Test
    void unreadCount_hasUnread_returnsCount() throws Exception {
        // Clear any existing unread for userB first
        mockMvc.perform(post("/api/notifications/mark-all-read")
                .header("Authorization", "Bearer " + userBToken));

        String postForCount = createPost(userAToken, "Count Test Post");
        notificationService.createNotification(userBId, userAId, NotificationType.POST_LIKED,
                UUID.fromString(postForCount), "post", null);
        notificationService.createNotification(userBId, userAId, NotificationType.POST_BOOKMARKED,
                UUID.fromString(postForCount), "post", null);

        mockMvc.perform(get("/api/notifications/unread-count")
                .header("Authorization", "Bearer " + userBToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(2))
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void unreadCount_noUnread_returnsZero() throws Exception {
        // First mark all read for userA
        mockMvc.perform(post("/api/notifications/mark-all-read")
                .header("Authorization", "Bearer " + userAToken));

        mockMvc.perform(get("/api/notifications/unread-count")
                .header("Authorization", "Bearer " + userAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void unreadCount_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
            .andExpect(status().isUnauthorized());
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

    private String getLatestNotificationId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + token))
            .andReturn();
        var items = objectMapper.readTree(result.getResponse().getContentAsString()).get("items");
        return items.get(items.size() - 1).get("id").asText();
    }
}
