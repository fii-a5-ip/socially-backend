package com.soccialy.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateUserDTO {
    private String email;
    private String bio;
    private String profilePictureUrl;
    private List<Integer> filterIds;
}