package com.soccialy.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutgoingResponseDTO {
    private Integer id;
    private String name;
    private String url;
    private Integer locationId;
    private LocalDateTime scheduledDate;
}