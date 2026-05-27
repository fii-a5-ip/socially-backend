package com.soccialy.backend.service;


import com.soccialy.backend.entity.*;
import com.soccialy.backend.mapper.GroupMapper;
import com.soccialy.backend.repository.EventRepository;
import com.soccialy.backend.dto.GroupDetailDTO;
import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.dto.GroupUserDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.GroupMember;
import com.soccialy.backend.entity.GroupUser;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.exception.GroupNotFoundException;
import com.soccialy.backend.mapper.GroupMapper;
import com.soccialy.backend.repository.GroupRepository;
import com.soccialy.backend.repository.UserVoteRepository;
import com.soccialy.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GroupService — covers group creation logic,
 * creator validation, and member assignment with roles.
 */
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMapper groupMapper;

    @Mock private EventRepository    eventRepository;
    @Mock private UserVoteRepository userVoteRepository;

    @InjectMocks
    private GroupService groupService;

    private User member;
    private Group group;

    private User creator;
    private User mockCreator;
    private User mockMember;


        @BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);

    // Used by the original createGroup tests
    mockCreator = new User();
    mockCreator.setId(1);
    mockCreator.setUsername("creator_user");
    mockCreator.setFullname("Creator User");

    mockMember = new User();
    mockMember.setId(2);
    mockMember.setUsername("member_user");
    mockMember.setFullname("Member User");

    // Used by the getGroupDetails / leaveGroup tests
    creator = new User();
    creator.setId(1);
    creator.setUsername("creator");
    creator.setFullname("Creator User");
    creator.setProfileImgUrl("https://example.com/creator.jpg");

    member = new User();
    member.setId(2);
    member.setUsername("member");
    member.setFullname("Member User");
    member.setProfileImgUrl(null);

    group = Group.builder()
            .id(10)
            .name("Test Group")
            .imgLink("https://example.com/group.jpg")
            .creator(creator)
            .build();

    group.getMembers().add(GroupMember.builder()
            .id(1).group(group).user(creator).role("ADMIN").build());
    group.getMembers().add(GroupMember.builder()
            .id(2).group(group).user(member).role("MEMBER").build());
    }

    @Test
    void createGroup_Success_WithMembers() {
        // Arrange
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Test Grup");
        inputDTO.setCreatorUserId(1);
        inputDTO.setMembers(List.of(new GroupUserDTO(null, 2, "MEMBER")));

        Group entityFromMapper = new Group();
        entityFromMapper.setName("Test Grup");

        Group savedGroup = new Group();
        savedGroup.setId(1);
        savedGroup.setName("Test Grup");
        savedGroup.setCreator(mockCreator);

        GroupMember adminUser = new GroupMember();
        adminUser.setGroup(savedGroup);
        adminUser.setUser(mockCreator);
        adminUser.setRole("ADMIN");

        GroupMember normalMember = new GroupMember();
        normalMember.setGroup(savedGroup);
        normalMember.setUser(mockMember);
        normalMember.setRole("MEMBER");

        savedGroup.setMembers(new ArrayList<>(List.of(adminUser, normalMember)));

        GroupDTO outputDTO = new GroupDTO();
        outputDTO.setId(1);
        outputDTO.setName("Test Grup");
        outputDTO.setCreatorUserId(1);
        outputDTO.setMembers(List.of(
                new GroupUserDTO(1, 1, "ADMIN"),
                new GroupUserDTO(1, 2, "MEMBER")
        ));

        // Mocks
        when(groupMapper.toEntity(any(GroupDTO.class))).thenReturn(entityFromMapper);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockCreator));
        when(userRepository.findAllById(List.of(2))).thenReturn(List.of(mockMember));
        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
        when(groupMapper.toDTO(savedGroup)).thenReturn(outputDTO);

        // Act
        GroupDTO result = groupService.createGroup(inputDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Test Grup", result.getName());
        assertEquals(1, result.getCreatorUserId());
        assertNotNull(result.getMembers());
        assertEquals(2, result.getMembers().size());

        assertTrue(result.getMembers().stream().anyMatch(m -> m.getUserId() == 1 && "ADMIN".equals(m.getRole())));
        assertTrue(result.getMembers().stream().anyMatch(m -> m.getUserId() == 2 && "MEMBER".equals(m.getRole())));

        verify(groupRepository, times(1)).save(any(Group.class));
        verify(userRepository, times(1)).findById(1); // creator
        verify(userRepository, times(1)).findAllById(List.of(2)); // extra member
    }

    @Test
    void createGroup_Success_NoExtraMembers() {
        // Arrange — creator only, no extra members
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Solo Grup");
        inputDTO.setCreatorUserId(1);
        inputDTO.setMembers(Collections.emptyList());

        Group entityFromMapper = new Group();
        entityFromMapper.setName("Solo Grup");

        Group savedGroup = new Group();
        savedGroup.setId(2);
        savedGroup.setName("Solo Grup");
        savedGroup.setCreator(mockCreator);

        GroupMember adminUser = new GroupMember();
        adminUser.setGroup(savedGroup);
        adminUser.setUser(mockCreator);
        adminUser.setRole("ADMIN");
        savedGroup.setMembers(new ArrayList<>(List.of(adminUser)));

        GroupDTO outputDTO = new GroupDTO();
        outputDTO.setId(2);
        outputDTO.setName("Solo Grup");
        outputDTO.setCreatorUserId(1);
        outputDTO.setMembers(List.of(new GroupUserDTO(2, 1, "ADMIN")));

        when(groupMapper.toEntity(any(GroupDTO.class))).thenReturn(entityFromMapper);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockCreator));
        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
        when(groupMapper.toDTO(savedGroup)).thenReturn(outputDTO);

        // Act
        GroupDTO result = groupService.createGroup(inputDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Solo Grup", result.getName());
        assertEquals(1, result.getMembers().size());
        assertEquals(1, result.getMembers().get(0).getUserId());
        assertEquals("ADMIN", result.getMembers().get(0).getRole());
    }

    @Test
    void createGroup_UsesAuthenticatedUserIdAndDefaultMemberRole() {
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Override Creator");
        inputDTO.setDesc("Created from authenticated user");
        inputDTO.setImgLink("https://example.com/group.png");
        inputDTO.setCreatorUserId(999);
        inputDTO.setMembers(List.of(new GroupUserDTO(null, 2, " ")));

        Group entityFromMapper = new Group();
        entityFromMapper.setName("Override Creator");
        entityFromMapper.setDesc("Created from authenticated user");
        entityFromMapper.setImgLink("https://example.com/group.png");

        GroupDTO outputDTO = new GroupDTO();
        outputDTO.setName("Override Creator");

        when(groupMapper.toEntity(any(GroupDTO.class))).thenReturn(entityFromMapper);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockCreator));
        when(userRepository.findAllById(List.of(2))).thenReturn(List.of(mockMember));
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(groupMapper.toDTO(any(Group.class))).thenReturn(outputDTO);

        GroupDTO result = groupService.createGroup(inputDTO, 1);

        assertSame(outputDTO, result);

        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(groupCaptor.capture());

        Group savedGroup = groupCaptor.getValue();
        assertEquals("Override Creator", savedGroup.getName());
        assertEquals("Created from authenticated user", savedGroup.getDesc());
        assertEquals("https://example.com/group.png", savedGroup.getImgLink());
        assertEquals(mockCreator, savedGroup.getCreator());
        assertEquals(2, savedGroup.getMembers().size());
        assertTrue(savedGroup.getMembers().stream()
                .anyMatch(gu -> gu.getUser().equals(mockCreator) && "ADMIN".equals(gu.getRole())));
        assertTrue(savedGroup.getMembers().stream()
                .anyMatch(gu -> gu.getUser().equals(mockMember) && "MEMBER".equals(gu.getRole())));

        verify(userRepository, never()).findById(999);
    }

    @Test
    void createGroup_SkipsCreatorWhenIncludedAsMember() {
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Creator Duplicate");
        inputDTO.setCreatorUserId(1);
        inputDTO.setMembers(List.of(new GroupUserDTO(null, 1, "MEMBER")));

        Group entityFromMapper = new Group();
        entityFromMapper.setName("Creator Duplicate");

        GroupDTO outputDTO = new GroupDTO();
        outputDTO.setName("Creator Duplicate");

        when(groupMapper.toEntity(any(GroupDTO.class))).thenReturn(entityFromMapper);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockCreator));
        when(userRepository.findAllById(List.of(1))).thenReturn(List.of(mockCreator));
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(groupMapper.toDTO(any(Group.class))).thenReturn(outputDTO);

        groupService.createGroup(inputDTO);

        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(groupCaptor.capture());

        Group savedGroup = groupCaptor.getValue();
        // Creator was already added as MEMBER by findAllById, then the code checks
        // if creator is already a member and skips adding ADMIN duplicate.
        // The actual behavior: creator is added as MEMBER (from the members list),
        // then since creatorIsMember == true, ADMIN is NOT added again.
        // So we should have exactly 1 member with role MEMBER.
        assertEquals(1, savedGroup.getMembers().size());
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    void createGroup_MemberNotFound_SilentlySkipped() {
        // When a member ID doesn't exist, findAllById simply returns
        // an empty list for that user. No exception is thrown.
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Missing Member");
        inputDTO.setCreatorUserId(1);
        inputDTO.setMembers(List.of(new GroupUserDTO(null, 404, "MEMBER")));

        Group entityFromMapper = new Group();
        entityFromMapper.setName("Missing Member");

        Group savedGroup = new Group();
        savedGroup.setId(1);
        savedGroup.setName("Missing Member");
        savedGroup.setCreator(mockCreator);

        GroupDTO outputDTO = new GroupDTO();
        outputDTO.setId(1);
        outputDTO.setName("Missing Member");

        when(groupMapper.toEntity(any(GroupDTO.class))).thenReturn(entityFromMapper);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockCreator));
        when(userRepository.findAllById(List.of(404))).thenReturn(Collections.emptyList());
        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
        when(groupMapper.toDTO(any(Group.class))).thenReturn(outputDTO);

        // Should NOT throw — missing members are silently skipped
        GroupDTO result = groupService.createGroup(inputDTO);
        assertNotNull(result);
        verify(groupRepository, times(1)).save(any());
    }

    @Test
    void createGroup_CreatorNotFound_ThrowsException() {
        // Arrange
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Fail Grup");
        inputDTO.setCreatorUserId(999);

        Group entityFromMapper = new Group();
        entityFromMapper.setName("Fail Grup");

        when(groupMapper.toEntity(any(GroupDTO.class))).thenReturn(entityFromMapper);
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                groupService.createGroup(inputDTO));

        assertTrue(ex.getMessage().contains("Creator not found"));
        verify(groupRepository, never()).save(any());
    }

    @Test
    void createGroup_NullCreatorId_ThrowsException() {
        // Arrange
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("No Creator");
        inputDTO.setCreatorUserId(null);

        Group entityFromMapper = new Group();
        entityFromMapper.setName("No Creator");

        when(groupMapper.toEntity(any(GroupDTO.class))).thenReturn(entityFromMapper);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                groupService.createGroup(inputDTO));

        assertTrue(ex.getMessage().contains("Creator user ID is required"));
        verify(groupRepository, never()).save(any());
    }

    @Test
    void findGroupsByUserId_ReturnsMappedGroups() {
        Group group = new Group();
        group.setId(10);
        group.setName("User Group");

        GroupDTO dto = new GroupDTO();
        dto.setId(10);
        dto.setName("User Group");

        when(groupRepository.findGroupsByUserId(1)).thenReturn(List.of(group));
        when(groupMapper.toDTO(group)).thenReturn(dto);

        List<GroupDTO> result = groupService.findGroupsByUserId(1);

        assertEquals(1, result.size());
        assertEquals("User Group", result.get(0).getName());
        verify(groupRepository).findGroupsByUserId(1);
    }

    @Test
    void searchGroups_TrimsQueryAndReturnsMappedGroups() {
        Group group = new Group();
        group.setId(11);
        group.setName("Search Result");

        GroupDTO dto = new GroupDTO();
        dto.setId(11);
        dto.setName("Search Result");

        when(groupRepository.searchGroups("board games")).thenReturn(List.of(group));
        when(groupMapper.toDTO(group)).thenReturn(dto);

        List<GroupDTO> result = groupService.searchGroups("  board games  ");

        assertEquals(1, result.size());
        assertEquals("Search Result", result.get(0).getName());
        verify(groupRepository).searchGroups("board games");
    }

    @Test
    void findGroupById_ReturnsMappedGroup() {
        Group group = new Group();
        group.setId(12);
        group.setName("Details");

        GroupDTO dto = new GroupDTO();
        dto.setId(12);
        dto.setName("Details");

        when(groupRepository.findById(12)).thenReturn(Optional.of(group));
        when(groupMapper.toDTO(group)).thenReturn(dto);

        GroupDTO result = groupService.findGroupById(12);

        assertEquals(12, result.getId());
        assertEquals("Details", result.getName());
    }

    @Test
    void findGroupById_NotFound_ThrowsException() {
        when(groupRepository.findById(404)).thenReturn(Optional.empty());

        GroupNotFoundException ex = assertThrows(GroupNotFoundException.class, () ->
                groupService.findGroupById(404));

        assertTrue(ex.getMessage().contains("404"));
        verify(groupMapper, never()).toDTO(any());
    }


    @Test
    void getGroupDetails_AuthenticatedMember_ReturnsIsCurrentUserMemberTrue() {
        when(groupRepository.findById(10)).thenReturn(Optional.of(group));
        when(eventRepository.findByGroupId(10)).thenReturn(List.of());
 
        GroupDetailDTO result = groupService.getGroupDetails(10, 1, null);
 
        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("Test Group", result.getName());
        assertTrue(result.getIsCurrentUserMember(),
                "Creator (userId=1) is a member — isCurrentUserMember must be true");
    }
 
    @Test
    void getGroupDetails_AuthenticatedNonMember_ReturnsIsCurrentUserMemberFalse() {
        when(groupRepository.findById(10)).thenReturn(Optional.of(group));
        when(eventRepository.findByGroupId(10)).thenReturn(List.of());
 
        GroupDetailDTO result = groupService.getGroupDetails(10, 99, null);
 
        assertNotNull(result);
        assertFalse(result.getIsCurrentUserMember(),
                "User 99 is not a member — isCurrentUserMember must be false");
    }
 
    @Test
    void getGroupDetails_NullUserId_ReturnsIsCurrentUserMemberFalse() {
        when(groupRepository.findById(10)).thenReturn(Optional.of(group));
        when(eventRepository.findByGroupId(10)).thenReturn(List.of());
 
        GroupDetailDTO result = groupService.getGroupDetails(10, null, null);
 
        assertFalse(result.getIsCurrentUserMember(),
                "Unauthenticated request (userId=null) must return false");
    }
 
    @Test
    void getGroupDetails_MemberDTOs_MappedCorrectly() {
        when(groupRepository.findById(10)).thenReturn(Optional.of(group));
        when(eventRepository.findByGroupId(10)).thenReturn(List.of());
 
        GroupDetailDTO result = groupService.getGroupDetails(10, 1, null);
 
        assertEquals(2, result.getMembers().size());
 
        // Creator: has fullname, so name should be fullname
        var creatorDTO = result.getMembers().stream()
                .filter(m -> m.getId().equals(1)).findFirst().orElseThrow();
        assertEquals("Creator User", creatorDTO.getName());
        assertEquals("ADMIN", creatorDTO.getRole());
        assertEquals("https://example.com/creator.jpg", creatorDTO.getAvatar());
        assertTrue(creatorDTO.isReal());
 
        // Member: also has fullname
        var memberDTO = result.getMembers().stream()
                .filter(m -> m.getId().equals(2)).findFirst().orElseThrow();
        assertEquals("Member User", memberDTO.getName());
        assertEquals("MEMBER", memberDTO.getRole());
    }
 
    @Test
    void getGroupDetails_MemberWithNullFullname_FallsBackToUsername() {
        member.setFullname(null); // force the username fallback branch
 
        when(groupRepository.findById(10)).thenReturn(Optional.of(group));
        when(eventRepository.findByGroupId(10)).thenReturn(List.of());
 
        GroupDetailDTO result = groupService.getGroupDetails(10, 1, null);
 
        var memberDTO = result.getMembers().stream()
                .filter(m -> m.getId().equals(2)).findFirst().orElseThrow();
        assertEquals("member", memberDTO.getName(),
                "When fullname is null the username should be used as the display name");
    }
 
    @Test
    void getGroupDetails_GroupNotFound_Throws() {
        when(groupRepository.findById(999)).thenReturn(Optional.empty());
 
        assertThrows(RuntimeException.class, () ->
                groupService.getGroupDetails(999, 1, null));
    }
 
    @Test
    void getGroupDetails_NoEvents_ReturnsEmptyEventList() {
        when(groupRepository.findById(10)).thenReturn(Optional.of(group));
        when(eventRepository.findByGroupId(10)).thenReturn(List.of());
 
        GroupDetailDTO result = groupService.getGroupDetails(10, 1, null);
 
        assertNotNull(result.getEvents());
        assertTrue(result.getEvents().isEmpty());
    }
 
    @Test
    void getGroupDetails_GroupName_PresentInResult() {
        when(groupRepository.findById(10)).thenReturn(Optional.of(group));
        when(eventRepository.findByGroupId(10)).thenReturn(List.of());
 
        GroupDetailDTO result = groupService.getGroupDetails(10, 1, null);
 
        assertEquals("Test Group", result.getName());
        assertEquals("https://example.com/group.jpg", result.getImgLink());
    }
 
    // -------------------------------------------------------------------------
    // leaveGroup
    // -------------------------------------------------------------------------
 
    @Test
    void leaveGroup_RemovesMemberAndSaves() {
        when(groupRepository.findById(10)).thenReturn(Optional.of(group));
 
        groupService.leaveGroup(10, 2); // member leaves
 
        boolean memberStillPresent = group.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(2));
 
        assertFalse(memberStillPresent, "User 2 should have been removed from the group");
        verify(groupRepository).save(group);
    }
 
    @Test
    void leaveGroup_CreatorLeaves_OnlyCreatorRemoved() {
        when(groupRepository.findById(10)).thenReturn(Optional.of(group));
 
        groupService.leaveGroup(10, 1); // creator leaves
 
        assertEquals(1, group.getMembers().size());
        assertEquals(2, group.getMembers().get(0).getUser().getId());
        verify(groupRepository).save(group);
    }
 
    @Test
    void leaveGroup_UserNotInGroup_NoChangeAndSaves() {
        // User 99 is not a member — removeIf finds nothing, but save still called
        when(groupRepository.findById(10)).thenReturn(Optional.of(group));
 
        groupService.leaveGroup(10, 99);
 
        assertEquals(2, group.getMembers().size(), "Member list should be unchanged");
        verify(groupRepository).save(group);
    }
 
    @Test
    void leaveGroup_GroupNotFound_Throws() {
        when(groupRepository.findById(999)).thenReturn(Optional.empty());
 
        assertThrows(RuntimeException.class, () ->
                groupService.leaveGroup(999, 1));
 
        verify(groupRepository, never()).save(any());
    }
}
