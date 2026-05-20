package com.soccialy.backend.service;

import com.soccialy.backend.dto.NotificationDTO;
import com.soccialy.backend.entity.Notification;
import com.soccialy.backend.entity.NotificationType;
import com.soccialy.backend.mapper.NotificationMapper;
import com.soccialy.backend.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getUserNotifications_ReturnsMappedList() {
        Notification n1 = Notification.builder()
                .id(1).recipientUserId(10)
                .type(NotificationType.GROUP_INVITE)
                .message("Invite").createdAt(LocalDateTime.now()).build();

        NotificationDTO dto1 = NotificationDTO.builder()
                .id(1).type("GROUP_INVITE").message("Invite").build();

        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(10))
                .thenReturn(List.of(n1));
        when(notificationMapper.toDTO(n1)).thenReturn(dto1);

        List<NotificationDTO> result = notificationService.getUserNotifications(10);

        assertEquals(1, result.size());
        assertEquals("GROUP_INVITE", result.get(0).getType());
        verify(notificationRepository).findByRecipientUserIdOrderByCreatedAtDesc(10);
    }

    @Test
    void getUserNotifications_EmptyList() {
        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(99))
                .thenReturn(List.of());

        List<NotificationDTO> result = notificationService.getUserNotifications(99);

        assertTrue(result.isEmpty());
    }

    @Test
    void createTestNotification_SavesAndReturnsDTO() {
        NotificationDTO expectedDTO = NotificationDTO.builder()
                .id(1).type("GROUP_INVITE").message("Ai fost invitat la Board Games Group").build();

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    n.setId(1);
                    return n;
                });
        when(notificationMapper.toDTO(any(Notification.class))).thenReturn(expectedDTO);

        NotificationDTO result = notificationService.createTestNotification(42);

        assertNotNull(result);
        assertEquals("GROUP_INVITE", result.getType());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(42, saved.getRecipientUserId());
        assertEquals(NotificationType.GROUP_INVITE, saved.getType());
        assertEquals("GROUP", saved.getReferenceType());
        assertEquals(1, saved.getReferenceId());
    }

    @Test
    void markRead_SetsReadTrue() {
        Notification notification = Notification.builder()
                .id(5).recipientUserId(10)
                .type(NotificationType.REMINDER)
                .message("Reminder").createdAt(LocalDateTime.now()).build();

        when(notificationRepository.findById(5)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markRead(5);

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markRead_NotFound_Throws() {
        when(notificationRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                notificationService.markRead(999));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void unreadCount_ReturnsCount() {
        when(notificationRepository.countByRecipientUserIdAndIsReadFalse(10))
                .thenReturn(3L);

        long count = notificationService.unreadCount(10);

        assertEquals(3L, count);
    }

    @Test
    void unreadCount_ZeroWhenAllRead() {
        when(notificationRepository.countByRecipientUserIdAndIsReadFalse(10))
                .thenReturn(0L);

        assertEquals(0L, notificationService.unreadCount(10));
    }
}
