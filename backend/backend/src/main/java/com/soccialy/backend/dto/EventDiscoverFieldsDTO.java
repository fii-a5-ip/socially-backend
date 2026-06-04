package com.soccialy.backend.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventDiscoverFieldsDTO {
    private List<Integer> filterIds;
    private Double maxDistance;
    private Integer maxDays;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime localTime;

    private BigDecimal lat;
    private BigDecimal lng;
    private String query;
    private Integer offset;
}