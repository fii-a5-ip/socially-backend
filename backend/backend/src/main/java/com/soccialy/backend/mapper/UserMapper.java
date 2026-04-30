package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.FilterDTO;
import com.soccialy.backend.dto.UserDTO;
import com.soccialy.backend.entity.Filter;
import com.soccialy.backend.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setBio(user.getBio());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());

        if (user.getFilters() != null) {
            List<FilterDTO> filterDTOs = user.getFilters().stream()
                    .map(this::filterToDTO)
                    .collect(Collectors.toList());
            dto.setFilters(filterDTOs);
        }

        return dto;
    }

    public User toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setBio(dto.getBio());
        user.setProfilePictureUrl(dto.getProfilePictureUrl());
        return user;
    }

    private FilterDTO filterToDTO(Filter filter) {
        FilterDTO dto = new FilterDTO();
        dto.setId(filter.getId());
        dto.setName(filter.getName());
        return dto;
    }
}