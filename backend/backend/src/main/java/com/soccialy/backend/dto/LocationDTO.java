package com.soccialy.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LocationDTO {
    private Integer id;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String imgUrl;
}