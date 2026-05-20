package com.soccialy.backend.entity;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal longitude;

    @Column(name = "img_url", length = 2048)
    private String imgUrl;

    @Column(length = 100)
    private String country;

    @Column(name = "state_county", length = 100)
    private String stateCounty;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String street;

    @Column(name = "street_number", length = 20)
    private String streetNumber;

    @Column(length = 20)
    private String postalcode;

    @Column(name = "formatted_address", length = 255)
    private String formattedAddress;

    @Column(length = 100)
    private String contact;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Builder.Default
    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Event> events = new HashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "location_filters",
            joinColumns = @JoinColumn(name = "location_id"),
            inverseJoinColumns = @JoinColumn(name = "filter_id")
    )
    private Set<Filter> filters = new HashSet<>();

    public Location(String name, BigDecimal latitude, BigDecimal longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.events = new HashSet<>();
        this.filters = new HashSet<>();
    }
}