package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class GroupMapper {

    public GroupDTO toDTO(Group group) {
        if (group == null) {
            return null;
        }
        GroupDTO dto = new GroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setImgLink(group.getImgLink());
        dto.setDesc(group.getDesc());
        
        if (group.getCreator() != null) {
            dto.setCreatorUserId(group.getCreator().getId());
        }
        
        if (group.getUsers() != null) {
            dto.setMemberIds(group.getUsers().stream().map(User::getId).collect(Collectors.toList()));
        }
        return dto;
    }

    public Group toEntity(GroupDTO dto) {
        if (dto == null) {
            return null;
        }
        Group group = new Group();
        group.setId(dto.getId());
        group.setName(dto.getName());
        group.setImgLink(dto.getImgLink());
        group.setDesc(dto.getDesc());
        // Relatiile (creator si users) se seteaza in Service unde avem acces la UserRepository
        return group;
    }
}
