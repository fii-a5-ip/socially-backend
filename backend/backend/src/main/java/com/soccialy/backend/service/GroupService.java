package com.soccialy.backend.service;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.dto.GroupUserDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.GroupUser;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.mapper.GroupMapper;
import com.soccialy.backend.repository.GroupRepository;
import com.soccialy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupMapper groupMapper;

    @Transactional
    public GroupDTO createGroup(GroupDTO groupDTO) {
        Group group = new Group();
        group.setName(groupDTO.getName());
        group.setDesc(groupDTO.getDesc());
        group.setImgLink(groupDTO.getImgLink());

        if (groupDTO.getCreatorUserId() == null) {
            throw new RuntimeException("Creator user ID is required");
        }
        User creator = userRepository.findById(groupDTO.getCreatorUserId())
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        group.setCreator(creator);

        Set<GroupUser> groupUsers = new HashSet<>();

        GroupUser creatorMember = new GroupUser();
        creatorMember.setGroup(group);
        creatorMember.setUser(creator);
        creatorMember.setRole("ADMIN");
        groupUsers.add(creatorMember);

        if (groupDTO.getMembers() != null && !groupDTO.getMembers().isEmpty()) {
            for (GroupUserDTO memberDTO : groupDTO.getMembers()) {
                if (memberDTO.getUserId().equals(creator.getId())) {
                    continue;
                }

                User user = userRepository.findById(memberDTO.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found with id: " + memberDTO.getUserId()));

                GroupUser groupUser = new GroupUser();
                groupUser.setGroup(group);
                groupUser.setUser(user);

                String role = (memberDTO.getRole() != null && !memberDTO.getRole().isBlank())
                        ? memberDTO.getRole()
                        : "MEMBER";
                groupUser.setRole(role);

                groupUsers.add(groupUser);
            }
        }

        group.setGroupUsers(groupUsers);

        Group savedGroup = groupRepository.save(group);

        return groupMapper.toDTO(savedGroup);
    }
}