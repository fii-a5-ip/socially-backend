package com.soccialy.backend.dto;

import lombok.Data;

@Data
public class OutgoingDTO {
    private Integer id;
    private String name;
    private String url;
    private Integer locationId;
}