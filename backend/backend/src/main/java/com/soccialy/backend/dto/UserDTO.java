package com.soccialy.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDTO {
    private Integer id;
    private String username;
    private String email;
    private String bio;
    private String profilePictureUrl;
    private List<FilterDTO> filters;
}
    private String fullname;
    private String email;
    private String profileImgUrl;
}
