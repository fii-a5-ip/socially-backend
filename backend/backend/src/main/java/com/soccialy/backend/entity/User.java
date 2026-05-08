package com.soccialy.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 100)
    private String fullname;

    @Column(unique = true, length = 100)
    private String email;

    @Column(name = "google_id", unique = true, length = 100)
    private String googleId;

    @Column(name = "profile_img_url", length = 2048)
    private String profileImgUrl;

    @JsonIgnore
    @Column(length = 256)
    private String password;


    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "user_filters",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "filter_id")
    )
    private Set<Filter> filters = new HashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "group_users",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<Group> groups = new HashSet<>();

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
