package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.NotificationDTO;
import com.soccialy.backend.entity.Notification;
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
