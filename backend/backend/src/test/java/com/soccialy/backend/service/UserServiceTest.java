package com.soccialy.backend.service;

import com.soccialy.backend.dto.FilterDTO;
import com.soccialy.backend.dto.UpdateUserDTO;
import com.soccialy.backend.dto.UserDTO;
import com.soccialy.backend.entity.Filter;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.mapper.UserMapper;
import com.soccialy.backend.repository.FilterRepository;
import com.soccialy.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterRepository filterRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findAllUsers_returnsListOfUserDTOs() {
        User user = new User();
        UserDTO dto = new UserDTO();
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toDTO(user)).thenReturn(dto);
        List<UserDTO> result = userService.findAllUsers();
        assertEquals(1, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void findUserById_returnsUserDTO() {
        User user = new User();
        UserDTO dto = new UserDTO();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(dto);
        UserDTO result = userService.findUserById(1);
        assertNotNull(result);
    }

    @Test
    void findUserById_throwsWhenNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> userService.findUserById(99));
    }

    @Test
    void findUserByUsername_returnsUserDTO() {
        User user = new User();
        UserDTO dto = new UserDTO();
        when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(dto);
        UserDTO result = userService.findUserByUsername("test");
        assertNotNull(result);
    }

    @Test
    void findUserByUsername_throwsWhenNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> userService.findUserByUsername("ghost"));
    }

    @Test
    void updateUser_updatesFieldsAndSaves() {
        User user = new User();
        user.setUsername("test");
        UpdateUserDTO updateDTO = new UpdateUserDTO();
        updateDTO.setEmail("new@email.com");
        updateDTO.setBio("bio");
        updateDTO.setProfileImgUrl("url");
        updateDTO.setFilterIds(List.of(1));
        when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));
        when(filterRepository.findAllById(List.of(1))).thenReturn(List.of(new Filter()));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(new UserDTO());
        UserDTO result = userService.updateUser("test", updateDTO);
        assertNotNull(result);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserById_updatesFiltersAndSaves() {
        User user = new User();
        user.setUsername("test");
        UpdateUserDTO updateDTO = new UpdateUserDTO();
        updateDTO.setFilterIds(List.of(1, 2));
        Filter filter1 = new Filter();
        filter1.setId(1);
        filter1.setName("cafe");
        Filter filter2 = new Filter();
        filter2.setId(2);
        filter2.setName("bar");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(filterRepository.findAllById(List.of(1, 2))).thenReturn(List.of(filter1, filter2));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(new UserDTO());

        UserDTO result = userService.updateUserById(7, updateDTO);

        assertNotNull(result);
        assertEquals(2, user.getFilters().size());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserById_throwsWhenNotFound() {
        UpdateUserDTO updateDTO = new UpdateUserDTO();
        when(userRepository.findById(7)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> userService.updateUserById(7, updateDTO));
    }

    @Test
    void updateUser_throwsWhenNotFound() {
        UpdateUserDTO updateDTO = new UpdateUserDTO();
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> userService.updateUser("ghost", updateDTO));
    }

    @Test
    void getUserFilters_returnsFilters() {
        User user = new User();
        Filter filter = new Filter();
        user.setFilters(new HashSet<>(List.of(filter)));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        List<FilterDTO> result = userService.getUserFilters(1);
        assertNotNull(result);
    }

    @Test
    void getUserFilters_throwsWhenNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> userService.getUserFilters(99));
    }

    @Test
    void saveUser_returnsSavedUserDTO() {
        UserDTO dto = new UserDTO();
        User user = new User();
        when(userMapper.toEntity(dto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(dto);
        UserDTO result = userService.saveUser(dto);
        assertNotNull(result);
    }
}
