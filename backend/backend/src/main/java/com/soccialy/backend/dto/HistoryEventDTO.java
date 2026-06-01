package com.soccialy.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HistoryEventDTO {
    private Long id;
    private String name;
    private String date;
    private String time;
    private String locationName;
    private String imageUrl;
    private String role; // "ORGANIZER" or "PARTICIPANT"
}
