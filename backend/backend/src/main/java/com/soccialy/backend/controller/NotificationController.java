package com.soccialy.backend.controller;

import com.soccialy.backend.service.NotificationService;


import com.soccialy.backend.dto.NotificationDTO;
import java.util.List;


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
    public  ResponseEntity<List<NotificationDTO>>
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
    public ResponseEntity<Long>
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
    public   ResponseEntity<NotificationDTO>
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
