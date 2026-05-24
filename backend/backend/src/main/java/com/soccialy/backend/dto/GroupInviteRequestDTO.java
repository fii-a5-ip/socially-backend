package com.soccialy.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupInviteRequestDTO {

    @NotNull(message = "User ID is required.")
    private Integer userId;
}
