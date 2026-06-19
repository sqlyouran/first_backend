package com.mooc.app.controller;

import com.mooc.app.dto.CreateConversationRequest;
import com.mooc.app.dto.SendMessageRequest;
import com.mooc.app.dto.response.*;
import com.mooc.app.service.ConversationService;
import com.mooc.app.service.JwtService;
import com.mooc.app.service.MessageService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final JwtService jwtService;

    public ConversationController(ConversationService conversationService,
                                   MessageService messageService,
                                   JwtService jwtService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/conversations")
    public ResponseEntity<CreateConversationResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest request,
            HttpServletRequest httpRequest) {
        UUID senderId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        CreateConversationResponse response = conversationService.createConversation(
                request, senderId, requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/conversations")
    public ResponseEntity<ConversationListResponse> listConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        ConversationListResponse response = conversationService.listConversations(
                userId, page, size, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/conversations/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        UnreadCountResponse response = conversationService.getUnreadCount(userId, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/conversations/{id}/messages")
    public ResponseEntity<MessageListResponse> listMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        MessageListResponse response = messageService.listMessages(id, userId, page, size, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/conversations/{id}/messages")
    public ResponseEntity<SendMessageResponse> sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {
        UUID senderId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        SendMessageResponse response = messageService.sendMessage(id, senderId, request, requestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/conversations/{id}/mark-read")
    public ResponseEntity<MarkReadResponse> markRead(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        MarkReadResponse response = messageService.markRead(id, userId, requestId);
        return ResponseEntity.ok(response);
    }
}
