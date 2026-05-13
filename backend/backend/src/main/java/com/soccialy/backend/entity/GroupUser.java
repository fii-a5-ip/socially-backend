package com.soccialy.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "group_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupUser {

    @EmbeddedId
    @Builder.Default
    private GroupUserId id = new GroupUserId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "role", length = 45)
    private String role;
}