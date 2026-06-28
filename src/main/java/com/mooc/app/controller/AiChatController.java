package com.mooc.app.controller;

import com.mooc.app.dto.AiChatRequest;
import com.mooc.app.dto.response.AiConversationResponse;
import com.mooc.app.service.AiChatService;
import com.mooc.app.service.JwtService;
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

    public AiChatController(AiChatService aiChatService, JwtService jwtService) {
        this.aiChatService = aiChatService;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/ai/conversations")
    public ResponseEntity<AiConversationResponse> createConversation(HttpServletRequest httpRequest) {
        Optional<UUID> userId = AuthUtil.optionalUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        AiConversationResponse response = aiChatService.createConversation(userId.orElse(null), requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/ai/chat")
    public SseEmitter chat(@Valid @RequestBody AiChatRequest request, HttpServletRequest httpRequest) {
        return aiChatService.sendMessage(request.conversation_id(), request.message());
    }
}
