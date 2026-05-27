package com.soccialy.backend.service;

import com.soccialy.backend.dto.NotificationDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.GroupMember;
import com.soccialy.backend.entity.Notification;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.mapper.NotificationMapper;
import com.soccialy.backend.repository.GroupRepository;
import com.soccialy.backend.repository.NotificationRepository;
import com.soccialy.backend.repository.UserRepository;
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

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

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
                .type("GROUP_INVITE")
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
        assertEquals("GROUP_INVITE", saved.getType());
        assertEquals("GROUP_REFERENCE_TYPE", saved.getReferenceType());
        assertEquals(1, saved.getReferenceId());
    }

    @Test
    void markRead_SetsReadTrue() {
        Notification notification = Notification.builder()
                .id(5).recipientUserId(10)
                .type("REMINDER")
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

    @Test
    void createGroupInvite_SavesInviteNotification() {
        User actor = User.builder().id(1).username("ana").build();
        User recipient = User.builder().id(2).username("bob").build();
        Group group = Group.builder().id(3).name("Board Games").creator(actor).build();

       //  Inseram un membru in grupul acesta fictiv, altfel nu va exista o sursa corecta de la care să fie trimise invitațiile
        GroupMember membruGrup = GroupMember.builder()
           .id(1) 
            .user(actor)
            .group(group)
            .role("ADMIN") 
            .build();
         group.getMembers().add(membruGrup);

        NotificationDTO expectedDTO = NotificationDTO.builder()
                .id(7).type("GROUP_INVITE").message("ana te-a invitat in grupul Board Games").build();

        when(userRepository.findById(1)).thenReturn(Optional.of(actor));
        when(userRepository.findById(2)).thenReturn(Optional.of(recipient));
        when(groupRepository.findById(3)).thenReturn(Optional.of(group));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(notificationMapper.toDTO(any(Notification.class))).thenReturn(expectedDTO);

        NotificationDTO result = notificationService.createGroupInvite(3, 2, 1);

        assertEquals(expectedDTO, result);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(2, saved.getRecipientUserId());
        assertEquals(1, saved.getActorUserId());
        assertEquals("GROUP_INVITE", saved.getType());
        assertEquals("GROUP", saved.getReferenceType());
        assertEquals(3, saved.getReferenceId());
        assertEquals("ana te-a invitat in grupul Board Games", saved.getMessage());
    }

    @Test
    void acceptGroupInvite_AddsRecipientToReferencedGroupAndMarksRead() {
        User recipient = User.builder().id(2).username("bob").build();
        Group group = Group.builder().id(3).name("Board Games").build();
        Notification invite = Notification.builder()
                .id(7)
                .recipientUserId(2)
                .actorUserId(1)
                .type("GROUP_INVITE")
                .message("Invite")
                .referenceType("GROUP")
                .referenceId(3)
                .build();

        when(notificationRepository.findById(7)).thenReturn(Optional.of(invite));
        when(groupRepository.findById(3)).thenReturn(Optional.of(group));
        when(userRepository.findById(2)).thenReturn(Optional.of(recipient));

        notificationService.acceptGroupInvite(7, 2);

        assertTrue(invite.isRead());
        assertEquals(1, group.getMembers().size());
        GroupMember addedMember = group.getMembers().get(0);
        assertEquals(group, addedMember.getGroup());
        assertEquals(recipient, addedMember.getUser());
        assertEquals("MEMBER", addedMember.getRole());
        verify(groupRepository).save(group);
        verify(notificationRepository).save(invite);
    }

    @Test
    void acceptGroupInvite_DoesNotDuplicateExistingMember() {
        User recipient = User.builder().id(2).username("bob").build();
        Group group = Group.builder().id(3).name("Board Games").build();
        group.getMembers().add(GroupMember.builder()
                .group(group)
                .user(recipient)
                .role("MEMBER")
                .build());
        Notification invite = Notification.builder()
                .id(7)
                .recipientUserId(2)
                .type("GROUP_INVITE")
                .referenceType("GROUP")
                .referenceId(3)
                .build();

        when(notificationRepository.findById(7)).thenReturn(Optional.of(invite));
        when(groupRepository.findById(3)).thenReturn(Optional.of(group));
        when(userRepository.findById(2)).thenReturn(Optional.of(recipient));

        notificationService.acceptGroupInvite(7, 2);

        assertEquals(1, group.getMembers().size());
        assertTrue(invite.isRead());
        verify(groupRepository, never()).save(group);
        verify(notificationRepository).save(invite);
    }

    @Test
    void acceptGroupInvite_ForDifferentRecipientThrows() {
        Notification invite = Notification.builder()
                .id(7)
                .recipientUserId(2)
                .type("GROUP_INVITE")
                .referenceType("GROUP")
                .referenceId(3)
                .build();
        when(notificationRepository.findById(7)).thenReturn(Optional.of(invite));

        assertThrows(IllegalArgumentException.class,
                () -> notificationService.acceptGroupInvite(7, 99));
        verify(groupRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void declineGroupInvite_MarksOwnInviteRead() {
        Notification invite = Notification.builder()
                .id(7)
                .recipientUserId(2)
                .type("GROUP_INVITE")
                .referenceType("GROUP")
                .referenceId(3)
                .build();
        when(notificationRepository.findById(7)).thenReturn(Optional.of(invite));

        notificationService.declineGroupInvite(7, 2);

        assertTrue(invite.isRead());
        verify(notificationRepository).save(invite);
        verify(groupRepository, never()).save(any());
    }
}
