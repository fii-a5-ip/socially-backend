package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.UserDTO;
import com.soccialy.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();

        if (user.getId() != null) {
            dto.setId(user.getId().intValue());
        }

        dto.setUsername(user.getUsername());
        dto.setFullname(user.getFullname());
        dto.setEmail(user.getEmail());
        dto.setProfileImgUrl(user.getProfileImgUrl());
        return dto;
    }

    public User toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();

        if (dto.getId() != null) {
            user.setId(dto.getId().intValue());
        }

        user.setUsername(dto.getUsername());
        user.setFullname(dto.getFullname());
        user.setEmail(dto.getEmail());
        user.setProfileImgUrl(dto.getProfileImgUrl());
        return user;
    }
}