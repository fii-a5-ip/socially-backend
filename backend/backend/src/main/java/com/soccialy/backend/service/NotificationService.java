package com.soccialy.backend.service;

import com.soccialy.backend.dto.NotificationDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.GroupMember;
import com.soccialy.backend.entity.Notification;
import com.soccialy.backend.entity.NotificationType;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.mapper.NotificationMapper;
import com.soccialy.backend.repository.GroupRepository;
import com.soccialy.backend.repository.NotificationRepository;
import com.soccialy.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.time.LocalDateTime; // lasa-l aici, de ce nu
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String GROUP_REFERENCE_TYPE = "GROUP";
    private static final String NOTIF_TYPE_GROUP = "GROUP_INVITE";

    private final NotificationRepository
            notificationRepository;

    private final NotificationMapper
            notificationMapper;

    private final GroupRepository groupRepository;

    private final UserRepository userRepository;


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
                        .type( "GROUP_INVITE" ) //reamintesc ca conform versiunii V12 de a bazei de date, coloana aceasta este un varchar. nu mai este enum
                        .message(
                                "Ai fost invitat la Board Games Group"
                        )
                        .referenceType(
                                "GROUP_REFERENCE_TYPE"
                        )
                        .referenceId(
                                1
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

    @Transactional
    public NotificationDTO createGroupInvite(
            Integer groupId,
            Integer recipientUserId,
            Integer actorUserId
    ){
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        User recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Actor not found"));

        if (recipient.getId().equals(actor.getId())) {
            throw new IllegalArgumentException("You cannot invite yourself");
        }

        //Nou: verificăm că cel ce trimite invitația face parte din grupul respectiv
        if (!group.hasMember(actor.getId()))
         {throw new IllegalArgumentException ("You do not belong to this group and therefore can not send invitations"); }


        boolean alreadyMember = group.getMembers().stream()
                .anyMatch(member -> member.getUser().getId().equals(recipient.getId()));
        if (alreadyMember) {
            throw new IllegalArgumentException("User is already a group member");
        }

        Notification notification = Notification.builder()
                .recipientUserId(recipient.getId())
                .actorUserId(actor.getId())
                .type("GROUP_INVITE") //NotificationType.GROUP_INVITE nu mai este compatibil. type e acum varchar nu enum
                .message(actor.getUsername() + " te-a invitat in grupul " + group.getName())
                .referenceType(GROUP_REFERENCE_TYPE) //
                .referenceId(group.getId())
                .build();

        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toDTO(saved);
    }

    @Transactional
    public void acceptGroupInvite(
            Integer id,
            Integer userId
    ){
        Notification notification = findOwnGroupInvite(id, userId);
        Group group = groupRepository.findById(notification.getReferenceId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean alreadyMember = group.getMembers().stream()
                .anyMatch(member -> member.getUser().getId().equals(userId));

        if (!alreadyMember) {
            group.getMembers().add(GroupMember.builder()
                    .group(group)
                    .user(user)
                    .role("MEMBER")
                    .build());
            groupRepository.save(group);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void declineGroupInvite(
            Integer id,
            Integer userId
    ){
        Notification notification = findOwnGroupInvite(id, userId);
        notification.setRead(true);
        notificationRepository.save(notification);
    }


    public void markRead(
            Integer id
    ){

        Notification notification=
                notificationRepository.findById(
                                id
                        )
                        .orElseThrow(() -> new NoSuchElementException("Notificare negăsită") );

        notification.setRead(
                true
        );

        notificationRepository.save(
                notification
        );

    }

    private Notification findOwnGroupInvite(
            Integer id,
            Integer userId
    ){
        Notification notification = notificationRepository.findById(id)
                .orElseThrow();

        if (!notification.getRecipientUserId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to current user");
        }
        if ( ! NOTIF_TYPE_GROUP.equals(notification.getType())
                || !GROUP_REFERENCE_TYPE.equals(notification.getReferenceType())
                || notification.getReferenceId() == null) {
            throw new IllegalArgumentException("Notification is not a group invite");
        }

        return notification;
    }


    public long unreadCount(
            Integer userId
    ){

        return notificationRepository
                .countByRecipientUserIdAndIsReadFalse(
                        userId
                );

    }

}
