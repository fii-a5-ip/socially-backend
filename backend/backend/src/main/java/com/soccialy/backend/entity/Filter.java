package com.soccialy.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "filters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Filter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 45)
    private String name;

    @Builder.Default
    @ManyToMany(mappedBy = "filters")
    private Set<User> users = new HashSet<>();

    @Builder.Default
    @ManyToMany(mappedBy = "filters")
    private Set<Location> locations = new HashSet<>();

    public Filter(String name) {
        this.name = name;
    }
}
