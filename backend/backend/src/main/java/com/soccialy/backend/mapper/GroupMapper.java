package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.entity.Group;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

    public GroupDTO toDTO(Group group) {
        if (group == null) {
            return null;
        }

        GroupDTO dto = new GroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setMemberCount(group.getUsers() != null ? group.getUsers().size() : 0);
        return dto;
    }

    public Group toEntity(GroupDTO groupDTO) {
        if (groupDTO == null) {
            return null;
        }

        Group group = new Group();
        group.setId(groupDTO.getId());
        group.setName(groupDTO.getName());
        return group;
    }
}
