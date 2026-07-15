package com.mooc.app.controller;

import com.mooc.app.dto.response.MarkAllReadResponse;
import com.mooc.app.dto.response.NotificationListResponse;
import com.mooc.app.dto.response.NotificationListResponse.NotificationItemResponse;
import com.mooc.app.dto.response.UnreadCountResponse;
import com.mooc.app.entity.NotificationEntity;
import com.mooc.app.entity.UserEntity;
import com.mooc.app.service.JwtService;
import com.mooc.app.service.NotificationService;
import com.mooc.app.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
        List<NotificationEntity> notifications = result.getContent();

        Map<UUID, UserEntity> actorMap = notificationService.batchResolveActors(notifications);
        Map<UUID, String> titleMap = notificationService.batchResolveTargetTitles(notifications);

        List<NotificationItemResponse> items = notifications.stream()
                .map(n -> toItemResponse(n, actorMap, titleMap))
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
        return ResponseEntity.ok().build();
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

    private NotificationItemResponse toItemResponse(NotificationEntity n,
                                                       Map<UUID, UserEntity> actorMap,
                                                       Map<UUID, String> titleMap) {
        UserEntity actor = actorMap.get(n.getActorId());
        String actorNickname = actor != null
                ? (actor.getNickname() != null ? actor.getNickname() : actor.getEmail())
                : "Unknown";
        String actorAvatarUrl = actor != null ? actor.getAvatarUrl() : null;
        String actorUsername = actor != null ? actor.getUsername() : null;
        String targetTitle = n.getEntityId() != null ? titleMap.get(n.getEntityId()) : null;

        return new NotificationItemResponse(
                n.getId().toString(),
                n.getType().name(),
                n.getActorId().toString(),
                actorNickname,
                actorAvatarUrl,
                actorUsername,
                n.getEntityId() != null ? n.getEntityId().toString() : null,
                n.getEntityType(),
                targetTitle,
                n.getContentPreview(),
                n.isRead(),
                n.getCreatedAt().toString()
        );
    }
}
