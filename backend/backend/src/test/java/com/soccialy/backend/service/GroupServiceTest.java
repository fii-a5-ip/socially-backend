package com.soccialy.backend.service;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.dto.GroupUserDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.GroupUser;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.mapper.GroupMapper;
import com.soccialy.backend.repository.GroupRepository;
import com.soccialy.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

    @InjectMocks
    private GroupService groupService;

    private User mockCreator;
    private User mockMember;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockCreator = new User();
        mockCreator.setId(1);
        mockCreator.setUsername("creator_user");
        mockCreator.setFullname("Creator User");

        mockMember = new User();
        mockMember.setId(2);
        mockMember.setUsername("member_user");
        mockMember.setFullname("Member User");
    }

    @Test
    void createGroup_Success_WithMembers() {
        // Arrange
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Test Grup");
        inputDTO.setCreatorUserId(1);
        inputDTO.setMembers(List.of(new GroupUserDTO(null, 2, "MEMBER")));

        Group savedGroup = new Group();
        savedGroup.setId(1);
        savedGroup.setName("Test Grup");
        savedGroup.setCreator(mockCreator);

        GroupUser adminUser = new GroupUser();
        adminUser.setGroup(savedGroup);
        adminUser.setUser(mockCreator);
        adminUser.setRole("ADMIN");

        GroupUser normalMember = new GroupUser();
        normalMember.setGroup(savedGroup);
        normalMember.setUser(mockMember);
        normalMember.setRole("MEMBER");

        savedGroup.setGroupUsers(new HashSet<>(Set.of(adminUser, normalMember)));

        GroupDTO outputDTO = new GroupDTO();
        outputDTO.setId(1);
        outputDTO.setName("Test Grup");
        outputDTO.setCreatorUserId(1);
        outputDTO.setMembers(List.of(
                new GroupUserDTO(1, 1, "ADMIN"),
                new GroupUserDTO(1, 2, "MEMBER")
        ));

        // Mocks
        when(userRepository.findById(1)).thenReturn(Optional.of(mockCreator));
        when(userRepository.findById(2)).thenReturn(Optional.of(mockMember));
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
        verify(userRepository, times(1)).findById(2); // extra member
    }

    @Test
    void createGroup_Success_NoExtraMembers() {
        // Arrange — creator only, no extra members
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Solo Grup");
        inputDTO.setCreatorUserId(1);
        inputDTO.setMembers(Collections.emptyList());

        Group savedGroup = new Group();
        savedGroup.setId(2);
        savedGroup.setName("Solo Grup");
        savedGroup.setCreator(mockCreator);

        GroupUser adminUser = new GroupUser();
        adminUser.setGroup(savedGroup);
        adminUser.setUser(mockCreator);
        adminUser.setRole("ADMIN");
        savedGroup.setGroupUsers(new HashSet<>(Set.of(adminUser)));

        GroupDTO outputDTO = new GroupDTO();
        outputDTO.setId(2);
        outputDTO.setName("Solo Grup");
        outputDTO.setCreatorUserId(1);
        outputDTO.setMembers(List.of(new GroupUserDTO(2, 1, "ADMIN")));

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
    void createGroup_CreatorNotFound_ThrowsException() {
        // Arrange
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Fail Grup");
        inputDTO.setCreatorUserId(999);

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

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                groupService.createGroup(inputDTO));

        assertTrue(ex.getMessage().contains("Creator user ID is required"));
        verify(groupRepository, never()).save(any());
    }
}