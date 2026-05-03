package com.soccialy.backend.service;

import com.soccialy.backend.dto.UserDTO;
import com.soccialy.backend.entity.Coordinates;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.mapper.UserMapper;
import com.soccialy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    public List<UserDTO> findAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    public UserDTO saveUser(UserDTO userDTO) {
        User user = userMapper.toEntity(userDTO);
        User savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }

    // --- MOCK METHODS FOR OUTGOING SERVICE ---

    /**
     * TODO: Replace with real external API logic.
     */
    public Coordinates getUserCoordinates(Integer userId) {
        return new Coordinates(45.0, 25.0); 
    }

    /**
     * TODO: Replace with real database logic.
     */
    public List<Integer> getUserProfileFilters(Integer userId) {
        return List.of(1, 4, 5);
    }
}