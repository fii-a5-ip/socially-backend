package com.soccialy.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_event_votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "vote_type", nullable = false)
    private String voteType; // "Da", "Nu", "Poate"
}
