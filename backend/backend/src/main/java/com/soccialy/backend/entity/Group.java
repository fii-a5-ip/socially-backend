package com.soccialy.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "`groups`") // 'groups' e cuvânt rezervat în MySQL — backtick-urile forțează Hibernate să-l escape corect
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

    @Column(name = "`desc`", columnDefinition = "LONGTEXT")
    private String desc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id", nullable = false)
    private User creator;

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupUser> groupUsers = new HashSet<>();

    public Group(String name) {
        this.name = name;
    }
}
