package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.LoginRequest;
import com.mooc.app.dto.RegisterRequest;
import com.mooc.app.entity.UserEntity;
import com.mooc.app.repository.UserRepository;
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

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConversationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VerificationCodeStore codeStore;
    @Autowired private RateLimitService rateLimitService;
    @Autowired private UserRepository userRepository;

    private String aliceToken;
    private String bobToken;
    private UUID aliceId;
    private UUID bobId;

    @BeforeEach
    void setup() throws Exception {
        RateLimitTestHelper.reset(rateLimitService);
        aliceToken = registerAndLogin("alice@example.com", "Password1");
        bobToken = registerAndLogin("bob@example.com", "Password1");
        aliceId = extractUserId(aliceToken);
        bobId = extractUserId(bobToken);
        setUsername(aliceId, "alice");
        setUsername(bobId, "bob");
    }

    // ===== POST /api/conversations =====

    @Test
    void createConversation_new_returns201() throws Exception {
        mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("recipient_username", "bob", "content", "Hi Bob!"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(jsonPath("$.conversation_id").isNotEmpty())
            .andExpect(jsonPath("$.message_id").isNotEmpty());
    }

    @Test
    void createConversation_existingConversation_reusesIt() throws Exception {
        String body = objectMapper.writeValueAsString(
            Map.of("recipient_username", "bob", "content", "First"));
        MvcResult r1 = mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn();
        String convId1 = objectMapper.readTree(r1.getResponse().getContentAsString())
            .get("conversation_id").asText();

        String body2 = objectMapper.writeValueAsString(
            Map.of("recipient_username", "bob", "content", "Second"));
        MvcResult r2 = mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON).content(body2))
            .andExpect(status().isCreated()).andReturn();
        String convId2 = objectMapper.readTree(r2.getResponse().getContentAsString())
            .get("conversation_id").asText();

        org.junit.jupiter.api.Assertions.assertEquals(convId1, convId2);
    }

    @Test
    void createConversation_sendToSelf_returns422() throws Exception {
        mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("recipient_username", "alice", "content", "Me"))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void createConversation_recipientDeleted_returns422() throws Exception {
        UserEntity bob = userRepository.findById(bobId).orElseThrow();
        bob.setState(UserEntity.State.deleted);
        userRepository.save(bob);

        mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("recipient_username", "bob", "content", "Hello?"))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("user_unavailable"));
    }

    @Test
    void createConversation_emptyContent_returns422() throws Exception {
        mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("recipient_username", "bob", "content", ""))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void createConversation_contentTooLong_returns422() throws Exception {
        mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("recipient_username", "bob", "content", "x".repeat(2001)))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void createConversation_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("recipient_username", "bob", "content", "Hi"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createConversation_recipientNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("recipient_username", "nonexistent", "content", "Hello?"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    // ===== GET /api/conversations =====

    @Test
    void listConversations_withData_returns200() throws Exception {
        createConv(aliceToken, "bob", "Hello Bob!");

        mockMvc.perform(get("/api/conversations")
                .header("Authorization", "Bearer " + aliceToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].conversation_id").isNotEmpty())
            .andExpect(jsonPath("$.items[0].other_user").isNotEmpty())
            .andExpect(jsonPath("$.items[0].last_message").isNotEmpty())
            .andExpect(jsonPath("$.items[0].unread_count").isNumber())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void listConversations_empty_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/conversations")
                .header("Authorization", "Bearer " + aliceToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(0)))
            .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void listConversations_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/conversations"))
            .andExpect(status().isUnauthorized());
    }

    // ===== GET /api/conversations/unread-count =====

    @Test
    void unreadCount_hasUnread_returnsCount() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "Hi Bob!");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();
        sendMsg(bobToken, convId, "Hi Alice!");

        mockMvc.perform(get("/api/conversations/unread-count")
                .header("Authorization", "Bearer " + aliceToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void unreadCount_noUnread_returnsZero() throws Exception {
        mockMvc.perform(get("/api/conversations/unread-count")
                .header("Authorization", "Bearer " + aliceToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0));
    }

    // ===== GET /api/conversations/{id}/messages =====

    @Test
    void listMessages_valid_returns200() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "Hello!");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();

        mockMvc.perform(get("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + aliceToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].content").value("Hello!"))
            .andExpect(jsonPath("$.items[0].sender_id").isNotEmpty())
            .andExpect(jsonPath("$.items[0].read").value(false))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(50))
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void listMessages_notParticipant_returns403() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "Private");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();
        String charlieToken = registerAndLogin("charlie@example.com", "Password1");

        mockMvc.perform(get("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + charlieToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error_code").value("access_denied"));
    }

    @Test
    void listMessages_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/conversations/00000000-0000-0000-0000-000000000001/messages")
                .header("Authorization", "Bearer " + aliceToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error_code").value("not_found"));
    }

    // ===== POST /api/conversations/{id}/messages =====

    @Test
    void sendMessage_valid_returns201() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "First");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();

        mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + bobToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "Reply"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(jsonPath("$.message_id").isNotEmpty())
            .andExpect(jsonPath("$.sender_id").isNotEmpty())
            .andExpect(jsonPath("$.content").value("Reply"))
            .andExpect(jsonPath("$.read").value(false));
    }

    @Test
    void sendMessage_notParticipant_returns403() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "Private");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();
        String charlieToken = registerAndLogin("charlie2@example.com", "Password1");

        mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + charlieToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "Intruder!"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error_code").value("access_denied"));
    }

    @Test
    void sendMessage_emptyContent_returns422() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "First");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();

        mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", ""))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void sendMessage_contentTooLong_returns422() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "First");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();

        mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "x".repeat(2001)))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    @Test
    void sendMessage_recipientDeleted_returns422() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "First");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();

        UserEntity bob = userRepository.findById(bobId).orElseThrow();
        bob.setState(UserEntity.State.deleted);
        userRepository.save(bob);

        mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "Hello?"))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("user_unavailable"));
    }

    // ===== POST /api/conversations/{id}/mark-read =====

    @Test
    void markRead_valid_marksMessagesAsRead() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "Hello Bob!");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();
        sendMsg(bobToken, convId, "Hi Alice!");

        mockMvc.perform(post("/api/conversations/" + convId + "/mark-read")
                .header("Authorization", "Bearer " + aliceToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(jsonPath("$.marked_count").value(1));

        // Verify unread count drops to 0
        mockMvc.perform(get("/api/conversations/unread-count")
                .header("Authorization", "Bearer " + aliceToken))
            .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void markRead_notParticipant_returns403() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "Private");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();
        String charlieToken = registerAndLogin("charlie3@example.com", "Password1");

        mockMvc.perform(post("/api/conversations/" + convId + "/mark-read")
                .header("Authorization", "Bearer " + charlieToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error_code").value("access_denied"));
    }

    // ===== Rate limit test =====

    @Test
    void sendMessage_rateLimit_returns429() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "First");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();

        // Send 19 more messages (first one was already sent in createConv)
        for (int i = 0; i < 19; i++) {
            sendMsg(aliceToken, convId, "msg" + i);
        }

        // 21st message should be rate limited
        mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "Over limit!"))))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error_code").value("rate_limited"));
    }

    // ===== GET /api/conversations/{id} =====

    @Test
    void getConversation_valid_returns200() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "Hello Bob!");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();

        mockMvc.perform(get("/api/conversations/" + convId)
                .header("Authorization", "Bearer " + aliceToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(jsonPath("$.conversation_id").value(convId))
            .andExpect(jsonPath("$.other_user.username").value("bob"))
            .andExpect(jsonPath("$.other_user.deleted").value(false));
    }

    @Test
    void getConversation_notParticipant_returns403() throws Exception {
        MvcResult r = createConv(aliceToken, "bob", "Private");
        String convId = objectMapper.readTree(r.getResponse().getContentAsString())
            .get("conversation_id").asText();
        String charlieToken = registerAndLogin("charlie4@example.com", "Password1");

        mockMvc.perform(get("/api/conversations/" + convId)
                .header("Authorization", "Bearer " + charlieToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error_code").value("access_denied"));
    }

    // ===== Helpers =====

    private String registerAndLogin(String email, String password) throws Exception {
        codeStore.save(email.toLowerCase(), "123456", 600);
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new RegisterRequest(email, "123456", password))));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("access_token").asText();
    }

    private MvcResult createConv(String senderToken, String recipientUsername, String content) throws Exception {
        return mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + senderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("recipient_username", recipientUsername, "content", content))))
            .andExpect(status().isCreated()).andReturn();
    }

    private void sendMsg(String senderToken, String conversationId, String content) throws Exception {
        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                .header("Authorization", "Bearer " + senderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", content))))
            .andExpect(status().isCreated());
    }

    private void setUsername(UUID userId, String username) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        user.setUsername(username);
        user.setNickname(username);
        userRepository.save(user);
    }

    private UUID extractUserId(String token) throws Exception {
        String payload = token.split("\\.")[1];
        String decoded = new String(java.util.Base64.getUrlDecoder().decode(payload));
        return UUID.fromString(objectMapper.readTree(decoded).get("sub").asText());
    }
}
