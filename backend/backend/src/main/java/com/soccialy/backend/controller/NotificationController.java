package com.soccialy.backend.controller;

import com.soccialy.backend.dto.NotificationDTO;
import com.soccialy.backend.service.NotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>>
    getNotifications(Authentication authentication) {

        Integer userId = currentUserId(authentication);

        return ResponseEntity.ok(
                notificationService.getUserNotifications(userId)
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long>
    getUnreadCount(Authentication authentication) {

        Integer userId = currentUserId(authentication);

        return ResponseEntity.ok(
                notificationService.unreadCount(userId)
        );
    }

    @PostMapping("/test")
    public ResponseEntity<NotificationDTO>
    createTestNotification(Authentication authentication) {

        Integer userId = currentUserId(authentication);

        return ResponseEntity.ok(
                notificationService.createTestNotification(userId)
        );
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void>
    markAsRead(@PathVariable Integer id) {

        notificationService.markRead(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Void>
    acceptGroupInvite(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        notificationService.acceptGroupInvite(id, currentUserId(authentication));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<Void>
    declineGroupInvite(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        notificationService.declineGroupInvite(id, currentUserId(authentication));

        return ResponseEntity.noContent().build();
    }

    private Integer currentUserId(Authentication authentication) {
        return Integer.parseInt(authentication.getPrincipal().toString());
    }
}
