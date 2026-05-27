package com.soccialy.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LocationSuggestionDTO {
    private String name;
    private String placeId;
    private BigDecimal lat;
    private BigDecimal lon;
    private String fullAddress;
    private String city;
    private String street;
    private String streetNumber;
    private String country;
    private Object distanceMeters;
}
