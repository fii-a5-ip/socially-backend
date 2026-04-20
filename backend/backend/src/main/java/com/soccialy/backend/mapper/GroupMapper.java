package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.entity.Group;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

    public GroupDTO toDTO(Group group) {
        if (group == null) return null;

        GroupDTO dto = new GroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        return dto;
    }

    public Group toEntity(GroupDTO dto) {
        if (dto == null) return null;

        return Group.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
    }
}