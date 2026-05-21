package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.NotificationDTO;
import com.soccialy.backend.entity.Notification;
import com.soccialy.backend.entity.NotificationType;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationMapperTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationMapper notificationMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void toDTO_NullInput_ReturnsNull() {
        assertNull(notificationMapper.toDTO(null));
    }

    @Test
    void toDTO_WithActor_MapsAllFields() {
        User actor = new User();
        actor.setId(5);
        actor.setUsername("john_doe");
        actor.setProfileImgUrl("https://example.com/avatar.jpg");


        //pentru momentan coloanele "column_name" asi "column_id" vor fi initializate ca null
        Notification notification = Notification.builder()
                .id(1)
                .recipientUserId(10)
                .actorUserId(5)
                .type("GROUP_INVITE")
                .message("You were invited")
                .referenceId(100)
                .referenceType("GROUP")
                .isRead(false)
                .createdAt(LocalDateTime.of(2026, 5, 20, 12, 0))
                .build();

        when(userRepository.findById(5)).thenReturn(Optional.of(actor));

        NotificationDTO dto = notificationMapper.toDTO(notification);

        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals(5, dto.getActorUserId());
        assertEquals("GROUP_INVITE", dto.getType());
        assertEquals("You were invited", dto.getMessage());
        assertEquals(100, dto.getReferenceId());
        assertEquals("GROUP", dto.getReferenceType());
        assertEquals(false, dto.getRead());
        assertEquals("john_doe", dto.getUsername());
        assertEquals("https://example.com/avatar.jpg", dto.getProfileImageUrl());
    }

    @Test
    void toDTO_WithoutActor_UsernameNull() {
        Notification notification = Notification.builder()
                .id(2)
                .recipientUserId(10)
                .actorUserId(null)
                .type("SYSTEM_UPDATE")
                .message("System update")
                .isRead(true)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationDTO dto = notificationMapper.toDTO(notification);

        assertNotNull(dto);
        assertNull(dto.getActorUserId());
        assertNull(dto.getUsername());
        assertNull(dto.getProfileImageUrl());
        assertEquals("SYSTEM_UPDATE", dto.getType());
        assertEquals(true, dto.getRead());
    }

    @Test
    void toDTO_ActorNotFound_UsernameNull() {
        Notification notification = Notification.builder()
                .id(3)
                .recipientUserId(10)
                .actorUserId(999)
                .type("MESSAGE")
                .message("Hello")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(999)).thenReturn(Optional.empty());

        NotificationDTO dto = notificationMapper.toDTO(notification);

        assertNotNull(dto);
        assertEquals(999, dto.getActorUserId());
        assertNull(dto.getUsername());
        assertNull(dto.getProfileImageUrl());
    }
}
