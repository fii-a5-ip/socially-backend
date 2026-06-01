package com.soccialy.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateUserDTO {
    private String email;
    private String fullname;
    private String bio;
    private String profileImgUrl;
    private List<Integer> filterIds;
}