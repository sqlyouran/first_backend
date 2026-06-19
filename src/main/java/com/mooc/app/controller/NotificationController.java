package com.mooc.app.controller;

import com.mooc.app.dto.response.MarkAllReadResponse;
import com.mooc.app.dto.response.NotificationListResponse;
import com.mooc.app.dto.response.NotificationListResponse.NotificationItemResponse;
import com.mooc.app.dto.response.UnreadCountResponse;
import com.mooc.app.entity.NotificationEntity;
import com.mooc.app.service.JwtService;
import com.mooc.app.service.NotificationService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    public NotificationController(NotificationService notificationService, JwtService jwtService) {
        this.notificationService = notificationService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<NotificationListResponse> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);

        Page<NotificationEntity> result = notificationService.listNotifications(userId, page, size);
        List<NotificationItemResponse> items = result.getContent().stream()
                .map(n -> toItemResponse(n))
                .toList();

        NotificationListResponse response = new NotificationListResponse(
                requestId, items, result.getTotalElements(), page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<MarkAllReadResponse> markAllRead(HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        int updatedCount = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(new MarkAllReadResponse(requestId, updatedCount));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(HttpServletRequest httpRequest) {
        UUID userId = AuthUtil.requireUserId(httpRequest, jwtService);
        String requestId = AuthUtil.getRequestId(httpRequest);
        long count = notificationService.countUnread(userId);
        return ResponseEntity.ok(new UnreadCountResponse(requestId, count));
    }

    private NotificationItemResponse toItemResponse(NotificationEntity n) {
        String actorNickname = notificationService.resolveActorNickname(n.getActorId());
        String targetTitle = notificationService.resolveTargetTitle(n.getEntityId());

        return new NotificationItemResponse(
                n.getId().toString(),
                n.getType().name(),
                n.getActorId().toString(),
                actorNickname,
                null,
                null,
                n.getEntityId() != null ? n.getEntityId().toString() : null,
                n.getEntityType(),
                targetTitle,
                n.getContentPreview(),
                n.isRead(),
                n.getCreatedAt().toString()
        );
    }
}
