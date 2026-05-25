package com.soccialy.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventDetailsRequestDTO {
    @NotBlank(message = "Numele evenimentului este obligatoriu")
    @Size(max = 45, message = "Numele este prea lung")
    private String name;

    @NotBlank(message = "Descrierea este obligatorie")
    @Size(max = 2048, message = "Descrierea este prea lungă")
    private String desc;
}