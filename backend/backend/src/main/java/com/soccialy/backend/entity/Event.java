package com.soccialy.backend.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id", nullable = false)
    private User creator;

    @Column(name = "`desc`", columnDefinition = "LONGTEXT", nullable = false)
    private String desc;

    @Column(name = "date", nullable = false)
    private LocalDateTime scheduledDate;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_filters", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "filter_id")
    private List<Integer> filterIds = new ArrayList<>();

//    @Builder.Default
//    @ManyToMany
//    @JoinTable(
//            name = "event_filters",
//            joinColumns = @JoinColumn(name = "event_id"),
//            inverseJoinColumns = @JoinColumn(name = "filter_id")
//    )
//    private Set<Filter> filters = new HashSet<>();

    public Event(String name, String url, Location location, User creator, String desc, LocalDateTime date) {
        this.name = name;
        this.url = url;
        this.location = location;
        this.creator = creator;
        this.desc = desc;
        this.scheduledDate = date;
    }
}