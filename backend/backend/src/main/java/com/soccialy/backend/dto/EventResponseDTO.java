package com.soccialy.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO {
    private Integer id;
    private String name;
    private String url;
    private String desc;
    private Integer locationId;
    private String address;
    private Integer creatorUserId;
    private Integer groupId;
    private LocalDateTime scheduledDate;
    private List<Integer> filterIds;
    private List<FilterDTO> filters;
    private WeatherDTO weather;
    private Double distance;
    private Boolean isJoined;
}
