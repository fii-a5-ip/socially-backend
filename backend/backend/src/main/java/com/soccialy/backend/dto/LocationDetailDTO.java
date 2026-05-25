package com.soccialy.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class LocationDetailDTO {
    private String name;
    private String formattedAddress;
    private Double lat;
    private Double lon;
    private String postcode;
    private String city;
    private String street;
    private String streetNumber;
    private String country;
    private String phone;
    private String website;
    private List<String> tags;
    private String mapHtml;
}
