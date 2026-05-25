package com.soccialy.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class LocationDTO {
    private Integer id;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String imgUrl;
    private String country;
    private String stateCounty;
    private String city;
    private String street;
    private String streetNumber;
    private String postalcode;
    private String formattedAddress;
    private String contact;
    private String phoneNumber;
    private List<String> tags;
}
