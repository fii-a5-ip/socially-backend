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
        if (group.getUsers() != null) {
            dto.setMemberCount(group.getUsers().size());
        }
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
