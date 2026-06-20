package com.mooc.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooc.app.dto.LoginRequest;
import com.mooc.app.dto.RegisterRequest;
import com.mooc.app.dto.SendCodeRequest;
import com.mooc.app.service.VerificationCodeStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VerificationCodeStore codeStore;

    // === send-code tests ===

    @Test
    void sendCode_validEmail_returns200WithRequestId() throws Exception {
        mockMvc.perform(post("/api/auth/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SendCodeRequest("user@example.com"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void sendCode_invalidEmail_returns422() throws Exception {
        mockMvc.perform(post("/api/auth/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"not-an-email\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"))
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void sendCode_ipRateLimit_returns429() throws Exception {
        // 5 requests within 1 minute should trigger rate limit
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/send-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new SendCodeRequest("user" + i + "@example.com"))));
        }
        mockMvc.perform(post("/api/auth/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SendCodeRequest("extra@example.com"))))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error_code").value("rate_limited"));
    }

    // === register tests ===

    @Test
    void register_validRequest_returns201() throws Exception {
        // Setup: store a valid code
        codeStore.save("newuser@example.com", "123456", 600);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest("newuser@example.com", "123456", "Password1"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void register_invalidCode_returns400() throws Exception {
        codeStore.save("user@example.com", "123456", 600);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest("user@example.com", "999999", "Password1"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("invalid_code"));
    }

    @Test
    void register_emailAlreadyRegistered_returns409() throws Exception {
        // First register
        codeStore.save("dup@example.com", "123456", 600);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest("dup@example.com", "123456", "Password1"))));

        // Try again
        codeStore.save("dup@example.com", "654321", 600);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest("dup@example.com", "654321", "Password2"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("email_already_registered"));
    }

    @Test
    void register_validationError_returns422() throws Exception {
        // Password too short
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest("user@example.com", "123456", "short"))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"))
            .andExpect(jsonPath("$.details.password").isNotEmpty());
    }

    // === login tests ===

    @Test
    void login_validCredentials_returns200WithTokenAndCookie() throws Exception {
        // Setup user
        registerUser("login@example.com", "Password1");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("login@example.com", "Password1"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").isNotEmpty())
            .andExpect(jsonPath("$.expires_in").value(1800))
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(cookie().exists("refresh_token"))
            .andExpect(cookie().httpOnly("refresh_token", true))
            .andExpect(cookie().path("refresh_token", "/"))
            .andReturn();
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        registerUser("user2@example.com", "Password1");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("user2@example.com", "WrongPassword1"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error_code").value("invalid_credentials"));
    }

    @Test
    void login_nonexistentUser_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("nobody@example.com", "Password1"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error_code").value("invalid_credentials"));
    }

    @Test
    void login_validationError_returns422() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"\", \"password\": \"\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error_code").value("validation_error"));
    }

    // === refresh tests ===

    @Test
    void refresh_validToken_returns200() throws Exception {
        registerUser("refresh@example.com", "Password1");
        Cookie refreshCookie = loginAndGetRefreshCookie("refresh@example.com", "Password1");

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").isNotEmpty())
            .andExpect(jsonPath("$.expires_in").value(1800));
    }

    @Test
    void refresh_noCookie_returns401AndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error_code").value("invalid_refresh_token"))
            .andExpect(cookie().maxAge("refresh_token", 0));
    }

    @Test
    void refresh_invalidToken_returns401AndClearsCookie() throws Exception {
        Cookie staleCookie = new Cookie("refresh_token", "invalid.stale.token");
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(staleCookie))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error_code").value("invalid_refresh_token"))
            .andExpect(cookie().maxAge("refresh_token", 0));
    }

    // === logout tests ===

    @Test
    void logout_validToken_returns204() throws Exception {
        registerUser("logout@example.com", "Password1");
        Cookie refreshCookie = loginAndGetRefreshCookie("logout@example.com", "Password1");

        mockMvc.perform(post("/api/auth/logout")
                .cookie(refreshCookie))
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge("refresh_token", 0));
    }

    @Test
    void logout_noCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isUnauthorized());
    }

    // === GET /me tests ===

    @Test
    void getMe_validToken_returns200() throws Exception {
        registerUser("me@example.com", "Password1");
        String accessToken = loginAndGetAccessToken("me@example.com", "Password1");

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("me@example.com"))
            .andExpect(jsonPath("$.state").value("active"))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.created_at").isNotEmpty())
            .andExpect(jsonPath("$.request_id").isNotEmpty())
            .andExpect(jsonPath("$.password_hash").doesNotExist())
            .andExpect(jsonPath("$.salt").doesNotExist());
    }

    @Test
    void getMe_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    // === DELETE /me tests ===

    @Test
    void deleteMe_validToken_returns204() throws Exception {
        registerUser("delete@example.com", "Password1");
        String accessToken = loginAndGetAccessToken("delete@example.com", "Password1");

        mockMvc.perform(delete("/api/auth/me")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNoContent());

        // Verify login fails after deletion
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("delete@example.com", "Password1"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMe_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/auth/me"))
            .andExpect(status().isUnauthorized());
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

    private Cookie loginAndGetRefreshCookie(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andReturn();
        return result.getResponse().getCookie("refresh_token");
    }
}
