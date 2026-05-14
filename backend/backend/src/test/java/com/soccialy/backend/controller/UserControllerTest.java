package com.soccialy.backend.controller;

import com.soccialy.backend.dto.FilterDTO;
import com.soccialy.backend.dto.UpdateUserDTO;
import com.soccialy.backend.dto.UserDTO;
import com.soccialy.backend.security.CurrentUserService;
import com.soccialy.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private CurrentUserService currentUserService;

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
    void getMe_returnsCurrentUser() {
        when(currentUserService.getCurrentUserId()).thenReturn(7);
        when(userService.findUserById(7)).thenReturn(new UserDTO());

        var response = userController.getMe();

        assertEquals(200, response.getStatusCode().value());
        verify(currentUserService).getCurrentUserId();
        verify(userService).findUserById(7);
    }

    @Test
    void getById_returnsUser() {
        when(userService.findUserById(1)).thenReturn(new UserDTO());

        var response = userController.getById(1);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void updateMe_returnsUpdatedUser() {
        when(currentUserService.getCurrentUserId()).thenReturn(7);
        when(userService.updateUserById(eq(7), any(UpdateUserDTO.class)))
                .thenReturn(new UserDTO());

        var response = userController.updateMe(new UpdateUserDTO());

        assertEquals(200, response.getStatusCode().value());
        verify(currentUserService).getCurrentUserId();
        verify(userService).updateUserById(eq(7), any(UpdateUserDTO.class));
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