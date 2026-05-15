package com.soccialy.backend.controller;

import com.soccialy.backend.dto.FilterDTO;
import com.soccialy.backend.dto.UpdateUserDTO;
import com.soccialy.backend.dto.UserDTO;
import com.soccialy.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAll_returnsListOfUsers() {
        when(userService.findAllUsers()).thenReturn(List.of(new UserDTO()));

        var response = userController.getAll();

        assertEquals(200, response.getStatusCode().value());
        verify(userService).findAllUsers();
    }

    @Test
    void getMe_returnsCurrentUserByIdPrincipal() {
        when(userService.findUserById(7)).thenReturn(new UserDTO());

        var response = userController.getMe(7);

        assertEquals(200, response.getStatusCode().value());
        verify(userService).findUserById(7);
    }

    @Test
    void getMe_returnsCurrentUserByUserDetailsPrincipal() {
        when(userDetails.getUsername()).thenReturn("test");
        when(userService.findUserByUsername("test")).thenReturn(new UserDTO());

        var response = userController.getMe(userDetails);

        assertEquals(200, response.getStatusCode().value());
        verify(userDetails).getUsername();
        verify(userService).findUserByUsername("test");
    }

    @Test
    void getMe_throwsWhenPrincipalIsMissing() {
        assertThrows(IllegalStateException.class, () -> userController.getMe(null));
    }

    @Test
    void getById_returnsUser() {
        when(userService.findUserById(1)).thenReturn(new UserDTO());

        var response = userController.getById(1);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void updateMe_returnsUpdatedUserByUserDetailsPrincipal() {
        UpdateUserDTO updateDTO = new UpdateUserDTO();
        when(userDetails.getUsername()).thenReturn("test");
        when(userService.updateUser("test", updateDTO)).thenReturn(new UserDTO());

        var response = userController.updateMe(userDetails, updateDTO);

        assertEquals(200, response.getStatusCode().value());
        verify(userDetails).getUsername();
        verify(userService).updateUser("test", updateDTO);
    }

    @Test
    void updateMe_updatesCurrentUserByIdPrincipal() {
        UpdateUserDTO updateDTO = new UpdateUserDTO();
        when(userService.updateUserById(7, updateDTO)).thenReturn(new UserDTO());

        var response = userController.updateMe(7, updateDTO);

        assertEquals(200, response.getStatusCode().value());
        verify(userService).updateUserById(7, updateDTO);
    }

    @Test
    void updateMe_throwsWhenPrincipalIsMissing() {
        UpdateUserDTO updateDTO = new UpdateUserDTO();

        assertThrows(IllegalStateException.class, () -> userController.updateMe(null, updateDTO));
    }

    @Test
    void getUserFilters_returnsFilters() {
        when(userService.getUserFilters(1)).thenReturn(List.of(new FilterDTO()));

        var response = userController.getUserFilters(1);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void create_returnsCreatedUser() {
        UserDTO dto = new UserDTO();
        when(userService.saveUser(dto)).thenReturn(dto);

        var response = userController.create(dto);

        assertEquals(200, response.getStatusCode().value());
    }
}