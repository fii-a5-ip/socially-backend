package com.soccialy.backend.controller;

import com.soccialy.backend.dto.NotificationDTO;
import com.soccialy.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getNotifications_returnsList() {
        Integer userId = 42;
        NotificationDTO dto = new NotificationDTO();
        when(authentication.getPrincipal()).thenReturn(userId.toString());
        when(notificationService.getUserNotifications(userId)).thenReturn(List.of(dto));

        ResponseEntity<List<NotificationDTO>> response = notificationController.getNotifications(authentication);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(notificationService).getUserNotifications(userId);
    }

    @Test
    void getUnreadCount_returnsCount() {
        Integer userId = 42;
        when(authentication.getPrincipal()).thenReturn(userId.toString());
        when(notificationService.unreadCount(userId)).thenReturn(5L);

        ResponseEntity<Long> response = notificationController.getUnreadCount(authentication);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(5L, response.getBody());
        verify(notificationService).unreadCount(userId);
    }

    @Test
    void createTestNotification_returnsDTO() {
        Integer userId = 42;
        NotificationDTO dto = new NotificationDTO();
        when(authentication.getPrincipal()).thenReturn(userId.toString());
        when(notificationService.createTestNotification(userId)).thenReturn(dto);

        ResponseEntity<NotificationDTO> response = notificationController.createTestNotification(authentication);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(dto, response.getBody());
        verify(notificationService).createTestNotification(userId);
    }

    @Test
    void acceptGroupInvite_usesAuthenticatedUser() {
        Integer userId = 42;
        Integer notificationId = 123;
        when(authentication.getPrincipal()).thenReturn(userId.toString());

        ResponseEntity<Void> response = notificationController.acceptGroupInvite(notificationId, authentication);

        assertNotNull(response);
        assertEquals(204, response.getStatusCode().value());
        verify(notificationService).acceptGroupInvite(notificationId, userId);
    }

    @Test
    void declineGroupInvite_usesAuthenticatedUser() {
        Integer userId = 42;
        Integer notificationId = 123;
        when(authentication.getPrincipal()).thenReturn(userId.toString());

        ResponseEntity<Void> response = notificationController.declineGroupInvite(notificationId, authentication);

        assertNotNull(response);
        assertEquals(204, response.getStatusCode().value());
        verify(notificationService).declineGroupInvite(notificationId, userId);
    }

    @Test
    void markAsRead_returnsNoContent() {
        Integer notificationId = 123;
        doNothing().when(notificationService).markRead(notificationId);

        ResponseEntity<Void> response = notificationController.markAsRead(notificationId);

        assertNotNull(response);
        assertEquals(204, response.getStatusCode().value());
        verify(notificationService).markRead(notificationId);
    }
}
