package com.soccialy.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private Integer id;

    private Integer actorUserId;

    private String username;

    private String profileImageUrl;

    private String type;

    private String message;

    private Integer referenceId;

    private String referenceType;

    private Boolean read;

    private LocalDateTime createdAt;

}
