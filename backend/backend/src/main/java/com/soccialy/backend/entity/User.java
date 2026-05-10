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

    @JsonIgnore
    @Column(nullable = false, length = 256)
    private String password;

    @Column(name = "profile_img_url", length = 2048)
    private String profileImgUrl;

    @Column(length = 300)
    private String bio;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "user_filters",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "filter_id")
    )
    private Set<Filter> filters = new HashSet<>();
//test
    @Builder.Default
    @ManyToMany(mappedBy = "users")
    private Set<Group> groups = new HashSet<>();
}
