package com.socially.mapper;

import com.socially.dto.NotificationDTO;
import com.socially.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationDTO toDTO(
            Notification notification
    ){

        return NotificationDTO.builder()
                .id(notification.getId())
                .actorUserId(
                        notification.getActorUserId()
                )
                .type(
                        notification.getType().name()
                )
                .message(
                        notification.getMessage()
                )
                .referenceId(
                        notification.getReferenceId()
                )
                .referenceType(
                        notification.getReferenceType()
                )
                .read(
                        notification.getRead()
                )
                .createdAt(
                        notification.getCreatedAt()
                )
                .build();

    }

}