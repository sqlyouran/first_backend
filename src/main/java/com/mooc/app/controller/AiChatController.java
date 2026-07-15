package com.mooc.app.controller;

import com.mooc.app.dto.AiChatRequest;
import com.mooc.app.dto.response.AiConversationResponse;
import com.mooc.app.exception.AiChatException;
import com.mooc.app.service.AiChatService;
import com.mooc.app.service.JwtService;
import com.mooc.app.service.RateLimitService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;
import java.util.UUID;

@RestController
public class AiChatController {

    private final AiChatService aiChatService;
    private final JwtService jwtService;
    private final RateLimitService rateLimitService;

    public AiChatController(AiChatService aiChatService, JwtService jwtService, RateLimitService rateLimitService) {
        this.aiChatService = aiChatService;
        this.jwtService = jwtService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/api/ai/conversations")
    public ResponseEntity<AiConversationResponse> createConversation(HttpServletRequest httpRequest) {
        Optional<UUID> userId = AuthUtil.optionalUserId(httpRequest, jwtService);
        checkAnonymousRateLimit(userId, httpRequest);
        String requestId = AuthUtil.getRequestId(httpRequest);
        AiConversationResponse response = aiChatService.createConversation(userId.orElse(null), requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/ai/chat")
    public SseEmitter chat(@Valid @RequestBody AiChatRequest request, HttpServletRequest httpRequest) {
        Optional<UUID> userId = AuthUtil.optionalUserId(httpRequest, jwtService);
        checkAnonymousRateLimit(userId, httpRequest);
        return aiChatService.sendMessage(request.conversation_id(), request.message());
    }

    private void checkAnonymousRateLimit(Optional<UUID> userId, HttpServletRequest httpRequest) {
        if (userId.isEmpty()) {
            String ip = httpRequest.getRemoteAddr();
            if (rateLimitService.isAiChatIpRateLimited(ip)) {
                throw new AiChatException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited",
                        "Anonymous users are limited to 20 AI chat requests per day");
            }
        }
    }
}
