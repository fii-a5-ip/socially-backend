package com.soccialy.backend.service;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.exception.GroupNotFoundException;
import com.soccialy.backend.mapper.GroupMapper;
import com.soccialy.backend.repository.GroupRepository;
import com.soccialy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;

    @Transactional(readOnly = true)
    public List<GroupDTO> findGroupsByUserId(Integer userId) {
        return groupRepository.findByUsersId(userId).stream()
                .map(groupMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupDTO> searchGroups(String query) {
        return groupRepository.searchByName(query).stream()
                .map(groupMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupDTO createGroup(GroupDTO groupDTO, Integer currentUserId) {
        Group group = groupMapper.toEntity(groupDTO);
        group.setId(null);

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));

        group.getUsers().add(currentUser);

        Group savedGroup = groupRepository.save(group);
        return groupMapper.toDTO(savedGroup);
    }

    @Transactional(readOnly = true)
    public GroupDTO findGroupById(Integer groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));

        return groupMapper.toDTO(group);
    }
}
