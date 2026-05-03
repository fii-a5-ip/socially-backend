package com.soccialy.backend.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventRequestDTO {

    @NotBlank(message = "Event name cannot be empty.")
    private String name;

    @NotBlank(message = "URL cannot be empty.")
    private String url;

    @NotNull(message = "A valid location ID is required.")
    private Integer locationId;

    @NotNull(message = "An event must have a scheduled date and time.")
    @FutureOrPresent(message = "You cannot schedule an event in the past.")
    private LocalDateTime scheduledDate;
}