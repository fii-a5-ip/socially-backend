package com.soccialy.backend.service;

import comsoccialy.backend.dto.NotificationDTO;
import com.soccialy.backend.entity.Notification;
import com.soccialy.backend.entity.NotificationType;
import com.soccialy.backend.mapper.NotificationMapper;
import com.soccialy.backend.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository
            notificationRepository;

    private final NotificationMapper
            notificationMapper;


    public List<NotificationDTO>
    getUserNotifications(
            Integer userId
    ){

        return notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(
                        userId
                )
                .stream()
                .map(
                        notificationMapper::toDTO
                )
                .toList();

    }


    public NotificationDTO
    createTestNotification(
            Integer userId
    ){

        Notification notification=
                Notification.builder()
                        .recipientUserId(
                                userId
                        )
                        .type(
                                NotificationType.GROUP_INVITE
                        )
                        .message(
                                "Ai fost invitat la Board Games Group"
                        )
                        .referenceType(
                                "GROUP"
                        )
                        .referenceId(
                                1
                        )
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        Notification saved=
                notificationRepository.save(
                        notification
                );

        return notificationMapper.toDTO(
                saved
        );

    }


    public void markRead(
            Integer id
    ){

        Notification notification=
                notificationRepository.findById(
                                id
                        )
                        .orElseThrow();

        notification.setRead(
                true
        );

        notificationRepository.save(
                notification
        );

    }


    public long unreadCount(
            Integer userId
    ){

        return notificationRepository
                .countByRecipientUserIdAndReadFalse(
                        userId
                );

    }

}
