package com.socially.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable=false)
    private Integer recipientUserId;

    private Integer actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private NotificationType type;

    @Column(nullable=false,length=255)
    private String message;

    private Integer referenceId;

    private String referenceType;

    @Builder.Default
    private Boolean read=false;

    private LocalDateTime createdAt;
}