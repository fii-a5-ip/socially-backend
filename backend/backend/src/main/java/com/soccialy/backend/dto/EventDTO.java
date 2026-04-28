package com.soccialy.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventDTO {
    private Integer id;
    private String name;
    private String url;
    private String description;
    private LocalDateTime date;
    private Integer locationId;
    private Integer creatorUserId;
}