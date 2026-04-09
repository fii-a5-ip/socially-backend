package com.socially.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @JsonIgnore // Safety first! Never serialize the hash to JSON
    @Column(nullable = false)
    private String password;

    // Custom constructor for manual creation without an ID
    public User(String username, String password)
    {
        this.username = username;
        this.password = password;
    }
}