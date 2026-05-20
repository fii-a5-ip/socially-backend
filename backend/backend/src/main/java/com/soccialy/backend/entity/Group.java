package com.soccialy.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "UserGroup")
@Table(name = "`groups`") // Notă: 'groups' este cuvânt rezervat în MySQL, dar JPA se descurcă dacă e setat corect.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 45)
    private String name;

    @Column(name = "img_link", columnDefinition = "LONGTEXT")
    private String imgLink;

    @Column(name = "`desc`", columnDefinition = "LONGTEXT")
    private String desc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id", nullable = false)
    private User creator;

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> members = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "group")
    private List<Event> events = new ArrayList<>();

    public Group(String name) {
        this.name = name;
    }

    /**
     * Return the {@link User} that belongs to this group with the given userId.
     * If the user is not a member of the group, returns {@code null}.
     * 
     * public User getUsers(Integer userId) {
     * if (userId == null)
     * return null;
     * return members.stream()
     * .filter(m -> m.getUser() != null && userId.equals(m.getUser().getId()))
     * .map(GroupMember::getUser)
     * .findFirst()
     * .orElse(null);
     * }
     */
    public List<User> getUsers() {
        return members.stream()
                .map(GroupMember::getUser)
                .toList();
    }
}
