package com.soccialy.backend.controller;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.dto.GroupUserDTO;
import com.soccialy.backend.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GroupController — covers the POST /api/groups endpoint.
 */
class GroupControllerTest {

    @Mock
    private GroupService groupService;

    @InjectMocks
    private GroupController groupController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createGroup_ReturnsCreatedGroup() {
        // Arrange
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Controller Test");
        inputDTO.setCreatorUserId(1);

        inputDTO.setMembers(List.of(
                new GroupUserDTO(null, 1, "ADMIN"),
                new GroupUserDTO(null, 2, "MEMBER")
        ));

        GroupDTO outputDTO = new GroupDTO();
        outputDTO.setId(1);
        outputDTO.setName("Controller Test");
        outputDTO.setCreatorUserId(1);

        outputDTO.setMembers(List.of(
                new GroupUserDTO(1, 1, "ADMIN"),
                new GroupUserDTO(1, 2, "MEMBER")
        ));

        when(groupService.createGroup(inputDTO)).thenReturn(outputDTO);

        // Act
        ResponseEntity<GroupDTO> response = groupController.createGroup(inputDTO);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getId());
        assertEquals("Controller Test", response.getBody().getName());

        verify(groupService, times(1)).createGroup(inputDTO);
    }

    @Test
    void createGroup_ServiceThrows_PropagatesException() {
        // Arrange
        GroupDTO inputDTO = new GroupDTO();
        inputDTO.setName("Fail Test");
        inputDTO.setCreatorUserId(null);

        when(groupService.createGroup(inputDTO))
                .thenThrow(new RuntimeException("Creator user ID is required"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                groupController.createGroup(inputDTO));

        assertTrue(ex.getMessage().contains("Creator user ID is required"));
    }
}
