package com.soccialy.backend.mapper;

import com.soccialy.backend.dto.GroupDTO;
import com.soccialy.backend.entity.Group;
import com.soccialy.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GroupMapper — covers Entity-to-DTO and DTO-to-Entity conversions.
 */
class GroupMapperTest {

    private GroupMapper groupMapper;

    @BeforeEach
    void setUp() {
        groupMapper = new GroupMapper();
    }

    @Test
    void toDTO_Success() {
        // Arrange
        User creator = new User();
        creator.setId(1);
        creator.setUsername("creator");

        User member = new User();
        member.setId(2);
        member.setUsername("member");

        Group group = new Group();
        group.setId(10);
        group.setName("Test Grup");
        group.setImgLink("https://example.com/img.png");
        group.setCreator(creator);
        group.setUsers(new HashSet<>(Set.of(creator, member)));

        // Act
        GroupDTO dto = groupMapper.toDTO(group);

        // Assert
        assertNotNull(dto);
        assertEquals(10, dto.getId());
        assertEquals("Test Grup", dto.getName());
        assertEquals("https://example.com/img.png", dto.getImgLink());
        assertEquals(1, dto.getCreatorUserId());
        assertTrue(dto.getMemberIds().contains(1));
        assertTrue(dto.getMemberIds().contains(2));
        assertEquals(2, dto.getMemberIds().size());
    }

    @Test
    void toDTO_NullGroup_ReturnsNull() {
        assertNull(groupMapper.toDTO(null));
    }

    @Test
    void toDTO_NullCreatorAndUsers() {
        Group group = new Group();
        group.setId(5);
        group.setName("Empty Group");

        GroupDTO dto = groupMapper.toDTO(group);

        assertNotNull(dto);
        assertEquals(5, dto.getId());
        assertEquals("Empty Group", dto.getName());
        assertNull(dto.getCreatorUserId());
        // Group entity initializes users with empty HashSet (@Builder.Default),
        // so memberIds will be an empty list, not null
        assertNotNull(dto.getMemberIds());
        assertTrue(dto.getMemberIds().isEmpty());
    }

    @Test
    void toEntity_Success() {
        // Arrange
        GroupDTO dto = new GroupDTO();
        dto.setId(10);
        dto.setName("DTO Grup");
        dto.setImgLink("https://example.com/img.png");

        // Act
        Group entity = groupMapper.toEntity(dto);

        // Assert
        assertNotNull(entity);
        assertEquals(10, entity.getId());
        assertEquals("DTO Grup", entity.getName());
        assertEquals("https://example.com/img.png", entity.getImgLink());
    }

    @Test
    void toEntity_NullDTO_ReturnsNull() {
        assertNull(groupMapper.toEntity(null));
    }
}
