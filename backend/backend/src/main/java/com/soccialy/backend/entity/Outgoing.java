package com.soccialy.backend.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "outgoings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outgoing {

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

    private LocalDateTime scheduledDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "outgoing_filters", joinColumns = @JoinColumn(name = "outgoing_id"))
    @Column(name = "filter_id")
    private List<Integer> filterIds = new ArrayList<>();

    public Outgoing(String name, String url, Location location) {
        this.name = name;
        this.url = url;
        this.location = location;
    }
}