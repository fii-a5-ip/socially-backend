package com.soccialy.backend.service;

import com.soccialy.backend.dto.FilterDTO;
import com.soccialy.backend.dto.UpdateUserDTO;
import com.soccialy.backend.dto.UserDTO;
import com.soccialy.backend.entity.Filter;
import com.soccialy.backend.entity.User;
import com.soccialy.backend.mapper.UserMapper;
import com.soccialy.backend.repository.FilterRepository;
import com.soccialy.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Comparator;

import com.soccialy.backend.dto.HistoryEventDTO;
import com.soccialy.backend.repository.EventRepository;
import com.soccialy.backend.entity.Event;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private static final String USER_NOT_FOUND_WITH_ID = "User not found with id: ";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FilterRepository filterRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserMapper userMapper;

    public List<HistoryEventDTO> getUserHistory(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_WITH_ID + userId));

        List<Event> createdEvents = eventRepository.findByCreatorId(user.getId());
        List<Event> joinedEvents = eventRepository.findByParticipantsId(user.getId());

        Set<Event> allEvents = new HashSet<>();
        allEvents.addAll(createdEvents);
        allEvents.addAll(joinedEvents);

        return allEvents.stream()
                .sorted(Comparator.comparing(Event::getScheduledDate).reversed()) // Newest first
                .map(event -> HistoryEventDTO.builder()
                        .id(event.getId().longValue())
                        .name(event.getName())
                        .date(event.getScheduledDate() != null ? event.getScheduledDate().toLocalDate().toString() : "")
                        .time(event.getScheduledDate() != null ? event.getScheduledDate().toLocalTime().toString() : "")
                        .locationName(event.getLocation() != null ? event.getLocation().getName() : "")
                        .imageUrl(event.getUrl())
                        .role(createdEvents.contains(event) ? "ORGANIZER" : "PARTICIPANT")
                        .build())
                .toList();
    }

    public List<UserDTO> findAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDTO)
                .toList();
    }

    public List<UserDTO> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return userRepository.searchUsers(query.trim()).stream()
                .map(userMapper::toDTO)
                .toList();
    }

    public UserDTO findUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_WITH_ID + id));
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

        return updateUserEntity(user, updateDTO);
    }

    public UserDTO updateUserById(Integer userId, UpdateUserDTO updateDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_WITH_ID + userId));

        return updateUserEntity(user, updateDTO);
    }

    private UserDTO updateUserEntity(User user, UpdateUserDTO updateDTO) {
        if (updateDTO.getEmail() != null) {
            user.setEmail(updateDTO.getEmail());
        }

        if (updateDTO.getFullname() != null) {
            user.setFullname(updateDTO.getFullname());
        }

        if (updateDTO.getBio() != null) {
            user.setBio(updateDTO.getBio());
        }

        if (updateDTO.getProfileImgUrl() != null) {
            user.setProfileImgUrl(updateDTO.getProfileImgUrl());
        }

        if (updateDTO.getFilterIds() != null) {
            Set<Filter> filters =
                    new HashSet<>(filterRepository.findAllById(updateDTO.getFilterIds()));
            user.setFilters(filters);
            LOGGER.info("Saved filters for user {}: {}", user.getUsername(),
                    filters.stream()
                            .map(filter -> filter.getId() + ":" + filter.getName())
                            .toList());
        }

        User saved = userRepository.save(user);
        return userMapper.toDTO(saved);
    }

    public List<FilterDTO> getUserFilters(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_WITH_ID + userId));
        return user.getFilters().stream()
                .map(f -> {
                    FilterDTO dto = new FilterDTO();
                    dto.setId(f.getId());
                    dto.setName(f.getName());
                    return dto;
                })
                .toList();
    }

    public UserDTO saveUser(UserDTO userDTO) {
        User user = userMapper.toEntity(userDTO);
        User savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }

    public List<Integer> getUserProfileFilters(Integer userId) {
        return getUserFilters(userId).stream()
                .map(FilterDTO::getId)
                .toList();
    }

    public com.soccialy.backend.entity.Coordinates getUserCoordinates(Integer userId) {
        com.soccialy.backend.entity.Coordinates coords = new com.soccialy.backend.entity.Coordinates();
        coords.setLatitude(java.math.BigDecimal.valueOf(45.0));
        coords.setLongitude(java.math.BigDecimal.valueOf(25.0));
        return coords;
    }
}