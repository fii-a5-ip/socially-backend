package com.soccialy.backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
public class EventSearchFieldsDTO extends EventDiscoverFieldsDTO {

    @NotBlank(message = "Search query cannot be blank")
    @Size(max = 150, message = "Search query is too long")
    private String query;

}