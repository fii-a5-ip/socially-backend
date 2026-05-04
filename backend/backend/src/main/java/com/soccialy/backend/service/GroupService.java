package com.soccialy.backend.service;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.mapper.GroupMapper;
import com.soccialy.backend.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMapper groupMapper;

    public List<GroupDTO> findAll() {
        return groupRepository.findAll().stream()
                .map(groupMapper::toDTO)
                .collect(Collectors.toList());
    }

    public GroupDTO save(GroupDTO groupDTO) {
        Group group = groupMapper.toEntity(groupDTO);
        Group savedGroup = groupRepository.save(group);
        return groupMapper.toDTO(savedGroup);
    }

    public void deleteById(Integer id) {
        groupRepository.deleteById(id);
    }
}