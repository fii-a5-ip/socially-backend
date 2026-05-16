package com.soccialy.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "`groups`") // 'groups' e cuvânt rezervat în MySQL — backtick-urile forțează Hibernate să-l
                          // escape corect
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

    @Column(name = "img_link", length = 2048)
    private String imgLink;

    @ManyToOne
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
                .collect(Collectors.toList());
    }
}
