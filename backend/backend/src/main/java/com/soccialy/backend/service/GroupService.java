package com.soccialy.backend.service;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.exception.GroupNotFoundException;
import com.soccialy.backend.mapper.GroupMapper;
import com.soccialy.backend.repository.GroupRepository;
import com.soccialy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupMapper groupMapper;

    public GroupDTO createGroup(GroupDTO groupDTO) {
        return createGroup(groupDTO, groupDTO.getCreatorUserId());
    }

    public GroupDTO createGroup(GroupDTO groupDTO, Integer creatorUserId) {
        Group group = groupMapper.toEntity(groupDTO);

        // Seteaza creatorul
        if (creatorUserId != null) {
            User creator = userRepository.findById(creatorUserId)
                    .orElseThrow(() -> new RuntimeException("Creator not found"));
            group.setCreator(creator);
        } else {
            throw new RuntimeException("Creator user ID is required");
        }

        // Seteaza membrii
        Set<User> members = new HashSet<>();
        if (groupDTO.getMemberIds() != null && !groupDTO.getMemberIds().isEmpty()) {
            List<User> foundUsers = userRepository.findAllById(groupDTO.getMemberIds());
            members.addAll(foundUsers);
        }
        
        // Asigura-te ca creatorul este adaugat automat in lista de membri
        members.add(group.getCreator());
        group.setUsers(members);

        Group savedGroup = groupRepository.save(group);
        return groupMapper.toDTO(savedGroup);
    }

    @Transactional(readOnly = true)
    public List<GroupDTO> findGroupsByUserId(Integer userId) {
        return groupRepository.findByUsersId(userId).stream()
                .map(groupMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupDTO> searchGroups(String query) {
        return groupRepository.searchGroups(query.trim()).stream()
                .map(groupMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDTO findGroupById(Integer groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));

        return groupMapper.toDTO(group);
    }
}
