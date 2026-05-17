package com.soccialy.backend.dto;

import lombok.Data;

@Data
public class LocationSuggestionDTO {
    private String name;
    private String placeId;
    private Double lat;
    private Double lon;
    private String fullAddress;
    private String city;
    private String street;
    private String streetNumber;
    private String country;
    private Object distanceMeters;
}
