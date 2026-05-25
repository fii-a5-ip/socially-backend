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
            .type("GROUP_INVITE") 
            .message("Hello")
            .referenceId(100)
            .referenceType("GROUP")
            .isRead(true)
           .createdAt(now)
           .actions("actions_str")
           .externalLink("http://example.com")
           // + columnName si columnNumber care ar trebui sa devina null automat
        .build();

        assertEquals(1, notification.getId());
        assertEquals(10, notification.getRecipientUserId());
        assertEquals(5, notification.getActorUserId());
        assertEquals("GROUP_INVITE", notification.getType());
        assertEquals("Hello", notification.getMessage());
        assertEquals(100, notification.getReferenceId());
        assertEquals("GROUP", notification.getReferenceType());
        assertTrue(notification.isRead());
        assertEquals(now, notification.getCreatedAt());
        assertEquals("actions_str", notification.getActions());
        assertEquals("http://example.com", notification.getExternalLink());

        // Test setters
        notification.setId(2);
        notification.setRecipientUserId(20);
        notification.setActorUserId(15);
        notification.setType("REMINDER");
        notification.setMessage("Reminder text");
        notification.setReferenceId(200);
        notification.setReferenceType("EVENT");
        notification.setRead(false);
        notification.setActions("dismiss");
        notification.setExternalLink("http://other.com");

        assertEquals(2, notification.getId());
        assertEquals(20, notification.getRecipientUserId());
        assertEquals(15, notification.getActorUserId());
        assertEquals("REMINDER", notification.getType());
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
        Notification notification = Notification.builder().id(1).recipientUserId(10).actorUserId(5).type("SYSTEM_UPDATE")
                                                .message("System Msg").referenceId(99)
                                                .referenceType("SYS").isRead(true).createdAt(now)
                                                .actions("none").externalLink("url").columnName("colName").columnId("colId").build(); //arata urat stiu

        assertEquals(1, notification.getId());
        assertEquals(10, notification.getRecipientUserId());
        assertEquals(5, notification.getActorUserId());
        assertEquals("SYSTEM_UPDATE", notification.getType());
        assertEquals("System Msg", notification.getMessage());
        assertEquals(99, notification.getReferenceId());
        assertEquals("SYS", notification.getReferenceType());
        assertTrue(notification.isRead());
        assertEquals(now, notification.getCreatedAt());
        assertEquals("none", notification.getActions());
        assertEquals("url", notification.getExternalLink());

        assertEquals("colName", notification.getColumnName());
        assertEquals("colId", notification.getColumnId());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        Notification n1 = Notification.builder().id(2).recipientUserId(26).actorUserId(12).type("REMINDER").message("M2").isRead(false).createdAt(now).columnName("colName").columnId("colId").build();
        Notification n2 = Notification.builder().id(2).recipientUserId(26).actorUserId(12).type("REMINDER").message("M2").isRead(false).createdAt(now).columnName("colName").columnId("colId").build();
        Notification n3 = Notification.builder().id(3).recipientUserId(26).actorUserId(14).type("SYSTEM_UPDATE").message("M3").isRead(true).createdAt(now).columnName("otherName").columnId("otherId").build();
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
