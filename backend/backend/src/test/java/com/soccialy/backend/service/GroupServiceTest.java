package com.soccialy.backend.service;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.mapper.GroupMapper;
import com.soccialy.backend.repository.GroupRepository;
import com.soccialy.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMapper groupMapper;

    @InjectMocks
    private GroupService groupService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("current_user");
        mockUser.setPassword("password");
    }

    @Test
    void findGroupsByUserId_ReturnsMappedGroups() {
        Group group = new Group();
        group.setId(1);
        group.setName("User Grup");

        GroupDTO groupDTO = new GroupDTO();
        groupDTO.setId(1);
        groupDTO.setName("User Grup");

        when(groupRepository.findByUsersId(1)).thenReturn(List.of(group));
        when(groupMapper.toDTO(group)).thenReturn(groupDTO);

        List<GroupDTO> result = groupService.findGroupsByUserId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("User Grup", result.get(0).getName());

        verify(groupRepository, times(1)).findByUsersId(1);
        verify(groupMapper, times(1)).toDTO(group);
    }

    @Test
    void findGroupById_ReturnsMappedGroup() {
        Group group = new Group();
        group.setId(3);
        group.setName("Details Grup");

        GroupDTO groupDTO = new GroupDTO();
        groupDTO.setId(3);
        groupDTO.setName("Details Grup");

        when(groupRepository.findById(3)).thenReturn(Optional.of(group));
        when(groupMapper.toDTO(group)).thenReturn(groupDTO);

        GroupDTO result = groupService.findGroupById(3);

        assertNotNull(result);
        assertEquals(3, result.getId());
        assertEquals("Details Grup", result.getName());

        verify(groupRepository, times(1)).findById(3);
        verify(groupMapper, times(1)).toDTO(group);
    }

    @Test
    void findGroupById_NotFound_ThrowsException() {
        when(groupRepository.findById(404)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                groupService.findGroupById(404));

        assertTrue(ex.getMessage().contains("Group not found with id: 404"));
        verify(groupMapper, never()).toDTO(any());
    }

    @Test
    void createGroup_AddsCurrentUserAsMember() {
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Test Grup");

        Group mappedGroup = new Group();
        mappedGroup.setName("Test Grup");

        Group savedGroup = new Group();
        savedGroup.setId(1);
        savedGroup.setName("Test Grup");
        savedGroup.setUsers(new HashSet<>(Set.of(mockUser)));

        GroupDTO outputDTO = new GroupDTO();
        outputDTO.setId(1);
        outputDTO.setName("Test Grup");
        outputDTO.setMemberCount(1);

        when(groupMapper.toEntity(inputDTO)).thenReturn(mappedGroup);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
        when(groupMapper.toDTO(savedGroup)).thenReturn(outputDTO);

        GroupDTO result = groupService.createGroup(inputDTO, 1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Test Grup", result.getName());
        assertEquals(1, result.getMemberCount());

        verify(groupRepository, times(1)).save(argThat(group -> group.getUsers().contains(mockUser)));
    }

    @Test
    void createGroup_CurrentUserNotFound_ThrowsException() {
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Fail Grup");

        Group mappedGroup = new Group();
        when(groupMapper.toEntity(inputDTO)).thenReturn(mappedGroup);
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                groupService.createGroup(inputDTO, 999));

        assertTrue(ex.getMessage().contains("Authenticated user not found."));
        verify(groupRepository, never()).save(any());
    }
}
