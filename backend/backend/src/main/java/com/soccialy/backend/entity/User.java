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

    @Column(unique = true, nullable = false, length = 45)
    private String username;


    @JsonIgnore
    @Column(nullable = false)
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
    @ManyToMany(mappedBy = "users")
    private Set<Group> groups = new HashSet<>();

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
