package com.soccialy.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 45)
    private String name;

    @Column(length = 2048)
    private String url;

    // Folosim backticks pentru 'desc'
    @Column(name = "`desc`", nullable = false, columnDefinition = "LONGTEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime date = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id", nullable = false)
    private User creator;

    public Event(String name, String url, String description, Location location, User creator) {
        this.name = name;
        this.url = url;
        this.description = description;
        this.location = location;
        this.creator = creator;
    }
}