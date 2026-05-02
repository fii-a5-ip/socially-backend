package com.soccialy.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GroupDTO {
    private Integer id;

    @NotBlank(message = "Group name cannot be empty.")
    @Size(max = 45, message = "Group name is too long.")
    private String name;

    private Integer memberCount;
}
