package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.NotificationDTO;
import com.soccialy.backend.entity.Notification;
import org.springframework.stereotype.Component;
import com.soccialy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Component
public class NotificationMapper {

    private final UserRepository userRepository;

    public NotificationDTO toDTO( Notification notification )
        {
                if (notification == null) { return null; }

                // prevenire crash în caz că ”nu există” users
            var user = (notification.getActorUserId() != null)
              ? userRepository.findById(notification.getActorUserId()).orElse(null) : null;

        return NotificationDTO.builder()
                .id(notification.getId())
                .actorUserId(
                        notification.getActorUserId()
                )
                .type(
                        notification.getType() != null ? notification.getType().name() : null
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
                        notification.isRead()
                )
                .createdAt(
                        notification.getCreatedAt()
                )

                .username(user != null ? user.getUsername() : null)
                .profileImageUrl(user != null ? user.getProfileImgUrl() : null)

                .build();

    }

}
