package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.UserDTO;
import com.soccialy.backend.entity.Filter;
import com.soccialy.backend.entity.User;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void toDTO_mapsAllFields() {
        User user = new User();
        user.setId(1);
        user.setUsername("test");
        user.setFullname("Test User");
        user.setEmail("test@test.com");
        user.setBio("bio");
        user.setProfilePictureUrl("url");
        user.setProfileImgUrl("imgUrl");
        user.setFilters(new HashSet<>());

        UserDTO dto = userMapper.toDTO(user);

        assertEquals(1, dto.getId());
        assertEquals("test", dto.getUsername());
        assertEquals("Test User", dto.getFullname());
        assertEquals("test@test.com", dto.getEmail());
        assertEquals("bio", dto.getBio());
        assertEquals("url", dto.getProfilePictureUrl());
    }

    @Test
    void toDTO_withFilters_mapsFilters() {
        Filter filter = new Filter();
        filter.setId(1);
        filter.setName("Sport");

        User user = new User();
        user.setFilters(new HashSet<>(Set.of(filter)));

        UserDTO dto = userMapper.toDTO(user);

        assertNotNull(dto.getFilters());
        assertEquals(1, dto.getFilters().size());
    }

    @Test
    void toDTO_nullUser_returnsNull() {
        assertNull(userMapper.toDTO(null));
    }

    @Test
    void toEntity_mapsAllFields() {
        UserDTO dto = new UserDTO();
        dto.setId(1);
        dto.setUsername("test");
        dto.setEmail("test@test.com");
        dto.setBio("bio");
        dto.setProfilePictureUrl("url");

        User user = userMapper.toEntity(dto);

        assertEquals(1, user.getId());
        assertEquals("test", user.getUsername());
    }

    @Test
    void toEntity_nullDTO_returnsNull() {
        assertNull(userMapper.toEntity(null));
    }
}
