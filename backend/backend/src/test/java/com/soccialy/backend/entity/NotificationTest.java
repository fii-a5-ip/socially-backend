package com.soccialy.backend.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void testNotificationGettersSettersAndBuilder() {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = Notification.builder()
                .id(1)
                .recipientUserId(10)
                .actorUserId(5)
                .type(NotificationType.GROUP_INVITE)
                .message("Hello")
                .referenceId(100)
                .referenceType("GROUP")
                .isRead(true)
                .createdAt(now)
                .actions("accept")
                .externalLink("http://example.com")
                .build();

        assertEquals(1, notification.getId());
        assertEquals(10, notification.getRecipientUserId());
        assertEquals(5, notification.getActorUserId());
        assertEquals(NotificationType.GROUP_INVITE, notification.getType());
        assertEquals("Hello", notification.getMessage());
        assertEquals(100, notification.getReferenceId());
        assertEquals("GROUP", notification.getReferenceType());
        assertTrue(notification.isRead());
        assertEquals(now, notification.getCreatedAt());
        assertEquals("accept", notification.getActions());
        assertEquals("http://example.com", notification.getExternalLink());

        // Test setters
        notification.setId(2);
        notification.setRecipientUserId(20);
        notification.setActorUserId(15);
        notification.setType(NotificationType.REMINDER);
        notification.setMessage("Reminder text");
        notification.setReferenceId(200);
        notification.setReferenceType("EVENT");
        notification.setRead(false);
        notification.setActions("dismiss");
        notification.setExternalLink("http://other.com");

        assertEquals(2, notification.getId());
        assertEquals(20, notification.getRecipientUserId());
        assertEquals(15, notification.getActorUserId());
        assertEquals(NotificationType.REMINDER, notification.getType());
        assertEquals("Reminder text", notification.getMessage());
        assertEquals(200, notification.getReferenceId());
        assertEquals("EVENT", notification.getReferenceType());
        assertFalse(notification.isRead());
        assertEquals("dismiss", notification.getActions());
        assertEquals("http://other.com", notification.getExternalLink());
    }

    @Test
    void testPrePersistSetsCreatedAt() {
        Notification notification = new Notification();
        assertNull(notification.getCreatedAt());

        notification.onCreate();

        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void testPrePersistDoesNotOverwriteExistingCreatedAt() {
        LocalDateTime existing = LocalDateTime.of(2026, 1, 1, 12, 0);
        Notification notification = new Notification();
        notification.setCreatedAt(existing);

        notification.onCreate();

        assertEquals(existing, notification.getCreatedAt());
    }

    @Test
    void testNoArgsConstructor() {
        Notification notification = new Notification();
        assertNull(notification.getId());
        assertFalse(notification.isRead());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = new Notification(
                1, 10, 5, NotificationType.SYSTEM_UPDATE, "System Msg", 99, "SYS", true, now, "none", "url"
        );

        assertEquals(1, notification.getId());
        assertEquals(10, notification.getRecipientUserId());
        assertEquals(5, notification.getActorUserId());
        assertEquals(NotificationType.SYSTEM_UPDATE, notification.getType());
        assertEquals("System Msg", notification.getMessage());
        assertEquals(99, notification.getReferenceId());
        assertEquals("SYS", notification.getReferenceType());
        assertTrue(notification.isRead());
        assertEquals(now, notification.getCreatedAt());
        assertEquals("none", notification.getActions());
        assertEquals("url", notification.getExternalLink());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        Notification n1 = new Notification(1, 10, 5, NotificationType.SYSTEM_UPDATE, "Msg", 99, "SYS", true, now, "none", "url");
        Notification n2 = new Notification(1, 10, 5, NotificationType.SYSTEM_UPDATE, "Msg", 99, "SYS", true, now, "none", "url");
        Notification n3 = new Notification(2, 10, 5, NotificationType.SYSTEM_UPDATE, "Msg", 99, "SYS", true, now, "none", "url");

        assertEquals(n1, n2);
        assertEquals(n1.hashCode(), n2.hashCode());
        assertNotEquals(n1, n3);
    }

    @Test
    void testToString() {
        Notification notification = Notification.builder().id(1).message("Hello").build();
        assertNotNull(notification.toString());
        assertTrue(notification.toString().contains("id=1"));
        assertTrue(notification.toString().contains("message=Hello"));
    }
}
