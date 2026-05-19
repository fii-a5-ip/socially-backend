package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.dto.GroupUserDTO;
import com.soccialy.backend.entity.Group;
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
        dto.setDesc(group.getDesc());
        dto.setImgLink(group.getImgLink());

        if (group.getCreator() != null) {
            dto.setCreatorUserId(group.getCreator().getId());
        }

        if (group.getMembers() != null) {
            dto.setMembers(group.getMembers().stream()
                    .map(gu -> new GroupUserDTO(
                            gu.getGroup().getId(),
                            gu.getUser().getId(),
                            gu.getRole()
                    ))
                    .collect(Collectors.toList()));
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
        group.setDesc(dto.getDesc());
        group.setImgLink(dto.getImgLink());

        return group;
    }
}
