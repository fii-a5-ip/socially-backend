package com.soccialy.backend.service;

import com.soccialy.backend.dto.*;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.GroupMember;
import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.dto.GroupUserDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.GroupUser;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.exception.GroupNotFoundException;
import com.soccialy.backend.mapper.GroupMapper;
import com.soccialy.backend.repository.GroupRepository;
import com.soccialy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

        @Autowired
        private GroupRepository groupRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private GroupMapper groupMapper;

        @Autowired
        private com.soccialy.backend.repository.EventRepository eventRepository;

        @Autowired
        private com.soccialy.backend.repository.UserVoteRepository userVoteRepository;

        public GroupDTO createGroup(GroupDTO groupDTO, Integer creatorUserId) {
                groupDTO.setCreatorUserId(creatorUserId);
                return createGroup(groupDTO);
        }

        public GroupDTO createGroup(GroupDTO groupDTO) {
                Group group = groupMapper.toEntity(groupDTO);

                if (groupDTO.getCreatorUserId() != null) {
                        User creator = userRepository.findById(groupDTO.getCreatorUserId())
                                        .orElseThrow(() -> new RuntimeException("Creator not found"));
                        group.setCreator(creator);
                } else {
                        throw new RuntimeException("Creator user ID is required");
                }

                if (groupDTO.getMembers() != null && !groupDTO.getMembers().isEmpty()) {
                        List<Integer> memberIds = groupDTO.getMembers().stream().map(GroupUserDTO::getUserId).collect(Collectors.toList());
                        List<User> foundUsers = userRepository.findAllById(memberIds);
                        for (User user : foundUsers) {
                                group.getMembers().add(GroupMember.builder()
                                                .group(group)
                                                .user(user)
                                                .role("MEMBER")
                                                .build());
                        }
                }

                boolean creatorIsMember = group.getMembers().stream()
                                .anyMatch(m -> m.getUser().getId().equals(group.getCreator().getId()));

                if (!creatorIsMember) {
                        group.getMembers().add(GroupMember.builder()
                                        .group(group)
                                        .user(group.getCreator())
                                        .role("ADMIN")
                                        .build());
                }

                Group savedGroup = groupRepository.save(group);
                return groupMapper.toDTO(savedGroup);
        }

        public GroupDetailDTO getGroupDetails(Integer groupId, Integer userId, String query) {
                Group group = groupRepository.findById(groupId)
                                .orElseThrow(() -> new RuntimeException("Group not found"));

                List<GroupMemberDTO> memberDTOs = group.getMembers().stream()
                                .map(member -> GroupMemberDTO.builder()
                                                .id(member.getUser().getId())
                                                .name(member.getUser().getFullname() != null
                                                                ? member.getUser().getFullname()
                                                                : member.getUser().getUsername())
                                                .avatar(member.getUser().getProfileImgUrl())
                                                .role(member.getRole())
                                                .isReal(true)
                                                .build())
                                .collect(Collectors.toList());

                List<com.soccialy.backend.entity.Event> allEvents = eventRepository.findByGroupId(groupId);
                if (query != null && !query.trim().isEmpty()) {
                        String lowerQuery = query.toLowerCase();
                        allEvents = allEvents.stream()
                                        .filter(e -> (e.getName() != null
                                                        && e.getName().toLowerCase().contains(lowerQuery)) ||
                                                        (e.getFilterIds() != null && !e.getFilterIds().isEmpty()
                                                                        ? "categorizat".contains(lowerQuery)
                                                                        : "general".contains(lowerQuery)))
                                        .collect(Collectors.toList());
                }

                List<GroupEventDTO> events = allEvents.stream()
                                .map(event -> {
                                         int daVotes = userVoteRepository.countByEventIdAndVoteType(event.getId(), "Da");
                                         int nuVotes = userVoteRepository.countByEventIdAndVoteType(event.getId(), "Nu");
                                         int poateVotes = userVoteRepository.countByEventIdAndVoteType(event.getId(), "Poate");

                                         String myVoteStr = null;
                                         if (userId != null) {
                                                 myVoteStr = userVoteRepository
                                                                 .findByUserIdAndEventId(userId, event.getId())
                                                                 .map(com.soccialy.backend.entity.UserVote::getVoteType)
                                                                 .orElse(null);
                                         }

                                        return GroupEventDTO.builder()
                                                        .id(String.valueOf(event.getId()))
                                                        .title(event.getName())
                                                        .type(event.getFilterIds().isEmpty() ? "General"
                                                                        : "Categorizat")
                                                        .location(event.getLocation() != null
                                                                        ? event.getLocation().getName()
                                                                        : "N/A")
                                                        .time(event.getScheduledDate() != null
                                                                        ? event.getScheduledDate().toString()
                                                                        : "TBD")
                                                        .score(85) // Placeholder
                                                        .imageUrl(event.getUrl())
                                                        .votes(GroupEventVotesDTO.builder()
                                                                        .da(daVotes)
                                                                        .nu(nuVotes)
                                                                        .poate(poateVotes)
                                                                        .build())
                                                        .myVote(myVoteStr)
                                                        .description(event.getDesc())
                                                        .isJoined(userId != null && event.getParticipants().stream().anyMatch(u -> u.getId().equals(userId)))
                                                        .attributes(new ArrayList<>())
                                                        .build();
                                })
                                .sorted((a, b) -> {
                                        // Priority 1: Most 'YES' votes
                                        int compareYes = Integer.compare(b.getVotes().getDa(), a.getVotes().getDa());
                                        if (compareYes != 0)
                                                return compareYes;

                                        // Priority 2: Fewest 'NO' votes
                                        int compareNo = Integer.compare(a.getVotes().getNu(), b.getVotes().getNu());
                                        if (compareNo != 0)
                                                return compareNo;

                                        // Priority 3: Most 'MAYBE' votes
                                        return Integer.compare(b.getVotes().getPoate(), a.getVotes().getPoate());
                                })
                                .collect(Collectors.toList());

                // Set isWinning for the first event if it has votes
                if (!events.isEmpty()) {
                        GroupEventDTO topEvent = events.get(0);
                        if (topEvent.getVotes().getDa() > 0) {
                                topEvent.setWinning(true);
                        }
                }

                return GroupDetailDTO.builder()
                                .id(group.getId())
                                .name(group.getName())
                                .imgLink(group.getImgLink())
                                .members(memberDTOs)
                                .events(events)
                                .build();
        }

        public void leaveGroup(Integer groupId, Integer userId) {
                Group group = groupRepository.findById(groupId)
                                .orElseThrow(() -> new RuntimeException("Group not found"));
                
                group.getMembers().removeIf(m -> m.getUser().getId().equals(userId));
                groupRepository.save(group);
        }

        @Transactional(readOnly = true)
        public List<GroupDTO> findGroupsByUserId(Integer userId) {
                return groupRepository.findGroupsByUserId(userId).stream()
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
