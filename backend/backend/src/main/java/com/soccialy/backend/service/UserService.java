package com.soccialy.backend.service;

import com.soccialy.backend.dto.FilterDTO;
import com.soccialy.backend.dto.UpdateUserDTO;
import com.soccialy.backend.dto.UserDTO;
import com.soccialy.backend.entity.Coordinates;
import com.soccialy.backend.entity.Filter;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.mapper.UserMapper;
import com.soccialy.backend.repository.FilterRepository;
import com.soccialy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FilterRepository filterRepository;

    @Autowired
    private UserMapper userMapper;

    public List<UserDTO> findAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    public UserDTO findUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return userMapper.toDTO(user);
    }

    public UserDTO findUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return userMapper.toDTO(user);
    }

    public UserDTO updateUser(String username, UpdateUserDTO updateDTO) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (updateDTO.getEmail() != null) {
            user.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getBio() != null) {
            user.setBio(updateDTO.getBio());
        }
        if (updateDTO.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(updateDTO.getProfilePictureUrl());
        }
        if (updateDTO.getFilterIds() != null) {
            Set<Filter> filters = new HashSet<>(filterRepository.findAllById(updateDTO.getFilterIds()));
            user.setFilters(filters);
        }

        User saved = userRepository.save(user);
        return userMapper.toDTO(saved);
    }

    public List<FilterDTO> getUserFilters(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return user.getFilters().stream()
                .map(f -> {
                    FilterDTO dto = new FilterDTO();
                    dto.setId(f.getId());
                    dto.setName(f.getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public UserDTO saveUser(UserDTO userDTO) {
        User user = userMapper.toEntity(userDTO);
        User savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }

    // --- USED BY OutgoingService ---

    /**
     * TODO: Replace with real external API logic for coordinates.
     */
    public Coordinates getUserCoordinates(Integer userId) {
        return new Coordinates(45.0, 25.0);
    }

    /**
     * Returns the filter IDs associated with a user.
     */
    public List<Integer> getUserProfileFilters(Integer userId) {
        return getUserFilters(userId).stream()
                .map(FilterDTO::getId)
                .collect(Collectors.toList());
    }
}