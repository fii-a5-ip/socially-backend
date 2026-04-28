package com.soccialy.backend.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Integer id;
    private String username;
    private String fullname;
    private String email;
    private String profileImgUrl;
}