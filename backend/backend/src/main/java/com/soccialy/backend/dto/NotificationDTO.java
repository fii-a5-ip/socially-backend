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

    private String columnName;
    private String columnId;
    private String actions;
    private String externalLink;

    private Boolean read;

    private LocalDateTime createdAt;

    private String actions;
    private String externalLink;

    private String columnName;
    private String columnId; //Sigur asta trebuie sa fie un varchar(100)?

}
