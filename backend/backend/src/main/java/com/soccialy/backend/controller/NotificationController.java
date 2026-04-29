package com.socially.controller;

import com.socially.service.NotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation
        .AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/notifications"
)
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService
            notificationService;


    @GetMapping
    public ResponseEntity<?>
    getNotifications(
            @AuthenticationPrincipal
            String currentUserIdStr
    ){

        Integer userId=
                Integer.parseInt(
                        currentUserIdStr
                );

        return ResponseEntity.ok(
                notificationService
                        .getUserNotifications(
                                userId
                        )
        );

    }


    @GetMapping(
            "/unread-count"
    )
    public ResponseEntity<?>
    getUnreadCount(
            @AuthenticationPrincipal
            String currentUserIdStr
    ){

        Integer userId=
                Integer.parseInt(
                        currentUserIdStr
                );

        return ResponseEntity.ok(
                notificationService
                        .unreadCount(
                                userId
                        )
        );

    }


    @PostMapping(
            "/test"
    )
    public ResponseEntity<?>
    createTestNotification(
            @AuthenticationPrincipal
            String currentUserIdStr
    ){

        Integer userId=
                Integer.parseInt(
                        currentUserIdStr
                );

        return ResponseEntity.ok(
                notificationService
                        .createTestNotification(
                                userId
                        )
        );

    }


    @PatchMapping(
            "/{id}/read"
    )
    public ResponseEntity<Void>
    markAsRead(
            @PathVariable
            Integer id
    ){

        notificationService.markRead(
                id
        );

        return ResponseEntity
                .noContent()
                .build();

    }

}